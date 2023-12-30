/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.getAllStackTraces;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

import org.slf4j.Logger;

public class DerbyLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(DerbyLeakTriggerer.class);

  @Override
  public void run() {
    // To avoid race conditions, I wait for the driver to be available.
    await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
        .anyMatch(driver -> driver.getClass().getName().contains("derby")));
    try (Connection con = DriverManager.getConnection("jdbc:derby:myDB;create=true;user=me;password=mine")) {
    } catch (SQLException e) {
      LOGGER.error("Connection could not be established: {}", e.getMessage(), e);
      fail("Connection could not be established");
    }
    await().until(() -> getAllStackTraces().keySet().stream()
            .anyMatch(thread -> thread.getName().startsWith("derby.rawStoreDaemon")));
    /*
      getAllStackTraces().keySet().stream().filter(thread -> thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader()).map(thread -> thread.getClass().getName()).collect(Collectors.toList())

      getAllStackTraces().keySet().stream().filter(thread -> thread.getClass().getClassLoader() == Thread.currentThread().getContextClassLoader()).map(thread -> thread.getClass().getName()).collect(Collectors.toList())

      getAllStackTraces().keySet().stream().filter(thread -> thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader())
.collect(Collectors.toList())

      0 = "java.util.TimerThread"
      1 = "java.lang.Thread"
* */

  }
}
