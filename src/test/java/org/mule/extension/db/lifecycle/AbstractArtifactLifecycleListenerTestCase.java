/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;


import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.copyOf;
import static org.mule.extension.db.util.CollectableReference.collectedByGc;
import static org.mule.extension.db.util.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.internal.lifecycle.DerbyArtifactLifecycleListener;
import org.mule.extension.db.util.CollectableReference;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Assume;
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

  protected Class getLeakTriggererClass() {
    return null;
  }

  abstract void assertThreadsAreDisposed();

  abstract void assertThreadsAreNotDisposed();

//  private URL[] appendUrlTo(URL[] urls, URL newUrl) {
//    urls = copyOf(urls, urls.length + 1);
//    urls[urls.length - 1] = newUrl;
//    return urls;
//  }
//  @Test
//  public void whenDriverIsInAppExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
//    Assume.assumeTrue(enableLibraryReleaseChecking());
//
//    List<URL> additionalLibraries = new ArrayList<>();
//    additionalLibraries.add(this.libraryUrl);
//
//    Optional.ofNullable(getLeakTriggererClass())
//            .ifPresent(c -> additionalLibraries.add(c.getProtectionDomain().getCodeSource().getLocation()));
//    Predicate<String> rootFilter = this::isClassFromLibrary;
//    rootFilter = rootFilter.negate();
//    rootFilter = rootFilter.and(name -> !DerbyArtifactLifecycleListener.class.getName().equals(name));
//    URL[] domainExtensionUrls = new URL[0];
//    URL[] appExtensionUrls = new URL[0];
//    URL[] appUrls = new URL[0];
//    URL[] domainUrls = new URL[0];
//    URL listenerClassUrl = DerbyArtifactLifecycleListener.class.getProtectionDomain().getCodeSource().getLocation();
//    appExtensionUrls = appendUrlTo(appExtensionUrls, listenerClassUrl);
//    domainExtensionUrls = appendUrlTo(domainExtensionUrls, listenerClassUrl);
//
////    ClassLoader domainClassLoader =
////            new URLClassLoader(additionalLibraries.toArray(new URL[0]), new TestClassLoadersHierarchy.FilteringClassLoader(getSystemClassLoader(), rootFilter));
//    ClassLoader appClassLoader = new URLClassLoader(appUrls);
//    ClassLoader appExtensionClassLoader = new URLClassLoader(appExtensionUrls, appClassLoader);
//    ArtifactLifecycleListener listener = DerbyArtifactLifecycleListener.class.getConstructor().newInstance();
////      withContextClassLoader(appExtensionClassLoader, () -> {
//        try {
//          if (getLeakTriggererClass() != null) {
//            Class<?> runnableClass = appExtensionClassLoader.loadClass(getLeakTriggererClass().getName());
//            Object runnable = runnableClass.newInstance();
//            ((Runnable)runnable).run();
//          }
//        } catch (Exception e) {
//          LOGGER.error(e.getMessage(), e);
//        }
////      });
//      listener.onArtifactDisposal(new TestArtifactDisposalContext(appClassLoader,appExtensionClassLoader ));
//      CollectableReference<ClassLoader> CollctableAppClassLoader =
//            new CollectableReference<>(appClassLoader);
//       CollectableReference<ClassLoader> CollectableAppExtensionClassLoader =
//            new CollectableReference<>(appExtensionClassLoader);
//      ((Closeable) appExtensionClassLoader).close();
//      appExtensionClassLoader=null;
//      ((Closeable) appClassLoader).close();
//      appClassLoader = null;
//      Runtime.getRuntime().gc();
//      await().until(() -> CollectableAppExtensionClassLoader, is(collectedByGc()));
//      await().until(() -> CollctableAppClassLoader, is(collectedByGc()));
//
//  }

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
                                       not(hasReadDriverThread()));
  }

  @Test
  public void whenDriverIsInAppExtensionThenThreadsAreNotLeakedAfterDisposal() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                       TestClassLoadersHierarchy::getAppExtensionClassLoader,
                                       not(hasReadDriverThread()));
  }

  @Test
  public void whenDriverIsInDomainThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       hasReadDriverThread());
  }

  @Test
  public void whenDriverIsInDomainExtensionThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    Assume.assumeTrue(enableThreadsReleaseChecking());
    assertThreadsNamesAfterAppDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                       TestClassLoadersHierarchy::getDomainExtensionClassLoader,
                                       hasReadDriverThread());
  }

  protected Matcher<Iterable<? super String>> hasReadDriverThread() {
    return hasItem(getDriverThreadName());
  }

  protected boolean isClassFromLibrary(String className) {
    return className.startsWith(getPackagePrefix())
        || className.startsWith(this.getClass().getPackage().getName());
  }

  protected static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
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
                                                  Matcher<Iterable<? super String>> threadNamesMatcher)
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
          classLoadersHierarchy.disposeApp();
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
          LOGGER.error(e.getMessage(), e);
          Assert.fail(e.getMessage());
        }
      });
      assertThat(getCurrentThreadNames(), threadNamesMatcher);
    }
  }

  protected TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(getArtifactLifecycleListenerClass())
        .excludingClassNamesFromRoot(this::isClassFromLibrary);
  }

  protected static List<String> getCurrentThreadNames() {
    return getAllStackTraces().keySet().stream().map(Thread::getName).collect(toList());
  }

  protected void disposeAppAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy)
      throws IOException, InterruptedException {
    CollectableReference<ClassLoader> appClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppClassLoader());
    CollectableReference<ClassLoader> appExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getAppExtensionClassLoader());
    classLoadersHierarchy.disposeApp();
    await().until(() -> appExtensionClassLoader, is(collectedByGc()));
    await().until(() -> appClassLoader, is(collectedByGc()));
  }

  protected void disposeDomainAndAssertRelease(TestClassLoadersHierarchy classLoadersHierarchy) throws IOException {
    CollectableReference<ClassLoader> domainClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainClassLoader());
    CollectableReference<ClassLoader> domainExtensionClassLoader =
        new CollectableReference<>(classLoadersHierarchy.getDomainExtensionClassLoader());
    classLoadersHierarchy.disposeDomain();
    await().until(() -> domainExtensionClassLoader, is(collectedByGc()));
    await().until(() -> domainClassLoader, is(collectedByGc()));
  }


  /* */
  private List<Driver> driversRegistered;

  //  @Before
  //  public void unregisterDrivers() {
  //    driversRegistered = Collections.list(DriverManager.getDrivers());
  //    try {
  //      getArtifactLifecycleListenerClass().newInstance().onArtifactDisposal(artifactDisposal);
  //    } catch (InstantiationException e) {
  //      throw new RuntimeException(e);
  //    } catch (IllegalAccessException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }
  //
  //  @After
  //  public void unregisterDriversAgain() {
  //    driversRegistered.forEach(driver -> {
  //      try {
  //        DriverManager.registerDriver(driver);
  //      } catch (SQLException e) {
  //        throw new RuntimeException(e);
  //      }
  //    });
  //  System.out.println("");
  //  }

}
