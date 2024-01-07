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

import java.sql.DriverManager;
import java.util.Collections;

import oracle.jdbc.diagnostics.Diagnostic;
import org.slf4j.Logger;

public class OracleLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(OracleLeakTriggerer.class);

  @Override
  public void run() {
    ClassLoader threadClassloader = Thread.currentThread().getContextClassLoader();
    ClassLoader parentClassloader = threadClassloader.getParent();
    // To avoid race conditions, I wait for the driver to be available.
    await().until(() -> Collections.list(DriverManager.getDrivers()).stream()
        .filter(d -> d.getClass().getName().contains("oracle"))
        .anyMatch(driver -> (driver.getClass().getClassLoader() == threadClassloader
            || driver.getClass().getClassLoader() == parentClassloader)));
    Diagnostic.get("oracle.jdbc", 1000);
    await().until(() -> getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("oracle.jdbc.diagnostics.Diagnostic.CLOCK"))
        .anyMatch(thread -> thread.getContextClassLoader() == threadClassloader
            || thread.getContextClassLoader() == parentClassloader));
  }
}
