/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static org.mule.extension.db.DbMunitUtils.getDbPort;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import org.mule.extension.db.internal.lifecycle.DerbyArtifactLifecycleListener;
import org.mule.extension.db.internal.lifecycle.OracleArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OracleArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "oracle.jdbc";
  public static final String DRIVER_NAME = "oracle.jdbc.OracleDriver";
  public static final String DRIVER_TIMER_THREAD = "oracle.jdbc.diagnostics.Diagnostic.CLOCK";

  public OracleArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {"com.oracle.database.jdbc", "ojdbc8", "23.2.0.0"}});
  }

  @Override
  Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass() {
    return OracleArtifactLifecycleListener.class;
  }

  @Override
  String getPackagePrefix() {
    return DRIVER_PACKAGE;
  }

  @Override
  public String getDriverName() {
    return DRIVER_NAME;
  }

  @Override
  public String getDriverThreadName() {
    return DRIVER_TIMER_THREAD;
  }

  @Override
  void assertThreadsAreNotDisposed() {
    assertThat(getCurrentThreadNames(), hasReadDriverThread());;
  }

  @Override
  void assertThreadsAreDisposed() {
    assertThat(getCurrentThreadNames(), not(hasReadDriverThread()));
  }

  @Test
  public void testOracleDriver() {
//    try {
//      Class<?> driverClass =
//          Thread.currentThread().getContextClassLoader().loadClass(DRIVER_NAME);
//      Driver oracle = (Driver) driverClass.getDeclaredConstructor().newInstance();
//      DriverManager.registerDriver(oracle);
//    } catch (ReflectiveOperationException | SQLException e) {
//      LOGGER.error(e.getMessage(), e);
//      fail("Could not load the driver");
//    }
    leakTriggerer();
    try {
      OracleArtifactLifecycleListener.class.newInstance().onArtifactDisposal(artifactDisposal);
      assertThreadsAreDisposed();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public void leakTriggerer() {
    try {
      String port = getDbPort("oracle");
      String jdbcUrl = "jdbc:oracle:thin:system/oracle@//127.0.0.1:"
          + (port != null && !port.equals("") ? port : "52554") + "/XEPDB1";
      Connection con = DriverManager.getConnection(jdbcUrl);
      LOGGER.debug("The connection was successfully established using the port {}", port);
      Statement statement = con.createStatement();
      String sql = "SELECT 1 FROM DUAL";
      statement.execute(sql);
      statement.close();
      con.close();
      LOGGER.debug("Select executed!");
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      fail("The SELECT sentence execution failed.");
    }
  }
}
