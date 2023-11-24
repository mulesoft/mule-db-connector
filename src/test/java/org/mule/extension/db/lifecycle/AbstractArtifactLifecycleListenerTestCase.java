/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;


import static org.mule.extension.db.util.CollectableReference.collectedByGc;
import static org.mule.extension.db.util.DependencyResolver.getDependencyFromMaven;
import static org.mule.extension.db.util.Eventually.eventually;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.extension.db.util.CollectableReference;

import java.io.IOException;
import java.net.URL;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;
import org.slf4j.Logger;

public abstract class AbstractArtifactLifecycleListenerTestCase {

  protected final Logger LOGGER = getLogger(this.getClass());
  protected String groupId;
  protected String artifactId;
  protected String version;
  protected URL libraryUrl;

  // Parameterized
  protected AbstractArtifactLifecycleListenerTestCase(String groupId, String artifactId, String version) {
    LOGGER.info("Parameters: {0}, {1}, {2}", groupId, artifactId, version);
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.libraryUrl = getDependencyFromMaven(this.groupId,
                                             this.artifactId,
                                             this.version);
  }

  abstract void generateTargetLeak(ClassLoader classLoader);

  abstract String getPackagePrefix();

  abstract Boolean shouldCheckLibraryRelease();

  abstract Boolean shouldCheckThreadsRelease();

  @Test
  public void whenLibraryIsInAppThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInApp,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInAppExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInDomainThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInDomainExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenLibraryIsInDomainThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    if (shouldCheckThreadsRelease()) {
      try (TestClassLoadersHierarchy classLoadersHierarchy = getBaseClassLoaderHierarchyBuilder()
          .withUrlsInDomain(new URL[] {this.libraryUrl})
          .build()) {
        classLoadersHierarchy.disposeApp();
        // When the app is disposed the thread is still active because it belongs to the domain
        assertThreadsAreNotDisposed();
      }
    }
  }

  abstract void assertThreadsAreNotDisposed();

  @Test
  public void whenLibraryIsInAppThenThreadsAreDisposedWhenAppIsDisposed() throws Exception {
    if (shouldCheckThreadsRelease()) {
      try (TestClassLoadersHierarchy classLoadersHierarchy = getBaseClassLoaderHierarchyBuilder()
          .withUrlsInDomain(new URL[] {this.libraryUrl})
          .build()) {
        classLoadersHierarchy.disposeApp();
        assertThreadsAreDisposed();
      }
    }
  }

  abstract void assertThreadsAreDisposed();

  private TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(new MySqlArtifactLifecycleListener())
        .excludingClassNamesFromRoot(this::isClassFromLibrary);
  }

  private void assertClassLoadersAreNotLeakedAfterDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                           Function<TestClassLoadersHierarchy, ClassLoader> connectionClassLoaderProvider)
      throws Exception {
    if (shouldCheckLibraryRelease()) {
      TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
      builder = driverConfigurer.apply(builder, new URL[] {this.libraryUrl});

      try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
        generateTargetLeak(connectionClassLoaderProvider.apply(classLoadersHierarchy));
        disposeAppAndAssertRelease(classLoadersHierarchy);
        disposeDomainAndAssertRelease(classLoadersHierarchy);
      }
    }
  }

  private void disposeAppAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy) throws IOException {
    CollectableReference<ClassLoader> appClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppClassLoader());
    CollectableReference<ClassLoader> extensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppExtensionClassLoader());
    classLoadersHierarchy.disposeApp();
    assertThat(extensionClassLoader, is(eventually(collectedByGc())));
    assertThat(appClassLoader, is(eventually(collectedByGc())));
  }

  private void disposeDomainAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy) throws IOException {
    CollectableReference<ClassLoader> domainClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainClassLoader());
    CollectableReference<ClassLoader> domainExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainExtensionClassLoader());
    classLoadersHierarchy.disposeDomain();
    assertThat(domainExtensionClassLoader, is(eventually(collectedByGc())));
    assertThat(domainClassLoader, is(eventually(collectedByGc())));
  }

  private boolean isClassFromLibrary(String className) {
    return className.startsWith(getPackagePrefix());
  }
}
