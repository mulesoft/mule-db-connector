/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.mule.db.commons.internal.domain.metadata.SelectMetadataResolver.DUPLICATE_COLUMN_LABEL_ERROR;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.mule.extension.db.integration.AbstractDbMetadataIntegrationTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import org.junit.Test;

public class SelectMetadataOutputTestCase extends AbstractDbMetadataIntegrationTestCase {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-metadata-config.xml"};
  }

  @Test
  public void selectAll() throws Exception {
    assertPlanetObjectType(getSelectOutputMetadata("selectAll"));
  }

  @Test
  public void selectSome() throws Exception {
    ObjectType record = getSelectOutputMetadata("selectSome");

    assertThat(record.getFields().size(), equalTo(2));
    assertFieldOfType(record, "ID", testDatabase.getIdFieldMetaDataType());
    assertFieldRequirement(record, "ID", true);
    assertFieldOfType(record, "POSITION", testDatabase.getPositionFieldMetaDataType());
  }

  @Test
  public void selectJoin() throws Exception {
    ObjectType record = getSelectOutputMetadata("selectJoin");

    assertThat(record.getFields().size(), equalTo(2));
    assertFieldOfType(record, "NAME", typeBuilder.stringType().build());
    assertFieldOfType(record, "NAME2", typeBuilder.stringType().build());
  }

  @Test
  public void selectInvalidJoin() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("selectInvalidJoin", null);
    assertThat(metadata.isSuccess(), is(false));
    assertThat(metadata.getFailures(), hasSize(1));
    MetadataFailure failure = metadata.getFailures().get(0);
    assertThat(failure.getFailureCode(), is(FailureCode.INVALID_METADATA_KEY));
    assertThat(failure.getMessage(), is(DUPLICATE_COLUMN_LABEL_ERROR));
  }

  private ObjectType getSelectOutputMetadata(String flowName) {
    // Pass null to use the query configuration from the XML flow
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata(flowName, null);
    assertThat(metadata.isSuccess(), is(true));
    ArrayType output = (ArrayType) metadata.get().getModel().getOutput().getType();
    return (ObjectType) output.getType();
  }
}
