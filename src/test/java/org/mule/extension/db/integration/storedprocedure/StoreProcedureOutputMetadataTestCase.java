/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class StoreProcedureOutputMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-metadata-config.xml"};
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
    MetadataType parameters = getParameterValuesMetadata("storedSingleParameterInputMetadata", null);

    assertThat(parameters, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parameters).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parameters).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }
}
