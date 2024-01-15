/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.format;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;

public class DerbyArtifactLifecycleListener extends AbstractDbArtifactLifecycleListener {

  private static final String AVOID_SHUTDOWN_CONNECTION_PROPERTY_NAME = "mule.db.connector.derby.avoid.shutdown.connection";

  private static final boolean AVOID_SHUTDOWN_CONNECTION =
      getBoolean(AVOID_SHUTDOWN_CONNECTION_PROPERTY_NAME);

  private static final Logger LOGGER = getLogger(DerbyArtifactLifecycleListener.class);
  private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";
  private static final String[] DRIVER_NAMES = {"org.apache.derby.jdbc.EmbeddedDriver", "org.apache.derby.jdbc.AutoloadedDriver"};

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on DerbyArtifactLifecycleListener");
    deregisterDerbyDrivers(artifactDisposalContext);
  }

  private void deregisterDerbyDrivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> d.getClass().getName().startsWith(DRIVER_PACKAGE))
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))

        .forEach(driver -> {
          try {
            deregisterDriver(driver);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
            if (isDerbyEmbeddedDriver(driver) && !AVOID_SHUTDOWN_CONNECTION) {
              leakPreventionForDerbyEmbeddedDriver(driver);
            }
          } catch (Exception e) {
            LOGGER.debug(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
          }
        });
  }

  private boolean isDerbyEmbeddedDriver(Driver driver) {
    // This is the dummy driver which is registered with the DriverManager and which is autoloaded by JDBC4
    return Arrays.stream(DRIVER_NAMES).anyMatch(name -> name.equals(driver.getClass().getName()));
  }

  private void leakPreventionForDerbyEmbeddedDriver(Driver driverObject) {
    try {
      driverObject.connect("jdbc:derby:;shutdown=true", null);
    } catch (SQLException e) {
      if (e.getSQLState().equals("XJ015")) {
        // A successful shutdown always results in an SQLException to indicate that Derby has shut down and that
        // there is no other exception.
        LOGGER.debug("(XJ015): Derby system shutdown.");
      } else {
        LOGGER.debug("Unable to shutdown Derby's embedded driver on the DerbyArtifactLifecycleListener", e);
      }
    }
  }
}
