/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.sql.DriverManager.getDrivers;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class DbCompositeLifecycleListener extends AbstractDbArtifactLifecycleListener {

  private static final String AVOID_ARTIFACT_DISPOSERS_PROPERTY_NAME = "mule.db.connector.avoid.all.artifact.disposers";
  private static final boolean AVOID_ARTIFACT_DISPOSERS =
      getBoolean(AVOID_ARTIFACT_DISPOSERS_PROPERTY_NAME);

  private final List<ArtifactLifecycleListener> delegates = new ArrayList<>();

  public DbCompositeLifecycleListener() {
    delegates.add(new DerbyArtifactLifecycleListener());
    delegates.add(new OracleArtifactLifecycleListener());
    delegates.add(new MySqlArtifactLifecycleListener());
    delegates.add(new DB2ArtifactLifecycleListener());
  }

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    if (!AVOID_ARTIFACT_DISPOSERS) {
      delegates.forEach(x -> x.onArtifactDisposal(disposalContext));
      // Unregistration of all other drivers
      deregisterDrivers(disposalContext);
    }
  }

  @Override
  protected Stream<Driver> getDriversStream() {
    return Collections.list(getDrivers()).stream();
  }

  @Override
  protected void unregisterDriver(Driver driver) throws SQLException {
    DriverManager.deregisterDriver(driver);
  }

  protected boolean isDriver(Driver driver) {
    return Driver.class.isInstance(driver);
  }

  protected void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    return;
  }
}
