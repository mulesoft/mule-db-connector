/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.function.oracle;

import static java.lang.reflect.Array.get;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class CreateArrayOracleTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameter(4)
  public String flowSuffix;

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "Create-Array"});
    }
    return configs;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/function/oracle/create-array.xml"};
  }

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).createPersonTable(getDefaultDataSource().getConnection());
  }

  @Test
  public void createArrayWithClobDataType() throws Exception {
    createArrayWithClob("createArrayFromObjectArray");
  }

  @Test
  public void createArrayFromList() throws Exception {
    createArrayWithClob("createArrayFromDWArray");
  }

  @Test
  public void createArrayFromStruct() throws Exception {
    createArrayWithClob("createArrayFromStruct");
  }

  private void createArrayWithClob(String createArrayFromStruct) throws Exception {
    CoreEvent createArray = flowRunner(createArrayFromStruct).run();
    TypedValue<Array> personTable = (TypedValue<Array>) createArray.getVariables().get("PERSON");
    validateOutputSQLArray(personTable);
  }

  private void validateOutputSQLArray(TypedValue<Array> personTable) throws SQLException {
    Object[] array = (Object[]) personTable.getValue().getArray();
    Struct struct = (Struct) array[0];
    Object[] attributes = struct.getAttributes();

    assertThat(attributes[0], is(instanceOf(String.class)));
    assertThat(attributes[1], is(instanceOf(Clob.class)));
    assertThat(attributes[2], is(instanceOf(BigDecimal.class)));
  }

  @After
  public void clean() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).dropPersonTable(getDefaultDataSource().getConnection());
  }
}
