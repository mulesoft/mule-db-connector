/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.currentThread;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.extension.db.internal.lifecycle.OracleArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MySqlArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public MySqlArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"mysql", "mysql-connector-java", "8.0.30"}});
  }

  @Override
  ArtifactLifecycleListener getArtifactLifecycleListener() {
    return new MySqlArtifactLifecycleListener();
  }

  @Override
  void generateTargetLeak(ClassLoader classLoader) {
    ClassLoader originalTCCL = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(classLoader);
    try {
      Connection connection = null;
      Class<?> mySqlDriver = classLoader.loadClass("com.mysql.cj.jdbc.Driver");
      Driver driver = (Driver) mySqlDriver.newInstance();
      // DriverManager.registerDriver(driver);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      // assertThat(DriverManager.getDrivers(), hasMySqlDriver());
    } finally {
      currentThread().setContextClassLoader(originalTCCL);
    }
  }

  @Override
  String getPackagePrefix() {
    return "com.mysql";
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
