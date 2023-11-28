/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.fail;

import org.mule.extension.db.internal.lifecycle.DerbyArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DerbyArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public DerbyArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Override
  Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass() {
    return DerbyArtifactLifecycleListener.class;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"org.apache.derby", "derby", "10.14.2.0"}});
  }

  @Override
  void generateTargetLeak(ClassLoader classLoader) {
    ClassLoader originalTCCL = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(classLoader);
    try {
      Class<?> driverClass = classLoader.loadClass("org.apache.derby.jdbc.EmbeddedDriver");
      Driver embeddedDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
      DriverManager.registerDriver(embeddedDriver);

      driverClass = classLoader.loadClass("org.apache.derby.jdbc.AutoloadedDriver");
      Driver autoloadedDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
      DriverManager.registerDriver(autoloadedDriver);

      Class.forName("org.apache.derby.jdbc.AutoloadedDriver", true, classLoader);

      String urlConnection = "jdbc:derby:myDB;create=true;user=me;password=mine";
      Connection con = DriverManager.getConnection(urlConnection);

      Statement statement = con.createStatement();
      String sql = "SELECT 1 FROM (VALUES(1)) AS DummyTable";
      statement.execute(sql);
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      fail("Connection could not be established");
    } catch (ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      fail("Could not load the driver");
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } finally {
      currentThread().setContextClassLoader(originalTCCL);
    }
  }

  @Override
  String getPackagePrefix() {
    return "org.apache.derby";
  }

  @Override
  Boolean enableLibraryReleaseChecking() {
    return true;
  }

  @Override
  Boolean enableThreadsReleaseChecking() {
    return true;
  }

  @Override
  void assertThreadsAreNotDisposed() {
    return;
  }

  @Override
  void assertThreadsAreDisposed() {
    return;
  }
}
