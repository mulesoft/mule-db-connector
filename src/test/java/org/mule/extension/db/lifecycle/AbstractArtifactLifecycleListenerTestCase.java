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
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.util.CollectableReference;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

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

  abstract void generateTargetLeak(ClassLoader classLoader);

  abstract String getPackagePrefix();

  abstract Boolean enableLibraryReleaseChecking();

  abstract Boolean enableThreadsReleaseChecking();

  abstract void assertThreadsAreDisposed();

  abstract void assertThreadsAreNotDisposed();


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
    if (enableThreadsReleaseChecking()) {
      try (TestClassLoadersHierarchy classLoadersHierarchy = getBaseClassLoaderHierarchyBuilder()
          .withUrlsInDomain(new URL[] {this.libraryUrl})
          .build()) {
        classLoadersHierarchy.disposeApp();
        // When the app is disposed the thread is still active because it belongs to the domain
        assertThreadsAreNotDisposed();
      }
    }
  }

  @Test
  public void whenLibraryIsInAppThenThreadsAreDisposedWhenAppIsDisposed() throws Exception {
    if (enableThreadsReleaseChecking()) {
      try (TestClassLoadersHierarchy classLoadersHierarchy = getBaseClassLoaderHierarchyBuilder()
          .withUrlsInDomain(new URL[] {this.libraryUrl})
          .build()) {
        classLoadersHierarchy.disposeApp();
        assertThreadsAreDisposed();
      }
    }
  }

  private TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(getArtifactLifecycleListenerClass())
        .excludingClassNamesFromRoot(this::isClassFromLibrary);
  }

  private void assertClassLoadersAreNotLeakedAfterDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                           Function<TestClassLoadersHierarchy, ClassLoader> executionClassLoaderProvider)
      throws Exception {
    ClassLoader originalTCCL = currentThread().getContextClassLoader();
    if (enableLibraryReleaseChecking()) {
      TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
      builder = driverConfigurer.apply(builder, new URL[] {this.libraryUrl});

      try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
        ClassLoader target = executionClassLoaderProvider.apply(classLoadersHierarchy);
        currentThread().setContextClassLoader(target);
        withContextClassLoader(target, () -> {
          try {
            generateTargetLeak(target);
            disposeAppAndAssertRelease(classLoadersHierarchy);
            disposeDomainAndAssertRelease(classLoadersHierarchy);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      } finally {
        currentThread().setContextClassLoader(originalTCCL);
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
