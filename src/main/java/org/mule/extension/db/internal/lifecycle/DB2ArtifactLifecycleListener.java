/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);


  /* This system property should be avoid.dispose.timer.threads because there are others drivers that also uses them, but we
   * cannot change it due to backward compatibility */
  private static final String AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME = "avoid.dispose.oracle.threads";
  private static final boolean JDBC_RESOURCE_RELEASER_AVOID_DISPOSE_TIMER_THREADS =
      getBoolean(AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME);
  public static final String DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final Pattern DRIVER_TIMER_THREAD_PATTERN = Pattern.compile("^Timer-\\d+");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    /*
     * (W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
     * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it
     */
    if (detectDB2Uses(artifactDisposalContext)) {
      disposeDriverTimerThreads(artifactDisposalContext);
    }
  }

  private boolean detectDB2Uses(ArtifactDisposalContext artifactDisposalContext) {
    try {
      return artifactDisposalContext.getArtifactClassLoader().getResources("com/ibm/db2").hasMoreElements();
    } catch (IOException e) {
      return false;
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
