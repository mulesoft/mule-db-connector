/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;

import org.mule.extension.db.internal.lifecycle.OracleArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OracleArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "oracle.jdbc";

  public static final List<String> DRIVER_THREAD_NAMES =
      Arrays.asList(new String[] {"oracle.jdbc.diagnostics.Diagnostic.CLOCK"});

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
  public List<String> getDriverThreadNames() {
    return DRIVER_THREAD_NAMES;
  }

  @Override
  protected Class getLeakTriggererClass() {
    return OracleLeakTriggerer.class;
  }

  protected Matcher<Iterable<? super Thread>> hasDriverThreadMatcher(ClassLoader target, boolean negateMatcher) {
    Matcher<Iterable<? super Thread>> matcher =
        hasItem(
                allOf(
                      hasProperty("name", isIn(getDriverThreadNames())),
                      not(isIn(previousThreads))));
    return negateMatcher ? not(matcher) : matcher;
  }
}
