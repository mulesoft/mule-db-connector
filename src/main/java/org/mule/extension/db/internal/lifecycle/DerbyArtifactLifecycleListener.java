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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class DerbyArtifactLifecycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = getLogger(DerbyArtifactLifecycleListener.class);
  private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";
  private static final String[] DRIVER_NAMES = {"org.apache.derby.jdbc.EmbeddedDriver", "org.apache.derby.jdbc.AutoloadedDriver"};

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on DerbyArtifactLifecycleListener");
    deregisterDerbyDrivers(artifactDisposalContext);
  }

  private void deregisterDerbyDrivers(ArtifactDisposalContext disposalContext) {
    List<java.sql.Driver> drivers = Collections.list(getDrivers())
        .stream()
        .filter(d -> disposalContext.isArtifactOwnedClassLoader(d.getClass().getClassLoader()) ||
            disposalContext.isExtensionOwnedClassLoader(d.getClass().getClassLoader()))
        .filter(d -> d.getClass().getName().startsWith(DRIVER_PACKAGE)).collect(Collectors.toList());
        for(java.sql.Driver driver:drivers)  {
          try {
            driver.getClass().getClassLoader();
            deregisterDriver(driver);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deregistering driver: {}", driver.getClass());
            }
            if (isDerbyEmbeddedDriver(driver)) {
              leakPreventionForDerbyEmbeddedDriver(driver);
            }
          } catch (Exception e) {
            LOGGER.warn(format("Can not deregister driver %s. This can cause a memory leak.", driver.getClass()), e);
          }
        }
  }

  private boolean isDerbyEmbeddedDriver(Driver driver) {
    // This is the dummy driver which is registered with the DriverManager and which is autoloaded by JDBC4
    return Arrays.stream(DRIVER_NAMES).anyMatch(name -> name.equals(driver.getClass().getName()));
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
    } catch (NoSuchMethodException | IllegalAccessException e) {
      //LOGGER.warn("Unable to shutdown Derby's embedded driver", e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof SQLException
          && ((SQLException) e.getCause()).getSQLState().equals("XJ015")) {
        // A successful shutdown always results in an SQLException to indicate that Derby has shut down and that
        // there is no other exception.
        //LOGGER.debug("(XJ015): Derby system shutdown.");
      } else {
        //LOGGER.warn("Unable to shutdown Derby's embedded driver", e);
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
