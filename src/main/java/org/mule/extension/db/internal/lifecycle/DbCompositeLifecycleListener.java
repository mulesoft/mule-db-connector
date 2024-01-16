/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.beans.Introspector.flushCaches;
import static java.lang.Boolean.getBoolean;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;

public class DbCompositeLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DbCompositeLifecycleListener.class);
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

  // TODO: W-14821871 Move this to a common class
  private void deregisterDrivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(this::isDriver)
        .forEach(driver -> {
          try {
            deregisterDriver(driver);
            additionalCleaning(disposalContext, driver);
          } catch (Exception e) {
            LOGGER.warn("Can not deregister driver. This can cause a memory leak.", e);
          }
        });
    cleanCaches(disposalContext);
  }

  // TODO: W-14821871 Move this to a common class
  private void cleanCaches(ArtifactDisposalContext disposalContext) {
    flushCaches();
    ResourceBundle.clearCache(disposalContext.getArtifactClassLoader());
    ResourceBundle.clearCache(disposalContext.getExtensionClassLoader());
  }

  protected boolean isDriver(Driver driver) {
    return Driver.class.isInstance(driver);
  }

  protected void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    return;
  }
}
