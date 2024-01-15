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

import java.lang.reflect.Field;
import java.sql.Driver;
import java.util.Timer;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener extends AbstractDbArtifactLifecycleListener {

  private static final String AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME = "mule.db.connector.db2.avoid.cancel.timer.thread";
  private static final boolean AVOID_DISPOSE_TIMER_THREADS =
      getBoolean(AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME);

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on DB2ArtifactLifecycleListener");
    deregisterDrivers(artifactDisposalContext);
  }

  public Boolean isDriver(Driver driver) {
    try {
      return driver.getClass().getClassLoader().loadClass("com.ibm.db2.jcc.DB2Driver").isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    if (!AVOID_DISPOSE_TIMER_THREADS) {
      /*(W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
       * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it */
      cancelTimerThreads(disposalContext.getExtensionOwnedThreads());
      cancelTimerThreads(disposalContext.getExtensionOwnedThreads());
    }
  }

  private void cancelTimerThreads(Stream<Thread> threadStream) {
    threadStream.filter(thread -> thread.getName().startsWith("Timer-")).forEach(thread -> {
      LOGGER.debug("Timer thread founded: {} - {}", thread.getName(), thread.getContextClassLoader());
      try {
        Class<?> diagnosticClass = Class.forName("com.ibm.db2.jcc.am.lg", true, thread.getContextClassLoader());
        Field clockField = diagnosticClass.getDeclaredField("a");
        Boolean accessibility = clockField.isAccessible();
        clockField.setAccessible(true);
        Timer clockValue = (Timer) clockField.get(null);
        clockValue.cancel();
        LOGGER.debug("Cancelling DB2's Timer Threads on classloader: {}", thread.getContextClassLoader().toString());
        clockField.setAccessible(accessibility);
      } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
        LOGGER.debug("Error attempting to cancel DB2's Timer Threads", e);
      }
    });
  }
}
