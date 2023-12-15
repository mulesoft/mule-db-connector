/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import org.mule.extension.db.internal.lifecycle.DerbyArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DerbyArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "org.apache.derby";
  public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String DRIVER_THREAD_NAME = "derby.rawStoreDaemon";

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
  String getPackagePrefix() {
    return DRIVER_PACKAGE;
  }

  @Override
  public String getDriverName() {
    return DRIVER_NAME;
  }

  @Override
  public String getDriverThreadName() {
    return DRIVER_THREAD_NAME;
  }

  @Override
  public Class getLeakTriggererClass() {
    return DerbyLeakTriggerer.class;
  }

  @Override
  void assertThreadsAreNotDisposed() {
    assertThat(getCurrentThreadNames(), hasReadDriverThread());
  }

  @Override
  void assertThreadsAreDisposed() {
    assertThat(getCurrentThreadNames(), not(hasReadDriverThread()));
  }

}
