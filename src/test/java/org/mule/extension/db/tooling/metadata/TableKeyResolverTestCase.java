/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.tooling.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.DbArtifactClassLoaderRunnerConfig;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class TableKeyResolverTestCase extends AbstractDbIntegrationTestCase implements DbArtifactClassLoaderRunnerConfig {

  @Inject
  @Named(METADATA_SERVICE_KEY)
  private MetadataService metadataService;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[]{"integration/metadata/polling-source-key-resolver.xml"};
  }

  @Test
  public void retrieveAvailableTables() {
    MetadataResult<MetadataKeysContainer> listener =
        metadataService.getMetadataKeys(Location.builder().globalName("listener").addSourcePart().build());

    Set<MetadataKey> availableTables = listener.get().getKeys("DbCategory").get();

    assertThat(availableTables, hasItems(metadataKeyWithId("PLANET"), metadataKeyWithId("SPACESHIP")));
  }
}
