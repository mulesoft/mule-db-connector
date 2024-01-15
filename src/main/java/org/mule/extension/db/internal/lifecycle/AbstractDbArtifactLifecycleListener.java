/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.util.Collections;

import org.slf4j.Logger;

public abstract class AbstractDbArtifactLifecycleListener implements ArtifactLifecycleListener {

  Logger LOGGER = getLogger(AbstractDbArtifactLifecycleListener.class);

  protected Boolean isDriver(Driver driver) {
    return true;
  }

  protected void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    return;
  }

  protected void deregisterDrivers(ArtifactDisposalContext disposalContext) {
    Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(this::isDriver)
        .forEach(driver -> {
          try {
            LOGGER.debug("Unregistering driver");
            deregisterDriver(driver);
            additionalCleaning(disposalContext, driver);
          } catch (Exception e) {
            LOGGER.warn("Can not deregister driver. This can cause a memory leak.", e);
          }
        });
  }

}
