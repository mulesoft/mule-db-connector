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
import java.sql.SQLException;
import java.util.Collections;

import oracle.jdbc.diagnostics.Diagnostic;
import org.slf4j.Logger;

public class OracleLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(OracleLeakTriggerer.class);

  @Override
  public void run() {
    // To avoid race conditions, I wait for the driver to be available.
    await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
        .anyMatch(driver -> driver.getClass().getName().contains("oracle")));
//    try (Connection con = DriverManager.getConnection("jdbc:oracle:thin:user/pass@localhost:1521/dummy")){
//    } catch (SQLException e) {
//      LOGGER.debug("The exception is the expected behavior. The Timer thread should have been launched. ");
//    }
    Diagnostic diagnostic = Diagnostic.get("oracle.jdbc", 1000);
    await().until(() -> getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("oracle.jdbc.diagnostics.Diagnostic.CLOCK"))
        .anyMatch(thread -> thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader().getParent()
            || thread.getContextClassLoader() == Thread.currentThread().getContextClassLoader()));
  }
}
