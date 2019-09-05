/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Before;
import org.junit.Test;

public class StoredProcedureMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-metadata-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureDoubleMyInt(getDefaultDataSource());
    testDatabase.createStoredProcedureConcatenateStrings(getDefaultDataSource());
    testDatabase.createStoredProcedureCountRecords(getDefaultDataSource());
    testDatabase.createStoredProcedureMultiplyInts(getDefaultDataSource());
    testDatabase.returnNullValue(getDefaultDataSource());
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
    testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
    testDatabase.createStoredProcedureParameterizedUpdatePlanetDescription(getDefaultDataSource());
    testDatabase.createStoredProcedureParameterizedUpdateTestType1(getDefaultDataSource());
    testDatabase.createStoredProcedureGetSplitRecords(getDefaultDataSource());
    testDatabase.createDelayFunction(getDefaultDataSource());
    testDatabase.createStoredProcedureConcatenateDateAndString(getDefaultDataSource());
  }

  @Test
  public void storedProcedureOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("storedOutputMetadata", "{ call getTestRecords() }");
    assertThat(metadata.isSuccess(), is(true));
    MetadataType output = metadata.get().getModel().getOutput().getType();
    assertThat(output, is(typeBuilder.objectType().build()));
  }

  @Test
  public void storedProcedureSingleParameterInputMetadata() throws Exception {
    MetadataType parameters = getParameterValuesMetadata("storedMixedParametersInputMetadata", null);

    assertThat(parameters, is(instanceOf(ObjectType.class)));
    assertFieldOfType(((ObjectType) parameters), "description", testDatabase.getDescriptionFieldMetaDataType());
  }
}
