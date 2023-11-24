/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.lifecycle;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.stream.Stream;

/**
 * An implementation of {@link ArtifactDisposalContext} used for unit-testing {@link ArtifactLifecycleListener} implementations.
 */
public class TestArtifactDisposalContext implements ArtifactDisposalContext {

  private final ClassLoader artifactClassLoader;
  private final ClassLoader extensionClassLoader;

  public TestArtifactDisposalContext(ClassLoader artifactClassLoader, ClassLoader extensionClassLoader) {
    this.artifactClassLoader = artifactClassLoader;
    this.extensionClassLoader = extensionClassLoader;
  }

  @Override
  public ClassLoader getExtensionClassLoader() {
    return extensionClassLoader;
  }

  @Override
  public ClassLoader getArtifactClassLoader() {
    return artifactClassLoader;
  }

  @Override
  public boolean isExtensionOwnedClassLoader(ClassLoader classLoader) {
    return classLoader == extensionClassLoader;
  }

  @Override
  public boolean isArtifactOwnedClassLoader(ClassLoader classLoader) {
    return classLoader == artifactClassLoader;
  }

  @Override
  public Stream<Thread> getExtensionOwnedThreads() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<Thread> getArtifactOwnedThreads() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isArtifactOwnedThread(Thread thread) {
    return isArtifactOwnedClassLoader(thread.getContextClassLoader());
  }

  @Override
  public boolean isExtensionOwnedThread(Thread thread) {
    return isExtensionOwnedClassLoader(thread.getContextClassLoader());
  }
}
