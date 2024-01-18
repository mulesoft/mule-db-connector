/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.getAllStackTraces;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MySqlArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "com.mysql";
  public static final List<String> DRIVER_THREAD_NAMES =
      Arrays.asList(new String[] {"mysql-cj-abandoned-connection-cleanup",
          "Abandoned connection cleanup thread"});


  public MySqlArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    super(groupId, artifactId, version);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"com.mysql", "mysql-connector-j", "8.0.33"},
        {"mysql", "mysql-connector-java", "8.0.30"},
        {"mysql", "mysql-connector-java", "6.0.6"}});
  }

  @Override
  Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass() {
    return MySqlArtifactLifecycleListener.class;
  }

  @Override
  String getPackagePrefix() {
    return DRIVER_PACKAGE;
  }

  public List<String> getDriverThreadNames() {
    return DRIVER_THREAD_NAMES;
  }

  @Override
  protected Class getLeakTriggererClass() {
    return MySqlLeakTriggerer.class;
  }

  @Before
  public void getPreviousThreads() throws Exception {
    await().until(() -> Collections.list(
                                         DriverManager.getDrivers())
        .stream()
        .anyMatch(d -> d.getClass().getName().contains("mysql")));
    await().until(() -> getAllStackTraces().keySet().stream()
        .anyMatch(thread -> getDriverThreadNames().contains(thread.getName())));
    previousThreads = getAllStackTraces().keySet().stream()
        .filter(thread -> getDriverThreadNames().contains(thread.getName()))
        .collect(Collectors.toList());
  }

  protected Matcher<Iterable<? super Thread>> hasDriverThreadMatcher(ClassLoader target, boolean negateMatcher) {
    Matcher<Iterable<? super Thread>> matcher = hasItem(
                                                        allOf(
                                                              anyOf(hasProperty("contextClassLoader", equalTo(target)),
                                                                    hasProperty("contextClassLoader",
                                                                                equalTo(target.getParent()))),
                                                              hasProperty("name", isIn(getDriverThreadNames()))));
    return negateMatcher ? not(matcher) : matcher;
  }
}
