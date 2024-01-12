/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;


import static org.mule.extension.db.util.CollectableReference.collectedByGc;
import static org.mule.extension.db.util.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.util.CollectableReference;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.IOException;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public abstract class AbstractArtifactLifecycleListenerTestCase {

  protected final Logger LOGGER = getLogger(this.getClass());
  protected String groupId;
  protected String artifactId;
  protected String artifactVersion;
  protected URL libraryUrl;

  // Parameterized
  protected AbstractArtifactLifecycleListenerTestCase(String groupId, String artifactId, String artifactVersion) {
    LOGGER.info("Parameters: {0}, {1}, {2}", groupId, artifactId, artifactVersion);
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.artifactVersion = artifactVersion;
    this.libraryUrl = getDependencyFromMaven(this.groupId,
                                             this.artifactId,
                                             this.artifactVersion);
  }

  abstract Class<? extends ArtifactLifecycleListener> getArtifactLifecycleListenerClass();

  abstract String getPackagePrefix();

  abstract String getDriverThreadName();

  protected Boolean enableLibraryReleaseChecking() {
    return true;
  }

  protected Boolean enableThreadsReleaseChecking() {
    return true;
  }

  protected Class getLeakTriggererClass() {
    return null;
  }

  protected List<Thread> previousThreads = new ArrayList<>();

  @Before
  public void getPreviousThreads() throws Exception {
    previousThreads = getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith(getDriverThreadName())).collect(Collectors.toList());
  }

  @After
  public void checkPreviousThreads() throws Exception {
    if (!previousThreads.isEmpty()) {
      assertThat(getAllStackTraces().keySet().stream()
          .filter(thread -> thread.getName().startsWith(getDriverThreadName())).collect(Collectors.toList()),
                 containsInAnyOrder(previousThreads.toArray()));
    }
  }

  /* ClassLoader Tests */
  @Test
  public void whenDriverIsInAppThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInApp,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInAppExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInDomainThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInDomainExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  /* Thread Tests */
  @Test
  public void whenDriverIsInAppThenThreadsAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInApp,
                                       TestClassLoadersHierarchy::getAppExtensionClassLoader,
                                       true);
  }

  @Test
  public void whenDriverIsInAppExtensionThenThreadsAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                       TestClassLoadersHierarchy::getAppExtensionClassLoader,
                                       true);
  }

  @Test
  public void whenDriverIsInDomainThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       false);
  }

  @Test
  public void whenDriverIsInDomainExtensionThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       false);
  }

  protected Matcher<Iterable<? super Thread>> hasDriverThreadMatcher(ClassLoader target, boolean negateMatcher) {
    Matcher<Iterable<? super Thread>> matcher = hasItem(
                                                        hasProperty("contextClassLoader", equalTo(target)));
    return negateMatcher ? not(matcher) : matcher;
  }

  protected boolean isClassFromLibrary(String className) {
    return className.startsWith(getPackagePrefix())
        || className.startsWith(this.getClass().getPackage().getName());
  }

  private void assertClassLoadersAreNotLeakedAfterDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                           Function<TestClassLoadersHierarchy, ClassLoader> executionClassLoaderProvider)
      throws Exception {
    TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
    List<URL> additionalLibraries = new ArrayList<>();
    additionalLibraries.add(this.libraryUrl);
    Optional.ofNullable(getLeakTriggererClass())
        .ifPresent(c -> additionalLibraries.add(c.getProtectionDomain().getCodeSource().getLocation()));
    builder = driverConfigurer.apply(builder, additionalLibraries.toArray(new URL[0]));
    try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
      withContextClassLoader(executionClassLoaderProvider.apply(classLoadersHierarchy), () -> {
        try {
          if (getLeakTriggererClass() != null) {
            Class<?> runnableClass = currentThread().getContextClassLoader().loadClass(getLeakTriggererClass().getName());
            Object runnable = runnableClass.newInstance();
            if (runnable instanceof Runnable) {
              ((Runnable) runnable).run();
            }
          }
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      });
      disposeAppAndAssertRelease(classLoadersHierarchy);
      disposeDomainAndAssertRelease(classLoadersHierarchy);
    }
  }

  private void assertThreadsNamesAfterAppDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                  Function<TestClassLoadersHierarchy, ClassLoader> executionClassLoaderProvider,
                                                  Boolean negateMatcher)
      throws Exception {
    List<Driver> driversAtBegining = Collections.list(DriverManager.getDrivers());
    TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
    List<URL> additionalLibraries = new ArrayList<>();
    additionalLibraries.add(this.libraryUrl);
    Optional.ofNullable(getLeakTriggererClass())
        .ifPresent(c -> additionalLibraries.add(c.getProtectionDomain().getCodeSource().getLocation()));
    builder = driverConfigurer.apply(builder, additionalLibraries.toArray(new URL[0]));
    try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
      ClassLoader target = executionClassLoaderProvider.apply(classLoadersHierarchy);
      withContextClassLoader(target, () -> {
        try {
          if (getLeakTriggererClass() != null) {
            Class<?> runnableClass = target.loadClass(getLeakTriggererClass().getName());
            Object runnable = runnableClass.newInstance();
            if (runnable instanceof Runnable) {
              ((Runnable) runnable).run();
            }
          }
          classLoadersHierarchy.disposeApp();
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
          LOGGER.error(e.getMessage(), e);
          Assert.fail(e.getMessage());
        }
      });
      Matcher<Iterable<? super Thread>> threadMatcher = hasDriverThreadMatcher(target, negateMatcher);
      assertThat(getCurrentThread(), threadMatcher);
    }
    assertThat(driversAtBegining, everyItem(isIn(Collections.list(DriverManager.getDrivers()))));
  }

  protected TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(getArtifactLifecycleListenerClass())
        .excludingClassNamesFromRoot(this::isClassFromLibrary);
  }

  protected static List<String> getCurrentThreadNames() {
    return getAllStackTraces().keySet().stream().map(Thread::getName).collect(toList());
  }

  protected static List<Thread> getCurrentThread() {
    return getAllStackTraces().keySet().stream().collect(toList());
  }

  protected void disposeAppAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy)
      throws IOException, InterruptedException {
    LOGGER.debug("disposeAppAndAssertRelease");
    CollectableReference<ClassLoader> appClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppClassLoader());
    CollectableReference<ClassLoader> appExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppExtensionClassLoader());
    classLoadersHierarchy.disposeApp();
    System.gc();
    Thread.sleep(1000);
    await().until(() -> appExtensionClassLoader, is(collectedByGc()));
    await().until(() -> appClassLoader, is(collectedByGc()));
  }

  protected void disposeDomainAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy)
      throws IOException, InterruptedException {
    LOGGER.debug("disposeDomainAndAssertRelease");
    CollectableReference<ClassLoader> domainClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainClassLoader());
    CollectableReference<ClassLoader> domainExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainExtensionClassLoader());
    classLoadersHierarchy.disposeDomain();
    System.gc();
    Thread.sleep(1000);
    await().until(() -> domainExtensionClassLoader, is(collectedByGc()));
    await().until(() -> domainClassLoader, is(collectedByGc()));
  }

}
