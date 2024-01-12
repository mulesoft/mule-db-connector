/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.getAllStackTraces;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;

import org.slf4j.Logger;

public class DB2LeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(DB2LeakTriggerer.class);

  public static final String DRIVER_NAME = "com.ibm.db2.jcc.DB2Driver";

  @Override
  public void run() {
    ClassLoader threadClassloader = Thread.currentThread().getContextClassLoader();
    ClassLoader parentClassloader = threadClassloader.getParent();
    // As we don't have the DB2's dependency in the pom, the SPI mechanism doesn't discover the driver automatically.
    try {
      Class<?> driverClass = threadClassloader.loadClass(DRIVER_NAME);
      driverClass.newInstance();
      await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
          .filter(d -> d.getClass().getName().contains("db2"))
          .anyMatch(driver -> (driver.getClass().getClassLoader() == threadClassloader
              || driver.getClass().getClassLoader() == parentClassloader)));
    } catch (Exception e2) {
      LOGGER.error(e2.getMessage(), e2);
    }
    try (Connection con = DriverManager.getConnection("jdbc:db2://localhost:50000/dummy:user=usuario;password=password;")) {
    } catch (Exception e) {
      LOGGER.debug("The exception is the expected behavior. The Timer thread should have been launched. ");
    }
    await().until(() -> getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("Timer-"))
        .anyMatch(thread -> thread.getContextClassLoader() == threadClassloader
            || thread.getContextClassLoader() == parentClassloader));
  }
}
