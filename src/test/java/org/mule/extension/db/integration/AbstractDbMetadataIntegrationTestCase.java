/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;

import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.runner.RunnerDelegateTo;

import javax.inject.Inject;

import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractDbMetadataIntegrationTestCase extends AbstractDbIntegrationTestCase {

  @Inject
  protected MetadataService metadataService;

  protected MetadataResult<ComponentMetadataDescriptor<OperationModel>> getMetadata(String flow, String query) {
    Location location = builder().globalName(flow).addProcessorsPart().addIndexPart(0).build();

    return isAllBlank(query) ? metadataService.getOperationMetadata(location)
        : metadataService.getOperationMetadata(location, newKey(query).build());
  }

  protected MetadataType getInputMetadata(String flow, String query) {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata(flow, query);

    assertThat(metadata.isSuccess(), is(true));
    return metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters") || p.getName().equals("bulkInputParameters"))
        .findFirst().get().getType();
  }

  protected MetadataType getParameterValuesMetadata(String flow, String query) {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata(flow, query);
    assertThat(metadata.isSuccess(), is(true));
    return metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters") || p.getName().equals("bulkInputParameters"))
        .findFirst().get().getType();
  }


}
