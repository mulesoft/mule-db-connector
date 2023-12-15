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
import java.sql.SQLException;
import java.util.Collections;

import oracle.jdbc.diagnostics.Diagnostic;
import org.slf4j.Logger;

public class OracleLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(OracleLeakTriggerer.class);

  @Override
  public void run() {
    try {
      Class<?> driverClass =
              Thread.currentThread().getContextClassLoader().loadClass(OracleArtifactLifecycleListenerTestCase.DRIVER_NAME);
      Driver oracle = (Driver) driverClass.getDeclaredConstructor().newInstance();
      DriverManager.registerDriver(oracle);
    } catch (ReflectiveOperationException | SQLException e) {
      LOGGER.error(e.getMessage(), e);
    }
    Diagnostic diagnostic = Diagnostic.get("oracle.jdbc", 1000);
  }
}
