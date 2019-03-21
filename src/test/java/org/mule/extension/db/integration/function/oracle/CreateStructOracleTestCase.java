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
import org.mule.runtime.core.api.event.CoreEvent;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class CreateStructOracleTestCase extends AbstractDbIntegrationTestCase {

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).createPersonType(getDefaultDataSource().getConnection());
  }

  @Parameterized.Parameter(4)
  public String flowSuffix;

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "Create-Struct"});
    }
    return configs;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/function/oracle/create-struct.xml"};
  }

  @Test
  public void createArrayWithClobDataType() throws Exception {
    Object[] person = new Object[] {1234, "Apple", 45};

    CoreEvent coreEvent = flowRunner("createStruct")
        .withVariable("struct", person).run();

    Object result = coreEvent.getVariables().get("STRUCT").getValue();
    assertThat(result, instanceOf(java.sql.Struct.class));

    Struct struct = (Struct) result;

    assertThat(struct.getAttributes().length, is(3));

    Object clobAttribute = get(struct.getAttributes(), 1);

    assertThat(clobAttribute.getClass().getName(), is("oracle.sql.CLOB"));
  }

  @After
  public void clean() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).dropPersonType(getDefaultDataSource().getConnection());
  }

}
