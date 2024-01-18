/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.beans.Introspector.flushCaches;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.slf4j.Logger;

/* TODO: W-14821871: This class should be abstract, but currently, if I change it, the abstract class will not be in the same classloader as the ArtifactLifecycleListener that is using it and a visibility error occurs.*/
public class DbArtifactLifecycleListenerCommons implements ArtifactLifecycleListener {

  protected DbArtifactLifecycleListenerCommons() {}

  private static final Logger LOGGER = getLogger(DbArtifactLifecycleListenerCommons.class);

  /* TODO: W-14821871: This method is not implemented here because this class is not in the same classloader of the ArtifactLifecycleListener that uses it and therefore, the DriverManager.getDrivers() method returns a different listing if I run it here or run it inside the ArtifactLifecycleListener.*/
  protected Stream<Driver> getDriversStream() {
    return Stream.empty();
  }

  /* TODO: W-14821871: This method is not implemented here because this class is not in the same classloader of the ArtifactLifecycleListener that uses it and therefore, the DriverManager.deregisterDriver() method fails if I run it here, but it runs well inside the ArtifactLifecycleListener.*/
  protected void unregisterDriver(Driver driver) throws SQLException {
    return;
  }

  protected void deregisterDrivers(ArtifactDisposalContext disposalContext) {
    getDriversStream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(this::isDriver)
        .forEach(driver -> {
          try {
            unregisterDriver(driver);
            additionalCleaning(disposalContext, driver);
          } catch (Exception e) {
            LOGGER.warn("Can not deregister driver. This can cause a memory leak.", e);
          }
        });
    cleanCaches(disposalContext);
  }

  protected boolean isDriver(Driver driver) {
    return Arrays.stream(getDriverNames()).anyMatch(name -> name.equals(driver.getClass().getName()));
  }

  protected void cleanCaches(ArtifactDisposalContext disposalContext) {
    flushCaches();
    ResourceBundle.clearCache(disposalContext.getArtifactClassLoader());
    ResourceBundle.clearCache(disposalContext.getExtensionClassLoader());
  }

  protected void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    return;
  }

  protected String[] getDriverNames() {
    return new String[] {};
  }

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {

  }
}
