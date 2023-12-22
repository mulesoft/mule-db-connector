/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;

import org.slf4j.Logger;

public class DB2LeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(DB2LeakTriggerer.class);

  @Override
  public void run() {
    try {
      Class<?> driverClass = Thread.currentThread().getContextClassLoader().loadClass(DB2ArtifactLifecycleListenerTestCase.DRIVER_NAME);
      Driver driver = (Driver) driverClass.newInstance();
      DriverManager.registerDriver(driver);
      LOGGER.warn("Drivers found: {}", Collections.list(DriverManager.getDrivers()).size());
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
