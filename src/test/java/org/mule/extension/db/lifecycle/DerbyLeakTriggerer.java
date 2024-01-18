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
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class DerbyLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(DerbyLeakTriggerer.class);

  @Override
  public void run() {
    List<Thread> previousThreads = getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("derby.rawStoreDaemon")).collect(Collectors.toList());
    ClassLoader threadClassloader = Thread.currentThread().getContextClassLoader();
    ClassLoader parentClassloader = threadClassloader.getParent();
    // To avoid race conditions, I wait for the driver to be available.
    await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
        .filter(d -> d.getClass().getName().contains("derby"))
        .anyMatch(driver -> (driver.getClass().getClassLoader() == threadClassloader
            || driver.getClass().getClassLoader() == parentClassloader)));
    try (Connection con = DriverManager.getConnection("jdbc:derby:derbyLeakTriggererDB;create=true;user=me;password=mine")) {
    } catch (SQLException e) {
      LOGGER.error("Connection could not be established: {}", e.getMessage(), e);
      fail("Connection could not be established");
    }
    await().until(() -> getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("derby.rawStoreDaemon"))
        .anyMatch(thread -> !previousThreads.contains(thread)));
  }
}
