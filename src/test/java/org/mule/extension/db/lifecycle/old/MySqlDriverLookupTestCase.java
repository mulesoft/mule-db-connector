/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle.old;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;

import static java.lang.System.gc;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.internal.lifecycle.MySqlArtifactLifecycleListener;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;

/**
 * MySql's resource cleaner test. The mysql-driver-v5/8.jar files where created to mock the cleanup thread package in both
 * versions.
 */
@RunWith(Parameterized.class)
public class MySqlDriverLookupTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 150;
  private static final int PROBER_POLLING_TIMEOUT = 6000;

  private static final String ARTIFACT_ID = "mysql-connector-java";
  private static final String GROUP_ID = "mysql";
  private String artifactVersion;

  private ClassLoaderLookupPolicy testLookupPolicy;
  private MuleArtifactClassLoader artifactClassLoader = null;
  private final MySqlArtifactLifecycleListener listener;
  private ArtifactDisposalContext artifactDisposalContext;

  private static final Logger LOGGER = getLogger(MySqlDriverLookupTestCase.class);

  // Parameterized
  public MySqlDriverLookupTestCase(String version) {
    artifactVersion = version;
    testLookupPolicy = new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return CHILD_FIRST;
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        return null;
      }

      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
        return null;
      }
    };
    listener = new MySqlArtifactLifecycleListener();
  }

  @Parameterized.Parameters(name = "Testing artifact {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "5.1.49",
        "6.0.6",
        "8.0.28"
    };
  }

  @Before
  public void setup() {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), is(false));

    artifactClassLoader =
        new MuleArtifactClassLoader("JdbcArtifactLifecycleListenerTest",
                                    mock(ArtifactDescriptor.class),
                                    new URL[] {getDependencyFromMaven(GROUP_ID, ARTIFACT_ID, artifactVersion)},
                                    currentThread().getContextClassLoader(),
                                    testLookupPolicy);
    artifactDisposalContext = new ArtifactDisposalContext() {

      @Override
      public ClassLoader getExtensionClassLoader() {
        return currentThread().getContextClassLoader();
      }

      @Override
      public ClassLoader getArtifactClassLoader() {
        return artifactClassLoader;
      }

      @Override
      public boolean isExtensionOwnedClassLoader(ClassLoader classLoader) {
        return false;
      }

      @Override
      public boolean isArtifactOwnedClassLoader(ClassLoader classLoader) {
        return false;
      }

      @Override
      public Stream<Thread> getExtensionOwnedThreads() {
        return null;
      }

      @Override
      public Stream<Thread> getArtifactOwnedThreads() {
        return null;
      }

      @Override
      public boolean isArtifactOwnedThread(Thread thread) {
        return false;
      }

      @Override
      public boolean isExtensionOwnedThread(Thread thread) {
        return false;
      }
    };
  }

  private URL getDependencyFromMaven(String groupId, String artifactId, String version) {
    URL settingsUrl = getClass().getClassLoader().getResource("custom-settings.xml");
    final MavenClientProvider mavenClientProvider = discoverProvider(this.getClass().getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build());

    try {
      BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder().setGroupId(groupId)
          .setArtifactId(artifactId).setVersion(version).build();

      BundleDependency dependency = mavenClient.resolveBundleDescriptor(bundleDescriptor);

      return dependency.getBundleUri().toURL();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void runMySqlDriverAndDispose() {
    String foundClassname = null;
    for (String knownCleanupThreadClassAddress : MySqlArtifactLifecycleListener.CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES) {
      try {
        Class<?> clazz = artifactDisposalContext.getArtifactClassLoader().loadClass(knownCleanupThreadClassAddress);
        foundClassname = clazz.getName();
      } catch (ClassNotFoundException e) {
        LOGGER.warn("No AbandonedConnectionCleanupThread registered with class address {}", knownCleanupThreadClassAddress);
      }
    }
    assertTrue(foundClassname.contains("AbandonedConnectionCleanupThread"));
    // Should I call it ?
    //artifactClassLoader.dispose();
    listener.onArtifactDisposal(artifactDisposalContext);
    // Call it twice to make sure all proper validations are in place, since the runtime may call repeated logic because
    // the original resource releaser is still in place and will be called at the same time if Java version is lower
    // than 17
    listener.onArtifactDisposal(artifactDisposalContext);
    assertClassLoaderIsEnqueued();
  }

  private void assertClassLoaderIsEnqueued() {
    PhantomReference<ClassLoader> artifactClassLoaderRef = new PhantomReference<>(artifactClassLoader, new ReferenceQueue<>());
    Assert.assertFalse(artifactClassLoaderRef.isEnqueued());
    artifactClassLoader = null;
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      gc();
      Assert.assertTrue(artifactClassLoaderRef.isEnqueued());
      return true;
    }));
  }
}
