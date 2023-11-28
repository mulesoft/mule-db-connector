/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.copyOf;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Predicate;

/**
 * Helper class for modeling a {@link ClassLoader} hierarchy that is similar to the one typically used by the Mule Runtime.
 * <ul>
 *   <li>domain</li>
 *   <ul>
 *     <li>domain-extension</li>
 *     <li>application</li>
 *     <ul>
 *       <li>application-extension</li>
 *     </ul>
 *   </ul>
 * </ul>
 * The root {@link ClassLoader} (the parent of the domain) is always the system {@link ClassLoader}.
 */
public class TestClassLoadersHierarchy implements AutoCloseable {

  /**
   * Fluent API for building a {@link TestClassLoadersHierarchy}.
   */
  public static class Builder {

    private Predicate<String> rootClassNameFilter;
    private URL[] domainUrls = new URL[0];
    private URL[] domainExtensionUrls = new URL[0];
    private URL[] appUrls = new URL[0];
    private URL[] appExtensionUrls = new URL[0];
    private Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass;

    /**
     * Allows for excluding classes by name from the root {@link ClassLoader}.
     * <p>
     * This is specially useful when the root classloader already contains a class that we want to be loaded from one of the child
     * classloaders in the hierarchy. Otherwise, it will be loaded from the root following a parent-first strategy.
     * @param rootClassNameExclusionFilter Classes matching this filter will not be loaded by the root {@link ClassLoader}, even
     *                                     if it has them in its class path.
     * @return This instance, for chaining purposes.
     */
    public Builder excludingClassNamesFromRoot(Predicate<String> rootClassNameExclusionFilter) {
      this.rootClassNameFilter = rootClassNameExclusionFilter.negate();
      return this;
    }

    /**
     * Adds the given {@link URL}s to the domain {@link ClassLoader}.
     * @param urls The {@link URL}s.
     * @return This instance, for chaining purposes.
     */
    public Builder withUrlsInDomain(URL[] urls) {
      domainUrls = urls;
      return this;
    }

    /**
     * Adds the given {@link URL}s to the {@link ClassLoader} of an extension in the domain.
     * @param urls The {@link URL}s.
     * @return This instance, for chaining purposes.
     */
    public Builder withUrlsInDomainExtension(URL[] urls) {
      domainExtensionUrls = urls;
      return this;
    }

    /**
     * Adds the given {@link URL}s to the application {@link ClassLoader}.
     * @param urls The {@link URL}s.
     * @return This instance, for chaining purposes.
     */
    public Builder withUrlsInApp(URL[] urls) {
      appUrls = urls;
      return this;
    }

    /**
     * Adds the given {@link URL}s to the {@link ClassLoader} of an extension in the application.
     * @param urls The {@link URL}s.
     * @return This instance, for chaining purposes.
     */
    public Builder withUrlsInAppExtension(URL[] urls) {
      appExtensionUrls = urls;
      return this;
    }

    /**
     * Sets up an {@link ArtifactLifecycleListener} to be triggered when disposing the {@link ClassLoader}s conforming this
     * hierarchy.
     * @param artifactLifecycleListener The {@link ArtifactLifecycleListener} to call whenever the disposal of an artifact is
     *                                  simulated.
     * @return This instance, for chaining purposes.
     */
    public Builder withArtifactLifecycleListener(Class<? extends ArtifactLifecycleListener> artifactLifecycleListener) {
      this.artifactLifecycleListenerClass = artifactLifecycleListener;
      return this;
    }

    /**
     * @return The {@link TestClassLoadersHierarchy}.
     */
    public TestClassLoadersHierarchy build() {
      if (artifactLifecycleListenerClass != null) {
        // Adjust the root class filter so that the listener class is also excluded from the root ClassLoader
        rootClassNameFilter = rootClassNameFilter.and(name -> !artifactLifecycleListenerClass.getName().equals(name));

        // Adds the URL with the location of the listener class at the classpath to the extension ClassLoaders, so it can be
        // found there (after having failed at the root ClassLoader)
        URL listenerClassUrl = artifactLifecycleListenerClass.getProtectionDomain().getCodeSource().getLocation();
        appExtensionUrls = appendUrlTo(appExtensionUrls, listenerClassUrl);
        domainExtensionUrls = appendUrlTo(domainExtensionUrls, listenerClassUrl);
      }
      ClassLoader domainClassLoader =
          new URLClassLoader(domainUrls, new FilteringClassLoader(getSystemClassLoader(), rootClassNameFilter));
      ClassLoader domainExtensionClassLoader = new URLClassLoader(domainExtensionUrls, domainClassLoader);
      ClassLoader appClassLoader = new URLClassLoader(appUrls, domainClassLoader);
      ClassLoader appExtensionClassLoader = new URLClassLoader(appExtensionUrls, appClassLoader);
      return new TestClassLoadersHierarchy(domainClassLoader, domainExtensionClassLoader, appClassLoader, appExtensionClassLoader,
                                           artifactLifecycleListenerClass);
    }

    private URL[] appendUrlTo(URL[] urls, URL newUrl) {
      urls = copyOf(urls, urls.length + 1);
      urls[urls.length - 1] = newUrl;
      return urls;
    }
  }

  private static class FilteringClassLoader extends ClassLoader {

    private final Predicate<String> classNameFilter;

    private FilteringClassLoader(ClassLoader parent, Predicate<String> classNameFilter) {
      super(parent);
      this.classNameFilter = classNameFilter;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (classNameFilter.test(name)) {
        return super.loadClass(name, resolve);
      } else {
        throw new ClassNotFoundException(name);
      }
    }
  }

  public static Builder getBuilder() {
    return new Builder();
  }

  private ClassLoader domainClassLoader;
  private ClassLoader domainExtensionClassLoader;
  private ClassLoader appClassLoader;
  private ClassLoader appExtensionClassLoader;
  private ArtifactLifecycleListener appExtensionArtifactLifecycleListener;
  private ArtifactLifecycleListener domainExtensionArtifactLifecycleListener;

  private TestClassLoadersHierarchy(ClassLoader domainClassLoader,
                                    ClassLoader domainExtensionClassLoader,
                                    ClassLoader appClassLoader,
                                    ClassLoader appExtensionClassLoader,
                                    Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass) {
    this.domainClassLoader = domainClassLoader;
    this.domainExtensionClassLoader = domainExtensionClassLoader;
    this.appClassLoader = appClassLoader;
    this.appExtensionClassLoader = appExtensionClassLoader;
    if (artifactLifecycleListenerClass != null) {
      createArtifactLifecycleListeners(artifactLifecycleListenerClass);
    }
  }

  /**
   * @return The {@link ClassLoader} of the domain.
   */
  public ClassLoader getDomainClassLoader() {
    return domainClassLoader;
  }

  /**
   * @return The {@link ClassLoader} of an extension in the domain.
   */
  public ClassLoader getDomainExtensionClassLoader() {
    return domainExtensionClassLoader;
  }

  /**
   * @return The {@link ClassLoader} of the application.
   */
  public ClassLoader getAppClassLoader() {
    return appClassLoader;
  }

  /**
   * @return The {@link ClassLoader} of an extension in the application.
   */
  public ClassLoader getAppExtensionClassLoader() {
    return appExtensionClassLoader;
  }

  /**
   * Simulates the disposal of the application (and its extensions).
   * <p>
   * If an {@link ArtifactLifecycleListener} is configured, it will be notified with a proper {@link ArtifactDisposalContext}.
   * @throws IOException If an I/O error occurs when closing the underlying {@link ClassLoader}s.
   */
  public void disposeApp() throws IOException {
    if (appClassLoader != null && appExtensionClassLoader != null) {
      if (appExtensionArtifactLifecycleListener != null) {
        appExtensionArtifactLifecycleListener.onArtifactDisposal(getDisposalContextForApp());
      }
      ((Closeable) appExtensionClassLoader).close();
      ((Closeable) appClassLoader).close();
      appExtensionClassLoader = null;
      appClassLoader = null;
      appExtensionArtifactLifecycleListener = null;
    }
  }

  /**
   * Simulates the disposal of the domain (and its extensions).
   * <p>
   * If an {@link ArtifactLifecycleListener} is configured, it will be notified with a proper {@link ArtifactDisposalContext}.
   * @throws IOException If an I/O error occurs when closing the underlying {@link ClassLoader}s.
   */
  public void disposeDomain() throws IOException {
    if (domainClassLoader != null && domainExtensionClassLoader != null) {
      if (domainExtensionArtifactLifecycleListener != null) {
        domainExtensionArtifactLifecycleListener.onArtifactDisposal(getDisposalContextForDomain());
      }
      ((Closeable) domainExtensionClassLoader).close();
      ((Closeable) domainClassLoader).close();
      domainExtensionClassLoader = null;
      domainClassLoader = null;
      domainExtensionArtifactLifecycleListener = null;
    }
  }

  @Override
  public void close() throws Exception {
    disposeApp();
    disposeDomain();
  }

  private ArtifactDisposalContext getDisposalContextForApp() {
    return new TestArtifactDisposalContext(getAppClassLoader(), getAppExtensionClassLoader());
  }

  private ArtifactDisposalContext getDisposalContextForDomain() {
    return new TestArtifactDisposalContext(getDomainClassLoader(), getDomainExtensionClassLoader());
  }

  private void createArtifactLifecycleListeners(Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass) {
    // Instantiates the listeners using the extension ClassLoaders to load the indicated class
    try {
      this.appExtensionArtifactLifecycleListener =
          createArtifactLifecycleInstance(loadFromClassLoader(artifactLifecycleListenerClass, appExtensionClassLoader));
      this.domainExtensionArtifactLifecycleListener =
          createArtifactLifecycleInstance(loadFromClassLoader(artifactLifecycleListenerClass, domainExtensionClassLoader));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Unable to create ArtifactLifecycleListener instance", e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> loadFromClassLoader(Class<T> originalClass, ClassLoader targetClassLoader) throws ClassNotFoundException {
    return (Class<T>) targetClassLoader.loadClass(originalClass.getName());
  }

  private ArtifactLifecycleListener createArtifactLifecycleInstance(Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    return artifactLifecycleListenerClass.getConstructor().newInstance();
  }
}
