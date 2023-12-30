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

  @Override
  public void run() {
      // To avoid race conditions, I wait for the driver to be available.
      await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
          .anyMatch(driver -> driver.getClass().getName().contains("db2")));
      try (Connection con = DriverManager.getConnection("jdbc:db2://localhost:50000/dummy:user=usuario;password=password;")){
      } catch (Exception e) {
        LOGGER.debug("The exception is the expected behavior. The Timer thread should have been launched. ");
        await().until(() -> getAllStackTraces().keySet().stream()
          .filter(thread -> thread.getName().startsWith("Timer-"))
          .anyMatch(thread -> thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader().getParent()
              || thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader()));
      }
  }
}
