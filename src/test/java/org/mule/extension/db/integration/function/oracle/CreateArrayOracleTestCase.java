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

import java.sql.Clob;
import java.sql.SQLException;
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
    Object person = new Object[] {1234, "Apple", 45};
    Object otherPerson = new Object[] {1235, "Name", 33};

    Object[] persons = {person, otherPerson};

    flowRunner("createArray")
        .withVariable("array", persons).run();

    assertThat(persons.length, is(2));
    assertThat(get(person, 1), instanceOf(Clob.class));
    assertThat(get(otherPerson, 1), instanceOf(Clob.class));
  }

  @After
  public void clean() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).dropPersonTable(getDefaultDataSource().getConnection());
  }
}
