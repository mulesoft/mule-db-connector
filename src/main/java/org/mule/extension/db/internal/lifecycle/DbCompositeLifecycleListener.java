/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.lifecycle;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.ArrayList;
import java.util.List;

public class DbCompositeLifecycleListener implements ArtifactLifecycleListener {

  private final List<ArtifactLifecycleListener> delegates = new ArrayList<>();


  public DbCompositeLifecycleListener() {
    delegates.add(new DB2ArtifactLifecycleListener());
    delegates.add(new DerbyArtifactLifecycleListener());
    delegates.add(new MySqlArtifactLifecycleListener());
    delegates.add(new OracleArtifactLifecycleListener());
  }

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    delegates.forEach(x -> x.onArtifactDisposal(artifactDisposalContext));
  }
}
