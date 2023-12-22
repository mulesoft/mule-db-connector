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

import java.io.IOException;
import java.sql.Driver;
import java.util.Collections;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);
  public static final String DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final Pattern DRIVER_TIMER_THREAD_PATTERN = Pattern.compile("^Timer-\\d+");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    deregisterDB2Drivers(artifactDisposalContext);

    /*(W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
     * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it */
    if (detectDB2Uses(artifactDisposalContext)) {
      disposeDriverTimerThreads(artifactDisposalContext);
    }
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
          disposeDriverTimerThreads(disposalContext);
        } catch (Exception e) {
          LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
        }
      });
  }

  private boolean isDB2Driver(Driver driver) {
    try {
      return driver.getClass().getClassLoader().loadClass("com.ibm.db2.jcc.DB2Driver").isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      // If the class is not found, there is no such driver.
      return false;
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
    //TODO Identificar DB2
    return thread.getClass().getSimpleName().equals(DRIVER_TIMER_THREAD_CLASS_NAME)
        && DRIVER_TIMER_THREAD_PATTERN.matcher(thread.getName()).matches();
  }

  private void disposeTimerThread(Thread thread) {
    // IntervalTimer.cleanup()
    // Connection.cancelQueryTimer()



    try {
      //      clearReferencesStopTimerThread(thread);
      //      thread.interrupt();
      //      thread.join();
    } catch (Throwable e) {
      LOGGER.warn("An error occurred trying to close the '" + thread.getName() + "' Thread. This might cause memory leaks.", e);
    }
  }


}
