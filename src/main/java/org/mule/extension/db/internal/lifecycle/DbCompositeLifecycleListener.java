/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.String.format;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

public class DbCompositeLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DbCompositeLifecycleListener.class);

  private final List<ArtifactLifecycleListener> delegates = new ArrayList<>();

  public DbCompositeLifecycleListener() {
    delegates.add(new DerbyArtifactLifecycleListener());
    delegates.add(new OracleArtifactLifecycleListener());
    delegates.add(new MySqlArtifactLifecycleListener());
    delegates.add(new DB2ArtifactLifecycleListener());
  }

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    delegates.forEach(x -> x.onArtifactDisposal(artifactDisposalContext));
    // Unregistration of all other drivers
    Collections.list(getDrivers())
        .stream()
        .filter(d -> artifactDisposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            artifactDisposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .forEach(driver -> {
          try {
            deregisterDriver(driver);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
          } catch (Exception e) {
            LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
          }
        });
  }
}
