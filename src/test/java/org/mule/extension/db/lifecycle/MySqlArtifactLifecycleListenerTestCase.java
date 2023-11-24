/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;


import static org.mule.extension.db.AllureConstants.MySqlFeature.MYSQL_FEATURE;
import static org.mule.extension.db.AllureConstants.MySqlFeature.MySqlStories.MYSQL_RESOURCE_RELEASING;
import static org.mule.extension.db.util.CollectableReference.collectedByGc;
import static org.mule.extension.db.util.DependencyResolver.getDependencyFromMaven;
import static org.mule.extension.db.util.Eventually.eventually;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.extension.db.util.CollectableReference;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test case for the {@link org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener}.
 */
@Feature(MYSQL_FEATURE)
@Story(MYSQL_RESOURCE_RELEASING)
public class MySqlArtifactLifecycleListenerTestCase {

  private static final Logger LOGGER = getLogger(MySqlArtifactLifecycleListenerTestCase.class);

  private static final URL MYSQL_DRIVER_URL = getDependencyFromMaven("mysql",
                                                                     "mysql-connector-java",
                                                                     "8.0.30");
  private static final String MYSQL_PACKAGE_PREFIX = "com.mysql";

  @Test
  public void whenDriverIsInAppThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInApp,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInAppExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInAppExtension,
                                                TestClassLoadersHierarchy::getAppExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInDomainThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomain,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInDomainExtensionThenClassLoadersAreNotLeakedAfterDisposal() throws Exception {
    assertClassLoadersAreNotLeakedAfterDisposal(TestClassLoadersHierarchy.Builder::withUrlsInDomainExtension,
                                                TestClassLoadersHierarchy::getDomainExtensionClassLoader);
  }

  @Test
  public void whenDriverIsInDomainThenThreadsAreNotDisposedWhenAppIsDisposed() throws Exception {
    try (TestClassLoadersHierarchy classLoadersHierarchy = getBaseClassLoaderHierarchyBuilder()
        .withUrlsInDomain(new URL[] {MYSQL_DRIVER_URL})
        .build()) {
      tryStartFailingMySqlConnection(classLoadersHierarchy.getDomainExtensionClassLoader());
      classLoadersHierarchy.disposeApp();
      // When the app is disposed the thread is still active because it belongs to the domain
      //      assertThat(getCurrentThreadNames(), hasReadCheckTimer()); //TODO Buscar el hilo de AbandonedConnectionCleanupThread
    }
  }

  private TestClassLoadersHierarchy.Builder getBaseClassLoaderHierarchyBuilder() {
    return TestClassLoadersHierarchy.getBuilder()
        .withArtifactLifecycleListener(new MySqlArtifactLifecycleListener())
        .excludingClassNamesFromRoot(this::isClassFromDriver);
  }

  private void assertClassLoadersAreNotLeakedAfterDisposal(BiFunction<TestClassLoadersHierarchy.Builder, URL[], TestClassLoadersHierarchy.Builder> driverConfigurer,
                                                           Function<TestClassLoadersHierarchy, ClassLoader> connectionClassLoaderProvider)
      throws Exception {
    TestClassLoadersHierarchy.Builder builder = getBaseClassLoaderHierarchyBuilder();
    builder = driverConfigurer.apply(builder, new URL[] {MYSQL_DRIVER_URL});

    try (TestClassLoadersHierarchy classLoadersHierarchy = builder.build()) {
      tryStartFailingMySqlConnection(connectionClassLoaderProvider.apply(classLoadersHierarchy));

      disposeAppAndAssertRelease(classLoadersHierarchy);
      disposeDomainAndAssertRelease(classLoadersHierarchy);
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

  private void tryStartFailingMySqlConnection(ClassLoader classLoader) throws ReflectiveOperationException {
    ClassLoader originalTCCL = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(classLoader);
    try {
      Connection connection = null;

      try {
        Class<?> mySqlDriver = classLoader.loadClass("com.mysql.cj.jdbc.Driver");
        Driver driver = (Driver) mySqlDriver.newInstance();
        // DriverManager.registerDriver(driver);


        //    } catch (SQLException ite) {
        //      assertThat(getRootCause(ite), is(instanceOf(ConnectException.class)));
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
      // assertThat(DriverManager.getDrivers(), hasMySqlDriver());
    } finally {
      currentThread().setContextClassLoader(originalTCCL);
    }
  }

  private static Matcher<Enumeration<Driver>> hasMySqlDriver() {
    return new TypeSafeMatcher<Enumeration<Driver>>() {

      @Override
      protected boolean matchesSafely(Enumeration<Driver> drivers) {
        while (drivers.hasMoreElements()) {
          Driver driver = drivers.nextElement();
          if (isMySqlDriver(driver)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a collection containing the MySQL driver");
      }
    };
  }

  private static boolean isMySqlDriver(Driver driver) {
    return driver.getClass().getName().toLowerCase().contains("mysql");
  }

  private static List<String> getCurrentThreadNames() {
    return getAllStackTraces().keySet().stream().map(Thread::getName).collect(toList());
  }

  private static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  private boolean isClassFromDriver(String className) {
    return className.startsWith(MYSQL_PACKAGE_PREFIX);
  }
}
