/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;

import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Timer;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener extends DbArtifactLifecycleListenerCommons {

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);
  static final String[] DRIVER_NAMES = {"com.ibm.db2.jcc.DB2Driver"};
  private static final String AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME = "mule.db.connector.db2.avoid.cancel.timer.thread";
  private static final boolean AVOID_DISPOSE_TIMER_THREADS =
      getBoolean(AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on DB2ArtifactLifecycleListener");
    deregisterDrivers(disposalContext);
    /*(W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
     * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it */
    additionalCleaning(disposalContext, null);
  }

  public void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    if (!AVOID_DISPOSE_TIMER_THREADS) {
      cancelTimerThreads(disposalContext.getExtensionOwnedThreads());
      cancelTimerThreads(disposalContext.getArtifactOwnedThreads());
    }
  }

  public String[] getDriverNames() {
    return DRIVER_NAMES;
  }

  @Override
  public Stream<Driver> getDriversStream() {
    return Collections.list(getDrivers()).stream();
  }

  @Override
  public void unregisterDriver(Driver driver) throws SQLException {
    DriverManager.deregisterDriver(driver);
  }

  private void cancelTimerThreads(Stream<Thread> threadStream) {
    threadStream.filter(thread -> thread.getName().startsWith("Timer-")).forEach(thread -> {
      LOGGER.debug("DB2's Timer thread founded.");
      try {
        Class<?> diagnosticClass = Class.forName("com.ibm.db2.jcc.am.lg", true, thread.getContextClassLoader());
        // The protected static Timer "a" doesn't belong to any module
        Field clockField = diagnosticClass.getDeclaredField("a");
        Boolean accessibility = clockField.isAccessible();
        clockField.setAccessible(true);
        Timer clockValue = (Timer) clockField.get(null);
        clockValue.cancel();
        LOGGER.debug("Cancelling DB2's Timer Threads");
        clockField.setAccessible(accessibility);
      } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
        LOGGER.debug("Error attempting to cancel DB2's Timer Threads", e);
      }
    });
  }
}
