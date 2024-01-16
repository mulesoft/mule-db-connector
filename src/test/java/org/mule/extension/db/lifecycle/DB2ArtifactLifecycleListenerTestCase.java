/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNot.not;

import org.mule.extension.db.internal.lifecycle.DB2ArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Matcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DB2ArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "com.ibm.db2";
  public static final String DRIVER_THREAD_NAME = "Timer-";

  public DB2ArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"com.ibm.db2.jcc", "db2jcc", "db2jcc4"}});
  }

  @Override
  Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass() {
    return DB2ArtifactLifecycleListener.class;
  }

  @Override
  String getPackagePrefix() {
    return DRIVER_PACKAGE;
  }

  @Override
  public String getDriverThreadName() {
    return DRIVER_THREAD_NAME;
  }

  @Override
  protected Class getLeakTriggererClass() {
    return DB2LeakTriggerer.class;
  }

  @Override
  protected Matcher<Iterable<? super Thread>> hasDriverThreadMatcher(ClassLoader target, boolean negateMatcher) {
    Matcher<Thread> isNotInPreviousThreads = not(isIn(previousThreads));
    Matcher<Thread> nameStartsWithTimer = hasProperty("name", startsWith("Timer-"));
    Matcher<Thread> classLoaderMatcher = hasProperty("contextClassLoader", equalTo(target));
    Matcher<Iterable<? super Thread>> combinedMatcher =
        hasItem(allOf(isNotInPreviousThreads, nameStartsWithTimer, classLoaderMatcher));
    return negateMatcher ? not(combinedMatcher) : combinedMatcher;
  }


}
