/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.String.format;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

public class MySqlArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(MySqlArtifactLifecycleListener.class);
  public static final List<String> CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES =
      Arrays.asList("com.mysql.jdbc.AbandonedConnectionCleanupThread", "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    deregisterJdbcDrivers(artifactDisposalContext);
    shutdownMySqlAbandonedConnectionCleanupThread(artifactDisposalContext);
    if (getJavaVersion() <= 11.0F) {
      // codigo no compatible con java 17
    } else {
      LOGGER
          .warn("Some methods of the DB connector resource release could not be executed due to JAVA 17 restrictions. If any driver or library cannot be properly cleaned when undeploying or restarting the application, it could result in an Out of Memory error. Please verify that you are using the latest compatible version of your jdbc driver, and the memory behavior on redeploys. ");
    }
  }

  private void deregisterJdbcDrivers(ArtifactDisposalContext artifactDisposalContext) {
    Enumeration<Driver> drivers = getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      // Only unregister drivers that were loaded by the classloader that called this releaser.
      if (isDriverLoadedByThisClassLoader(artifactDisposalContext, driver)) {
        doDeregisterDriver(artifactDisposalContext, driver);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER
              .debug(format("Skipping deregister driver %s. It wasn't loaded by the classloader of the artifact being released.",
                            driver.getClass()));
        }
      }
    }
  }

  private boolean isDriverLoadedByThisClassLoader(ArtifactDisposalContext artifactDisposalContext, Driver driver) {
    ClassLoader driverClassLoader = driver.getClass().getClassLoader();
    while (driverClassLoader != null) {
      // It has to be the same reference not equals to
      if (driverClassLoader == artifactDisposalContext.getExtensionClassLoader()) {
        return true;
      }
      driverClassLoader = driverClassLoader.getParent();
    }
    return false;
  }

  private void doDeregisterDriver(ArtifactDisposalContext artifactDisposalContext, Driver driver) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Deregistering driver: {}", driver.getClass());
      }
      if (isMySqlDriver(driver)) {
        deregisterDriver(driver);
        shutdownMySqlAbandonedConnectionCleanupThread(artifactDisposalContext);
      }
    } catch (Exception e) {
      LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
    }
  }

  private boolean isMySqlDriver(Driver driver) {
    return isDriver(driver, "com.mysql.jdbc.Driver") || isDriver(driver, "com.mysql.cj.jdbc.Driver");
  }

  private boolean isDriver(Driver driver, String expectedDriverClass) {
    try {
      return driver.getClass().getClassLoader().loadClass(expectedDriverClass).isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      // If the class is not found, there is no such driver.
      return false;
    }
  }

  /**
   * Workaround for http://bugs.mysql.com/bug.php?id=65909
   */
  private void shutdownMySqlAbandonedConnectionCleanupThread(ArtifactDisposalContext artifactDisposalContext) {
    try {
      Class<?> cleanupThreadsClass = findMySqlDriverClass(artifactDisposalContext);
      shutdownMySqlConnectionCleanupThreads(cleanupThreadsClass);
      // The cleanup threads are fired from a single-thread ThreadPoolExecutor, which is created inside a
      // lambda, which wraps the thread pool into a finalizable wrapper. This leads to retention in the
      // artifact classloaders when several redeployments are performed.
      cleanMySqlCleanupThreadsThreadFactory(cleanupThreadsClass);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOGGER.warn("Unable to shutdown MySql's AbandonedConnectionCleanupThread", e);
    }
  }

  /**
   * Cleans a reference existing from the cleanupThread's class to a lambda-created-threadPoolExecutor, which retains a reference
   * to the DB connector artifact classLoader.
   *
   * @param cleanupThreadsClass The AbandonedConnectionCleanupThread class object
   */
  private void cleanMySqlCleanupThreadsThreadFactory(Class<?> cleanupThreadsClass) {
    // In new mysql driver versions (at least 8), the executor service is wrapped inside a delegate class
    // (DelegatedExecutorService) that exposes only the ExecutorService interface. In order to clean the threadPoolExecutor
    // classloader reference, it has to be extracted manually though reflection from each delegate/wrapper class.
    // Hierarchy leading to real ThreadPoolExecutor is: AbandonedConnectionCleanupThread.cleanupThreadExecutorService ->
    // class DelegatedExecutorService.e -> class ThreadPoolExecutor.
    // Note that the field 'cleanupThreadExcecutorService' is mispelled. There's actually a typo in MySql driver code.
    try {
      Method checkedShutdown = cleanupThreadsClass.getMethod("checkedShutdown", null);
      checkedShutdown.invoke(null);

      Field cleanupExecutorServiceField = cleanupThreadsClass
          .getDeclaredField("cleanupThreadExcecutorService");
      cleanupExecutorServiceField.setAccessible(true);
      ExecutorService delegateCleanupExecutorService =
          (ExecutorService) cleanupExecutorServiceField.get(cleanupThreadsClass);

      Field realExecutorServiceField = delegateCleanupExecutorService.getClass().getSuperclass().getDeclaredField("e");
      realExecutorServiceField.setAccessible(true);
      ThreadPoolExecutor realExecutorService =
          (ThreadPoolExecutor) realExecutorServiceField.get(delegateCleanupExecutorService);

      // Set cleanup thread executor service thread factory to one whose classloader is the system one
      realExecutorService.setThreadFactory(Executors.defaultThreadFactory());
    } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      LOGGER.warn("Error cleaning threadFactory from AbandonedConnectionCleanupThread executor service", e);
    }
  }

  /**
   * Sends a shutdown message to MySql's connection cleanup thread class.
   *
   * @param classAbandonedConnectionCleanupThread
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   */
  private void shutdownMySqlConnectionCleanupThreads(Class<?> classAbandonedConnectionCleanupThread)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    try {
      Method uncheckedShutdown = classAbandonedConnectionCleanupThread.getMethod("uncheckedShutdown");
      uncheckedShutdown.invoke(null);
    } catch (NoSuchMethodException e) {
      Method checkedShutdown = classAbandonedConnectionCleanupThread.getMethod("shutdown");
      checkedShutdown.invoke(null);
    }
  }

  /**
   * Tries to find the MySql driver AbandonedConnectionCleanupThread class, with the known class addresses.
   *
   * @return The MySql driver AbandonedConnectionCleanupThread class object, if found.
   */
  private Class<?> findMySqlDriverClass(ArtifactDisposalContext artifactDisposalContext) throws ClassNotFoundException {
    for (String knownCleanupThreadClassAddress : CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES) {
      try {
        return artifactDisposalContext.getArtifactClassLoader().loadClass(knownCleanupThreadClassAddress);
      } catch (ClassNotFoundException e) {
        LOGGER.warn("No AbandonedConnectionCleanupThread registered with class address {}", knownCleanupThreadClassAddress);
      }
    }
    throw new ClassNotFoundException("No MySql's AbandonedConnectionCleanupThread class was found");
  }

  @Deprecated
  private static Float getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    LOGGER.info("Java version {}", version);
    return Float.parseFloat(version);
  }
}
