/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.String.format;
import static java.lang.Thread.getAllStackTraces;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.Field;
import java.sql.Driver;
import java.util.Collections;
import java.util.Timer;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    deregisterDB2Drivers(artifactDisposalContext);

    /*(W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
     * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it */
    cancelTimerThreads(artifactDisposalContext.getArtifactClassLoader());
    cancelTimerThreads(artifactDisposalContext.getExtensionClassLoader());
  }

  private void deregisterDB2Drivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(this::isDB2Driver)
        .forEach(driver -> {
          try {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
            deregisterDriver(driver);
          } catch (Exception e) {
            LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
          }
        });
  }

  private boolean isDB2Driver(Driver driver) {
    try {
      return driver.getClass().getClassLoader().loadClass("com.ibm.db2.jcc.DB2Driver").isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private void cancelTimerThreads(ClassLoader classLoader) {
    LOGGER.debug("Timer threads founded: {}",
                 getAllStackTraces().keySet().stream().map(Thread::getName).anyMatch(s -> s.startsWith("Timer-")));
    try {
      Class<?> diagnosticClass = Class.forName("com.ibm.db2.jcc.am.lg", true, classLoader);
      if (classLoader == diagnosticClass.getClassLoader()) {
        Field clockField = diagnosticClass.getDeclaredField("a");
        Boolean accessibility = clockField.isAccessible();
        clockField.setAccessible(true);
        Timer clockValue = (Timer) clockField.get(null);
        clockValue.cancel();
        LOGGER.debug("Cancelling DB2's Timer Threads on classloader: {}", classLoader.toString());
        clockField.setAccessible(accessibility);
      }
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      LOGGER.debug("Error attempting to cancel DB2's Timer Threads", e);
    }
  }
}
