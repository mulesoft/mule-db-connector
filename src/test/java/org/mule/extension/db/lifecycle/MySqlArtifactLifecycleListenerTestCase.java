/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MySqlArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "com.mysql";
  public static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
  public static final String DRIVER_THREAD_NAME = "mysql-cj-abandoned-connection-cleanup";

  public MySqlArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"mysql", "mysql-connector-java", "8.0.30"}});
  }

  @Override
  Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass() {
    return MySqlArtifactLifecycleListener.class;
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
  void assertThreadsAreNotDisposed() {
    assertTrue("MySQL Store Daemon loaded by domain is not still present", isThreadDaemonPresent());
  }

  @Override
  void assertThreadsAreDisposed() {
    assertFalse("MySQL Store Daemon is still present", isThreadDaemonPresent());
  }

  protected boolean isThreadDaemonPresent() {
    return getAllStackTraces().keySet().stream().map(Thread::getName).collect(toList()).stream()
        .anyMatch(t -> t.equals(getDriverThreadName()));
  }

  public String getDriverThreadName() {
    return DRIVER_THREAD_NAME;
  }

  @Override
  protected Class getLeakTriggererClass() {
    return MySqlLeakTriggerer.class;
  }
}
