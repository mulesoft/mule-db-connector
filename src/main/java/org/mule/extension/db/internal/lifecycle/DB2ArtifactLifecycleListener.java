/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class DB2ArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DB2ArtifactLifecycleListener.class);

  public static final String DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";
  public static final Pattern DRIVER_TIMER_THREAD_PATTERN = Pattern.compile("^Timer-\\d+");

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());

    //TODO INCOMPLETO
    /*
     * (W-12460123) When we have a DB2 driver in the application: Due to in this class getDrivers() method does not return any
     * values when we had a DB2 driver, we found the TimerThread that it triggers for canceling it */
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
    try {
      //      clearReferencesStopTimerThread(thread);
      //      thread.interrupt();
      //      thread.join();
    } catch (Throwable e) {
      LOGGER.warn("An error occurred trying to close the '" + thread.getName() + "' Thread. This might cause memory leaks.", e);
    }
  }


}
