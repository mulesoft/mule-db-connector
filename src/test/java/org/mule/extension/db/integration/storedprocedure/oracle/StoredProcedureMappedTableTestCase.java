/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.select.Fruit;
import org.mule.runtime.api.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureMappedTableTestCase extends AbstractStoredProcedureTableTestCase {

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-with-column-types-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "WithMapping"});
    }
    return configs;
  }

  @Override
  protected void validateStructData(List<?> retrievedFruits, Fruit[] fruits) {
    List<Map<String, Fruit>> receivedFruits = (List<Map<String, Fruit>>) retrievedFruits;
    for (Fruit fruit : fruits) {
      assertThat(receivedFruits, hasItem(hasEntry(is("FRUIT"), is(fruit))));
    }
  }

  @Override
  public void validateTable(Map<String, Object> storeProcedureReturn) {
    Object value = ((Object[]) storeProcedureReturn.get("out"))[0];
    assertThat(value, is(instanceOf(Fruit.class)));
  }

  @Test
  public void processNestedTypeOutputParam() throws Exception {
    flowRunner("storedProcedureWithNestedArrayTypeOutputParamSerialized").run();
  }

  @Test
  public void processNestedObjectTypeOutputParam() throws Exception {
    Message response = flowRunner("storedProcedureWithNestedObjectTypeOutputParamSerialized").run().getMessage();
    System.out.println("Hola");
  }
}
