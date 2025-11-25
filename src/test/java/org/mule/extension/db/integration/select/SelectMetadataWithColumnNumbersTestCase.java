/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.db.integration.AbstractDbMetadataIntegrationTestCase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

/**
 * Test case for metadata resolution with column numbers support.
 * Tests that queries with duplicate column names can be resolved when useColumnNumbers is enabled.
 */
public class SelectMetadataWithColumnNumbersTestCase extends AbstractDbMetadataIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {
        "integration/select/select-metadata-with-column-numbers-config.xml"
    };
  }

  @Test
  public void testMetadataWithColumnNumbers() throws Exception {
    ObjectType record = getSelectOutputMetadata("selectAllColumnsWithColumnNumbers");

    // When using column numbers, keys should be numeric: "1", "2", "3", etc.
    assertThat(record.getFields().size(), equalTo(5)); // PLANET table has 4 columns
    assertThat(record.getFieldByName("1").isPresent(), is(true));
    assertThat(record.getFieldByName("2").isPresent(), is(true));
    assertThat(record.getFieldByName("3").isPresent(), is(true));
    assertThat(record.getFieldByName("4").isPresent(), is(true));
    assertThat(record.getFieldByName("5").isPresent(), is(true));
  }

  @Test
  public void testMetadataWithDuplicateColumns() throws Exception {
    // With useColumnNumbers=true, duplicate column names should work
    ObjectType record = getSelectOutputMetadata("selectDuplicateColumnsWithColumnNumbers");

    // Should have 2 fields with numeric keys
    assertThat(record.getFields().size(), equalTo(2));
    assertThat(record.getFieldByName("1").isPresent(), is(true));
    assertThat(record.getFieldByName("2").isPresent(), is(true));
  }

  @Test
  public void testQuerySingleMetadataWithColumnNumbers() throws Exception {
    ObjectType record = getQuerySingleOutputMetadata("querySingleMetadataWithColumnNumbers");

    // When using column numbers, keys should be numeric: "1", "2", "3", etc.
    assertThat(record.getFields().size(), equalTo(5)); // PLANET table has 5 columns
    assertThat(record.getFieldByName("1").isPresent(), is(true));
    assertThat(record.getFieldByName("2").isPresent(), is(true));
    assertThat(record.getFieldByName("3").isPresent(), is(true));
    assertThat(record.getFieldByName("4").isPresent(), is(true));
    assertThat(record.getFieldByName("5").isPresent(), is(true));
  }

  @Test
  public void testQuerySingleMetadataWithDuplicateColumns() throws Exception {
    // With useColumnNumbers=true, duplicate column names should work for querySingle
    ObjectType record = getQuerySingleOutputMetadata("querySingleDuplicateColumnsWithColumnNumbers");

    // Should have 2 fields with numeric keys
    assertThat(record.getFields().size(), equalTo(2));
    assertThat(record.getFieldByName("1").isPresent(), is(true));
    assertThat(record.getFieldByName("2").isPresent(), is(true));
  }

  private ObjectType getSelectOutputMetadata(String flowName) {
    // Pass null to use the query configuration from the XML flow
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata(flowName, null);
    assertThat(metadata.isSuccess(), is(true));
    ArrayType output = (ArrayType) metadata.get().getModel().getOutput().getType();
    return (ObjectType) output.getType();
  }

  private ObjectType getQuerySingleOutputMetadata(String flowName) {
    // Pass null to use the query configuration from the XML flow
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata(flowName, null);
    assertThat(metadata.isSuccess(), is(true));
    // querySingle returns a single object, not an array
    return (ObjectType) metadata.get().getModel().getOutput().getType();
  }
}
