/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class DerbyArtifactLifecycleListener extends DbArtifactLifecycleListenerCommons {

  private static final Logger LOGGER = getLogger(DerbyArtifactLifecycleListener.class);
  private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";
  private static final String[] DRIVER_NAMES = {"org.apache.derby.jdbc.EmbeddedDriver", "org.apache.derby.jdbc.AutoloadedDriver"};
  private static final String AVOID_SHUTDOWN_CONNECTION_PROPERTY_NAME = "mule.db.connector.derby.avoid.shutdown.connection";
  private static final boolean AVOID_SHUTDOWN_CONNECTION =
      getBoolean(AVOID_SHUTDOWN_CONNECTION_PROPERTY_NAME);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on DerbyArtifactLifecycleListener");
    deregisterDrivers(artifactDisposalContext);
  }

  public String[] getDriverNames() {
    return DRIVER_NAMES;
  }

  public void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    if (isDriver(driver) && !AVOID_SHUTDOWN_CONNECTION) {
      leakPreventionForDerbyEmbeddedDriver(driver);
    }
  }

  @Override
  public Stream<Driver> getDriversStream() {
    return Collections.list(getDrivers()).stream();
  }

  @Override
  public void unregisterDriver(Driver driver) throws SQLException {
    DriverManager.deregisterDriver(driver);
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
