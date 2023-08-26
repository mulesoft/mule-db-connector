/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.rules.ExpectedException.none;

import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.select.Fruit;
import org.mule.functional.api.exception.ExpectedError;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

public class StoredProcedureTableTestCase extends AbstractStoredProcedureTableTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), ""});
    }
    return configs;
  }

  @Override
  protected void validateStructData(List<?> retrievedFruits, Fruit[] fruits) {

    List<Map<String, Object[]>> receivedFruits = (List<Map<String, Object[]>>) retrievedFruits;
    for (Fruit fruit : fruits) {
      Matcher fruitIDMatcher = is(new BigDecimal(fruit.getFruitID()));
      Matcher nameMatcher = is(fruit.getFruitName());
      Matcher quantityMatcher = is(new BigDecimal(fruit.getFruitQuantity()));
      Matcher fruitMatcher = hasEntry(is("FRUIT"), arrayContaining(fruitIDMatcher, nameMatcher, quantityMatcher));
      assertThat(receivedFruits, hasItem(fruitMatcher));
    }
  }

  @Override
  public void validateTable(Map<String, Object> storeProcedureReturn) {
    assertThat(storeProcedureReturn, hasEntry(is("out"), instanceOf(Object[].class)));
  }
}
