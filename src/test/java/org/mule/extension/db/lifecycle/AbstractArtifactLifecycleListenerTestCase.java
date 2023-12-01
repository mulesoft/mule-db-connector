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

import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.util.CollectableReference;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.IOException;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.sun.org.apache.bcel.internal.generic.NEW;
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

  abstract String getDriverName();

  abstract String getDriverThreadName();

  protected Boolean enableLibraryReleaseChecking() {
    return true;
  }

  protected Boolean enableThreadsReleaseChecking() {
    return true;
  }

  protected String getRunnableClass() {
    return null;
  }

  abstract void assertThreadsAreDisposed();

  abstract void assertThreadsAreNotDisposed();

  /* ClassLoader Tests */
  @Test
  public void whenLibraryIsInAppThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInApp,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInAppExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInDomainThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableLibraryReleaseChecking());
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInDomainExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
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
                                       not(hasReadCheckTimer()));
  }

  @Test
  public void whenDriverIsInAppExtensionThenThreadsAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                       TestClassLoadersHierarchy::getAppExtensionClassLoader,
                                       not(hasReadCheckTimer()));
  }

  @Test
  public void whenDriverIsInDomainThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       hasReadCheckTimer());
  }

  @Test
  public void whenDriverIsInDomainExtensionThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       hasReadCheckTimer());
  }

  private Matcher<Iterable<? super String>> hasReadCheckTimer() {
    return hasItem(getDriverThreadName());
  }

  private boolean isClassFromLibrary(String className) {
    return className.startsWith(getPackagePrefix());
  }

  private static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  private void assertClassLoadersAreNotLeakedAfterDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                           Function<TestClassLoadersHierarchy, ClassLoader> executionClassLoaderProvider)
      throws Exception {
    TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
    builder = driverConfigurer.apply(builder, new URL[] {this.libraryUrl});
    try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
      ClassLoader target = executionClassLoaderProvider.apply(classLoadersHierarchy);
      withContextClassLoader(target, () -> {
        try {
          try {
            Class<?> driverClass = target.loadClass(getDriverName());
            driverClass.newInstance();
            if (getRunnableClass() != null) {
              Class<?> runnableClass = target.loadClass(getRunnableClass());
              Object runnable = runnableClass.newInstance();
              if (runnable instanceof Runnable) {
                ((Runnable) runnable).run();
              }
            }
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          }
          disposeAppAndAssertRelease(classLoadersHierarchy);
          disposeDomainAndAssertRelease(classLoadersHierarchy);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private void assertThreadsNamesAfterAppDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                  Function<TestClassLoadersHierarchy, ClassLoader> executionClassLoaderProvider,
                                                  Matcher<Iterable<? super String>> threadNamesMatcher)
      throws Exception {
    TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
    builder = driverConfigurer.apply(builder, new URL[] {this.libraryUrl});
    try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
      ClassLoader target = executionClassLoaderProvider.apply(classLoadersHierarchy);
      withContextClassLoader(target, () -> {
        try {
          if (getRunnableClass() != null) {
            Class<?> runnableClass = target.loadClass(getRunnableClass());
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
        assertThat(getCurrentThreadNames(), threadNamesMatcher);
      });
    }
  }

  private TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(getArtifactLifecycleListenerClass())
        .excludingClassNamesFromRoot(this::isClassFromLibrary);
  }

  private static List<String> getCurrentThreadNames() {
    return getAllStackTraces().keySet().stream().map(Thread::getName).collect(toList());
  }

  private void disposeAppAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy) throws IOException {
    CollectableReference<ClassLoader> appClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppClassLoader());
    CollectableReference<ClassLoader> extensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppExtensionClassLoader());
    classLoadersHierarchy.disposeApp();
    await().until(() -> extensionClassLoader, is(collectedByGc()));
    await().until(() -> appClassLoader, is(collectedByGc()));
  }

  private void disposeDomainAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy) throws IOException {
    CollectableReference<ClassLoader> domainClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainClassLoader());
    CollectableReference<ClassLoader> domainExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainExtensionClassLoader());
    classLoadersHierarchy.disposeDomain();
    await().until(() -> domainExtensionClassLoader, is(collectedByGc()));
    await().until(() -> domainClassLoader, is(collectedByGc()));
  }

  private List<Driver> driversRegistered;

  @Before
  public void unregisterDrivers() {
    driversRegistered = Collections.list(DriverManager.getDrivers());
    Collections.list(DriverManager.getDrivers()).forEach(driver -> {
      try {
        DriverManager.deregisterDriver(driver);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @After
  public void unregisterDriversAgain() {
    driversRegistered.forEach(driver -> {
      try {
        DriverManager.registerDriver(driver);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
