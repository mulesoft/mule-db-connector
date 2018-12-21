/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure.oracle;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.rules.ExpectedException.none;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.select.Fruit;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

public abstract class AbstractStoredProcedureTableTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Parameterized.Parameter(4)
  public String flowSuffix;

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).initUdts(getDefaultDataSource().getConnection());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-oracle-table-config.xml"};
  }

  @Test
  public void insertTableTypeUsingParameters() throws Exception {
    Fruit[] fruits = getTestFruits();
    runProcedure("insertTableType" + flowSuffix, Arrays.asList(fruits));
    Iterator<Map<String, Object>> selectFromFruitsTable =
        ((CursorIteratorProvider) flowRunner("selectFromFruitsTable").keepStreamsOpen()
            .withPayload(Arrays.stream(fruits).mapToLong(Fruit::getFruitID).toArray()).run().getMessage().getPayload().getValue())
                .openCursor();
    validateInsertedData(copyOf(selectFromFruitsTable), fruits);
  }

  @Test
  public void insertTableTypeUsingStoredProcedure() throws Exception {
    Fruit[] fruits = getTestFruits();
    runProcedure("insertTableType2" + flowSuffix, Arrays.asList(fruits));
    runProcedure("insertTableType2" + flowSuffix, Arrays.asList(fruits));
    runProcedure("insertTableType2" + flowSuffix, Arrays.asList(fruits));
    runProcedure("insertTableType2" + flowSuffix, Arrays.asList(fruits));
    validateStructData(getFruitsFromDBLike(fruits), fruits);
  }

  @Test
  public void getTableTypeUsingStoredProcedure() throws Exception {
    Map<String, Object> struct = runProcedure("returnTableType" + flowSuffix);
    validateTable(struct);
  }

  @Test
  public void insertAndSelectStruct() throws Exception {
    Fruit fruit = getTestFruits()[0];
    flowRunner("insertStruct" + flowSuffix).withPayload(fruit).run();
    validateStructData(getFruitsFromDBLike(fruit), fruit);
  }

  protected abstract void validateStructData(List<?> retrievedFruits, Fruit... fruits);

  protected abstract void validateTable(Map<String, Object> storeProcedureReturn);

  private List getFruitsFromDBLike(Fruit... fruits) throws Exception {
    return copyOf(((CursorIteratorProvider) flowRunner("selectFromFruitsTable2").keepStreamsOpen()
        .withPayload(Arrays.stream(fruits).mapToLong(Fruit::getFruitID).toArray())
        .run()
        .getMessage().getPayload().getValue()).openCursor());
  }

  public void validateInsertedData(List<Map<String, Object>> storeProcedureReturn, Fruit... fruits) {
    for (Fruit fruit : fruits) {
      Matcher idMatcher = hasEntry("FRUITID", new BigDecimal(fruit.getFruitID()));
      Matcher nameMatcher = hasEntry("FRUITNAME", fruit.getFruitName());
      Matcher quantityMatcher = hasEntry("FRUITQUANTITY", new BigDecimal(fruit.getFruitQuantity()));
      Matcher<Map<String, Object>> fruitid = AllOf.allOf(idMatcher, nameMatcher, quantityMatcher);
      assertThat(storeProcedureReturn, IsCollectionContaining.hasItem(fruitid));
    }
  }

  public Fruit[] getTestFruits() {
    Random random = new Random();
    return new Fruit[] {
        new Fruit(random.nextInt() * 10000 + 1, "Apple", 123),
        new Fruit(random.nextInt() * 10000 + 1, "Banana", 123),
        new Fruit(random.nextInt() * 10000 + 1, "Orange", 123)
    };
  }
}
