/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import static java.lang.Boolean.getBoolean;
import static java.lang.Integer.toHexString;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.sql.DriverManager.getDrivers;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;

import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Timer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;

public class OracleArtifactLifecycleListener extends DbArtifactLifecycleListenerCommons {

  private static final Logger LOGGER = getLogger(OracleArtifactLifecycleListener.class);
  private static final String[] DRIVER_NAMES = {"oracle.jdbc.OracleDriver"};

  public static final String DIAGNOSABILITY_BEAN_NAME = "diagnosability";
  /*This property was used in the resource releasers that were on the runtime side.*/
  private static final String AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME = "mule.db.connector.oracle.avoid.dispose.threads";
  private static final boolean AVOID_DISPOSE_TIMER_THREADS =
      getBoolean(AVOID_DISPOSE_TIMER_THREADS_PROPERTY_NAME);

  private static final String AVOID_REMOVE_LOGGER_FORMATTER_PROPERTY_NAME =
      "mule.db.connector.oracle.avoid.remove.logger.formatter";
  private static final boolean AVOID_REMOVE_LOGGER_FORMATTER =
      getBoolean(AVOID_REMOVE_LOGGER_FORMATTER_PROPERTY_NAME);


  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.debug("Running onArtifactDisposal method on OracleArtifactLifecycleListener");
    deregisterDrivers(artifactDisposalContext);
  }

  public String[] getDriverNames() {
    return DRIVER_NAMES;
  }

  @Override
  public Stream<Driver> getDriversStream() {
    return Collections.list(getDrivers()).stream();
  }

  @Override
  public void unregisterDriver(Driver driver) throws SQLException {
    DriverManager.deregisterDriver(driver);
  }

  public void additionalCleaning(ArtifactDisposalContext disposalContext, Driver driver) {
    checkingVersionsWithLeaksKnownSolvedInNewerVersions(driver);
    if (!AVOID_REMOVE_LOGGER_FORMATTER) {
      removeOracleSimpleFormatter(disposalContext);
    }
    if (!AVOID_DISPOSE_TIMER_THREADS) {
      cleanClassloader(disposalContext.getArtifactClassLoader());
      cleanClassloader(disposalContext.getExtensionClassLoader());
    }
  }

  private void checkingVersionsWithLeaksKnownSolvedInNewerVersions(Driver driver) {
    int major = driver.getMajorVersion();
    int minor = driver.getMinorVersion();
    if (major < 19 || (major == 19 && minor < 14)) {
      LOGGER.warn("Oracle Driver prior to 19.4 have a known issue " +
          "whereby Thread Leaks are generated. Consider upgrading to a newer version of the driver.");
    }
  }

  private void cleanClassloader(ClassLoader classloader) {
    deregisterOracleDiagnosabilityMBean(classloader);
    cancelTimerThreads(classloader);
  }

  private void deregisterOracleDiagnosabilityMBean(ClassLoader cl) {
    MBeanServer mBeanServer = getPlatformMBeanServer();
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", DIAGNOSABILITY_BEAN_NAME);
    // In this MBean we have been fortunate that the oracle people have concatenated the hashCode of the classloader
    // in the bean name so it is easily identifiable to avoid deleting other MBeans.
    keys.put("name", cl.getClass().getName() + "@" + toHexString(cl.hashCode()).toLowerCase());
    try {
      mBeanServer.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
    } catch (javax.management.InstanceNotFoundException e) {
      LOGGER.debug("No Oracle's MBean found.");
    } catch (Throwable e) {
      LOGGER.debug("Unable to unregister Oracle's MBeans");
    }
  }

  private void cancelTimerThreads(ClassLoader classLoader) {
    try {
      Class<?> diagnosticClass = Class.forName("oracle.jdbc.diagnostics.Diagnostic", true, classLoader);
      Field clockField = diagnosticClass.getDeclaredField("CLOCK");
      Boolean accessibility = clockField.isAccessible();
      clockField.setAccessible(true);
      Timer clockValue = (Timer) clockField.get(null);
      clockValue.cancel();
      clockField.setAccessible(accessibility);
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      LOGGER.debug("Unable to cancel oracle.jdbc.diagnostics.Diagnostic.CLOCK Timer Thread");
    }
  }

  private void removeOracleSimpleFormatter(ArtifactDisposalContext disposalContext) {
    java.util.logging.Logger l = java.util.logging.Logger.getLogger("test");
    while (l != null && (l.getHandlers()).length == 0)
      l = l.getParent();
    Handler h = (l == null) ? null : l.getHandlers()[0];
    Formatter f = h.getFormatter();
    if (disposalContext.getExtensionClassLoader().equals(f.getClass().getClassLoader())
        || disposalContext.getArtifactClassLoader().equals(f.getClass().getClassLoader())) {
      SimpleFormatter s = new SimpleFormatter();
      h.setFormatter(s);
    }
  }
}
