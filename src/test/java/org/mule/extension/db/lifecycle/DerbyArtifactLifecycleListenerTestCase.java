/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.Thread.getAllStackTraces;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;

import org.mule.extension.db.internal.lifecycle.DerbyArtifactLifecycleListener;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DerbyArtifactLifecycleListenerTestCase extends AbstractArtifactLifecycleListenerTestCase {

  public static final String DRIVER_PACKAGE = "org.apache.derby";
  public static final String DRIVER_THREAD_NAME = "derby.rawStoreDaemon";

  private List<Thread> previousThreads = new ArrayList<>();

  @Before
  /* The thread that Derby creates when using the in-memory base, does not contain information of the contextClassloader
  in which it was created, this field is set to null. To test that the cleaning of the resourceReleaser does not delete
  threads that correspond to other instances, I create a connection to a different database before starting the test
  which should not be affected after the cleaning. As this connection is made with the driver loaded by SPI I cannot
  close it in the After Test because I would be unregistering a driver that may be required by another test suite. */
  public void addOtherDatabaseThreadInTheContext() throws Exception {
    DriverManager.getConnection("jdbc:derby:previousDB;create=true;user=me;password=mine");
    await().until(() -> getAllStackTraces().keySet().stream()
        .anyMatch(thread -> thread.getName().startsWith(getDriverThreadName())));
    previousThreads = getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith(getDriverThreadName())).collect(Collectors.toList());
  }

  @After
  public void checkTheOtherDatabaseThreadInTheContext() throws Exception {
    assertThat(getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith(getDriverThreadName())).collect(Collectors.toList()),
               containsInAnyOrder(previousThreads.toArray()));
  }

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
  public String getDriverThreadName() {
    return DRIVER_THREAD_NAME;
  }

  @Override
  public Class getLeakTriggererClass() {
    return DerbyLeakTriggerer.class;
  }

  protected Matcher<Iterable<? super Thread>> hasDriverThreadMatcher(ClassLoader target, boolean negateMatcher) {
    Matcher<Iterable<? super Thread>> matcher =
        hasItem(
                allOf(
                      hasProperty("name", is(getDriverThreadName())),
                      not(isIn(previousThreads))));
    return negateMatcher ? not(matcher) : matcher;
  }
}
