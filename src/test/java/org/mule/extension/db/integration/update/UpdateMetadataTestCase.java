/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.mule.extension.db.AllureConstants.DbFeature.DB_EXTENSION;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbMetadataIntegrationTestCase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(DB_EXTENSION)
@Story("Update Statement")
public class UpdateMetadataTestCase extends AbstractDbMetadataIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-metadata-config.xml"};
  }

  @Test
  public void updateOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("updateOutputMetadata", null);

    assertOutputPayload(metadata, typeLoader.load(StatementResult.class));
  }

  @Test
  public void bulkUpdateOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("bulkUpdateOutputMetadata", null);

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void bulkUpdateNoParametersInputMetadata() throws Exception {
    MetadataType parameters =
        getParameterValuesMetadata("bulkUpdateNoParametersInputMetadata", null);
    assertThat(parameters, is(instanceOf(NullType.class)));
  }

  @Test
  public void bulkUpdateParameterizedInputMetadata() throws Exception {
    MetadataType parameters =
        getParameterValuesMetadata("bulkUpdateParameterizedInputMetadata", null);

    assertThat(parameters, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parameters).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parameters).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void updateNoParametersInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("updateNoParametersInputMetadata", null);
    assertThat(parameters, is(instanceOf(NullType.class)));
  }

  @Test
  public void updateParameterizedInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("updateParameterizedInputMetadata", null);

    assertThat(parameters, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void updateWithExpressionInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("updateWithExpressionInputMetadata", null);
    assertThat(parameters, is(typeBuilder.anyType().build()));
  }

}
