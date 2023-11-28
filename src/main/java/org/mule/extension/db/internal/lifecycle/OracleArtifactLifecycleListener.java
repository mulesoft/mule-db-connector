/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Pattern;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;

public class OracleArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(OracleArtifactLifecycleListener.class);

  /* This system property should be avoid.dispose.timer.threads because there are others drivers that also uses them, but we
   * cannot change it due to backward compatibility */
  private static final String AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME = "avoid.dispose.oracle.threads";
  private static final boolean JDBC_RESOURCE_RELEASER_AVOID_DISPOSE_TIMER_THREADS =
      getBoolean(AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME);
  public static final String DIAGNOSABILITY_BEAN_NAME = "diagnosability";
  public static final String DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final Pattern DRIVER_TIMER_THREAD_PATTERN = Pattern.compile("^Timer-\\d+");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    deregisterJdbcDrivers(artifactDisposalContext);
  }

  private void deregisterJdbcDrivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(d -> isOracleDriver(d))
        .forEach(driver -> {
          try {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
            deregisterDriver(driver);
            deregisterOracleDiagnosabilityMBean(disposalContext.getArtifactClassLoader());
            deregisterOracleDiagnosabilityMBean(disposalContext.getArtifactClassLoader());
            disposeDriverTimerThreads(disposalContext);
          } catch (Exception e) {
            LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
          }
        });
  }

  private boolean isOracleDriver(Driver driver) {
    try {
      return driver.getClass().getClassLoader().loadClass("oracle.jdbc.OracleDriver").isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      // If the class is not found, there is no such driver.
      return false;
    }
  }

  private void deregisterOracleDiagnosabilityMBean(ClassLoader cl) {
    MBeanServer mBeanServer = getPlatformMBeanServer();
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", DIAGNOSABILITY_BEAN_NAME);
    keys.put("name", cl.getClass().getName() + "@" + toHexString(cl.hashCode()).toLowerCase());
    try {
      mBeanServer.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
    } catch (javax.management.InstanceNotFoundException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("No Oracle's '%s' MBean found.", DIAGNOSABILITY_BEAN_NAME));
      }
    } catch (Throwable e) {
      LOGGER.warn("Unable to unregister Oracle's mbeans", e);
    }
  }

  private void disposeDriverTimerThreads(ArtifactDisposalContext artifactDisposalContext) {
    try {
      if (JDBC_RESOURCE_RELEASER_AVOID_DISPOSE_TIMER_THREADS) {
        return;
      }
      /* IMPORTANT: This is done to avoid metaspace OOM caused by thread leaks from oracle and db2 drivers. This is only meant to
       * stop TimerThread threads spawned by oracle driver's HAManager class. This timer cannot be fetched by reflection because,
       * in order to do so, other oracle dependencies would be required. */
      artifactDisposalContext.getArtifactOwnedThreads()
          .filter(this::isTimerThread)
          .forEach(this::disposeTimerThread);
      artifactDisposalContext.getExtensionOwnedThreads()
          .filter(this::isTimerThread)
          .forEach(this::disposeTimerThread);

    } catch (Exception e) {
      LOGGER.error("An exception occurred while attempting to dispose of oracle timer threads: {}", e.getMessage());
    }
  }

  private boolean isTimerThread(Thread thread) {
    return thread.getClass().getSimpleName().equals(DRIVER_TIMER_THREAD_CLASS_NAME)
        && DRIVER_TIMER_THREAD_PATTERN.matcher(thread.getName()).matches();
  }

  private void disposeTimerThread(Thread thread) {
    try {
      clearReferencesStopTimerThread(thread);
      thread.interrupt();
      thread.join();
    } catch (Throwable e) {
      LOGGER.warn("An error occurred trying to close the '" + thread.getName() + "' Thread. This might cause memory leaks.", e);
    }
  }

  private void clearReferencesStopTimerThread(Thread thread)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    // Need to get references to:
    // in Sun/Oracle JDK:
    // - newTasksMayBeScheduled field (in java.util.TimerThread)
    // - queue field
    // - queue.clear()
    // in IBM JDK, Apache Harmony and DB2:
    // - cancel() method (in java.util.Timer$TimerImpl)
    try {
      Field newTasksMayBeScheduledField =
          thread.getClass().getDeclaredField("newTasksMayBeScheduled");
      newTasksMayBeScheduledField.setAccessible(true);
      Field queueField = thread.getClass().getDeclaredField("queue");
      queueField.setAccessible(true);
      Object queue = queueField.get(thread);
      Method clearMethod = queue.getClass().getDeclaredMethod("clear");
      clearMethod.setAccessible(true);
      synchronized (queue) {
        newTasksMayBeScheduledField.setBoolean(thread, false);
        clearMethod.invoke(queue);
        // In case queue was already empty. Should only be one
        // thread waiting but use notifyAll() to be safe.
        queue.notifyAll();
        newTasksMayBeScheduledField.setAccessible(false);
        queueField.setAccessible(false);
        clearMethod.setAccessible(false);
      }
    } catch (NoSuchFieldException noSuchFieldEx) {
      LOGGER.warn("Unable to clear timer references using 'clear' method. Attempting to use 'cancel' method.");
      Method cancelMethod = thread.getClass().getDeclaredMethod("cancel");
      cancelMethod.setAccessible(true);
      cancelMethod.invoke(thread);
      cancelMethod.setAccessible(false);
    }
  }
}
