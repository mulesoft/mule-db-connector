/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.insert.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.select.Fruit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.runners.Parameterized;

public class WithMappingInsertNestedTablesTestCase extends AbstractOracleInsertNestedTableTestCase {

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-with-column-types-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "With-Mapping"});
    }
    return configs;
  }

  @Override
  protected void assertRow(Map<String, Object> row) {
    Object[] fruits = (Object[]) row.get("FRUITS");
    Object possibleFruit = fruits[0];
    assertThat(possibleFruit, is(instanceOf(Fruit.class)));

    Fruit fruit = (Fruit) possibleFruit;
    assertThat(fruit.getFruitID(), is(123L));
    assertThat(fruit.getFruitName(), is("Apple"));
    assertThat(fruit.getFruitQuantity(), is(456));
  }
}
