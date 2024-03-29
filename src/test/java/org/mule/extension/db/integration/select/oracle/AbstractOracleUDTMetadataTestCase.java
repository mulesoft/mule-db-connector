/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select.oracle;

import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.extension.db.integration.AbstractDbMetadataIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractOracleUDTMetadataTestCase extends AbstractDbMetadataIntegrationTestCase {

  @Parameterized.Parameter(4)
  public String flowSuffix;

  ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).initUdts(getDefaultDataSource().getConnection());
  }

  @After
  public void cleanUp() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).dropTablesTypesProcedures(getDefaultDataSource().getConnection());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-oracle-table-config.xml"};
  }

  @Test
  public void selectStructType() {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("selectFromFruitsTable", "SELECT * FROM FRUITS_AS_TYPE");
    assertThat("Metadata Result is not success", metadata.isSuccess(), is(true));

    MetadataType type = metadata.get().getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ArrayType.class)));

    MetadataType arrayContentType = ((ArrayType) type).getType();
    Optional<ObjectFieldType> fruit = getFruitType(arrayContentType);

    validateStructType(fruit.get().getValue());
  }

  @Test
  public void insertStructType() {
    MetadataType insertStruct = getParameterValuesMetadata("insertStruct", "INSERT INTO FRUITS_AS_TYPE (FRUIT) VALUES (:FRUIT)");

    Optional<ObjectFieldType> fruit = getFruitType(insertStruct);

    validateStructType(fruit.get().getValue());
  }

  private Optional<ObjectFieldType> getFruitType(MetadataType insertStruct) {
    assertThat(insertStruct, is(instanceOf(ObjectType.class)));
    ObjectType rowType = (ObjectType) insertStruct;
    Optional<ObjectFieldType> fruit = rowType.getFieldByName("FRUIT");
    assertThat(fruit, is(not(empty())));
    return fruit;
  }

  abstract void validateStructType(MetadataType metadataType);
}
