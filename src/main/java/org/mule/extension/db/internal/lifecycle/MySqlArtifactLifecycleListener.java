/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.beans.Introspector.flushCaches;
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
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

public class MySqlArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(MySqlArtifactLifecycleListener.class);
  private static final List<String> CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES =
      Arrays.asList("com.mysql.jdbc.AbandonedConnectionCleanupThread", "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on MySqlArtifactLifecycleListener");
    deregisterMySqlDrivers(artifactDisposalContext);
    flushCaches();
    ResourceBundle.clearCache(artifactDisposalContext.getArtifactClassLoader());
    ResourceBundle.clearCache(artifactDisposalContext.getExtensionClassLoader());
  }

  private void deregisterMySqlDrivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(this::isMySqlDriver)
        .forEach(driver -> {
          try {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
            deregisterDriver(driver);
            shutdownMySqlAbandonedConnectionCleanupThread(disposalContext);
          } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
            }
          }
        });
  }

  private boolean isMySqlDriver(Driver driver) {
    return isDriver(driver, "com.mysql.jdbc.Driver")
        || isDriver(driver, "com.mysql.cj.jdbc.Driver");
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
      Class<?> cleanupThreadsClass = findMySqlCleanUpClass(artifactDisposalContext);
      shutdownMySqlConnectionCleanupThreads(cleanupThreadsClass);
      // The cleanup threads are fired from a single-thread ThreadPoolExecutor, which is created inside a
      // lambda, which wraps the thread pool into a finalizable wrapper. This leads to retention in the
      // artifact classloaders when several redeployments are performed.
      cleanMySqlCleanupThreadsThreadFactory(cleanupThreadsClass);
    } catch (ClassNotFoundException | SecurityException | IllegalArgumentException
        | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      LOGGER.debug("Unable to shutdown MySql's AbandonedConnectionCleanupThread", e);
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

      if (getJavaVersion() <= 11.0F) {
        /* According to this code (https://jar-download.com/artifacts/mysql/mysql-connector-java/5.1.49/source-code/com/mysql/jdbc/AbandonedConnectionCleanupThread.java),
        calling the checkedShutdown method should be sufficient (the tests pass without this
        part with versions 5 and 8).
        I'll keep this code only to avoid memory leaks in versions older than 5
        because I couldn't reproduce the situation in which this code is necessary.
         */
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
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
      LOGGER.debug("Error cleaning threadFactory from AbandonedConnectionCleanupThread executor service", e);
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
  private Class<?> findMySqlCleanUpClass(ArtifactDisposalContext artifactDisposalContext) throws ClassNotFoundException {
    for (String knownCleanupThreadClassAddress : CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES) {
      try {
        return Class.forName(knownCleanupThreadClassAddress, true, artifactDisposalContext.getExtensionClassLoader());
      } catch (ClassNotFoundException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("No AbandonedConnectionCleanupThread registered with class address {}", knownCleanupThreadClassAddress);
        }
      }
    }
    throw new ClassNotFoundException("No MySql's AbandonedConnectionCleanupThread class was found");
  }

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
    return Float.parseFloat(version);
  }
}
