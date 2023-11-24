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

import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;

import org.slf4j.Logger;

public class DerbyArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DerbyArtifactLifecycleListener.class);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on {}", getClass().getName());
    deregisterJdbcDrivers(artifactDisposalContext);
  }

  private void deregisterJdbcDrivers(ArtifactDisposalContext disposalContext) {
    Enumeration<Driver> drivers = getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      // Only unregister drivers that were loaded by the classloader that called this releaser.
      //if (isDriverLoadedByThisClassLoader(artifactDisposalContext, driver)) {
      ClassLoader cls = driver.getClass().getClassLoader();
      if (disposalContext.isArtifactOwnedClassLoader(cls) ||
          disposalContext.isExtensionOwnedClassLoader(cls)) {
        doDeregisterDriver(disposalContext, driver);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER
              .debug(format("Skipping deregister driver %s. It wasn't loaded by the classloader of the artifact being released.",
                            driver.getClass()));
        }
      }
    }
  }

  private boolean isDriverLoadedByThisClassLoader(ArtifactDisposalContext artifactDisposalContext, Driver driver) {
    ClassLoader driverClassLoader = driver.getClass().getClassLoader();
    while (driverClassLoader != null) {
      // It has to be the same reference not equals to
      if (driverClassLoader == artifactDisposalContext.getExtensionClassLoader()) {
        return true;
      }
      driverClassLoader = driverClassLoader.getParent();
    }
    return false;
  }

  private void doDeregisterDriver(ArtifactDisposalContext artifactDisposalContext, Driver driver) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Deregistering driver: {}", driver.getClass());
      }
      deregisterDriver(driver);

      if (isDerbyEmbeddedDriver(driver)) {
        leakPreventionForDerbyEmbeddedDriver(driver);
      }
    } catch (Exception e) {
      LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
    }
  }

  private boolean isDerbyEmbeddedDriver(Driver driver) {
    // This is the dummy driver which is registered with the DriverManager and which is autoloaded by JDBC4
    return isDriver(driver, "org.apache.derby.jdbc.AutoloadedDriver");
  }

  private boolean isDriver(Driver driver, String expectedDriverClass) {
    try {
      return driver.getClass().getClassLoader().loadClass(expectedDriverClass).isAssignableFrom(driver.getClass());
    } catch (ClassNotFoundException e) {
      // If the class is not found, there is no such driver.
      return false;
    }
  }

  private void leakPreventionForDerbyEmbeddedDriver(Object driverObject) {
    try {
      if (hasDeclaredMethod(driverObject.getClass(), "connect", String.class, java.util.Properties.class)) {
        Method m = driverObject.getClass().getDeclaredMethod("connect", String.class, java.util.Properties.class);
        m.invoke(driverObject, "jdbc:derby:;shutdown=true", null);
      }
    } catch (Throwable e) {
      Throwable cause = e.getCause();
      if (cause instanceof SQLException) {
        // A successful shutdown always results in an SQLException to indicate that Derby has shut down and that
        // there is no other exception.
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Expected exception when unregister Derby's embedded driver", e);
        }
      } else {
        LOGGER.warn("Unable to unregister Derby's embedded driver", e);
      }
    }
  }

  private boolean hasDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    try {
      return clazz.getDeclaredMethod(methodName, parameterTypes) != null;
    } catch (NoSuchMethodException ex) {
      return false;
    }
  }

}
