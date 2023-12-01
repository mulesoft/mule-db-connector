/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import org.mule.extension.db.internal.lifecycle.OracleArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
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
    return;
  }

  @Override
  void assertThreadsAreDisposed() {
    Thread[] threads = new Thread[java.lang.Thread.currentThread().getThreadGroup().activeCount()];
    try {
      Thread.enumerate(threads);
    } catch (Throwable t) {
      return;
    }
    for (java.lang.Thread thread : threads) {
      if (thread.getName().startsWith(DRIVER_TIMER_THREAD)
          && Thread.currentThread().getContextClassLoader().equals(thread.getClass().getClassLoader())) {
        Assert.fail(String.format("The thread {} is still present", thread.getName()));
      }
    }
  }
}
