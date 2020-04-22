/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.insert;

import static java.sql.Statement.SUCCESS_NO_INFO;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;

import org.junit.Ignore;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.sql.Array;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BulkInsertTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/bulk-insert-config.xml"};
  }

  @Test
  public void dynamicBulkInsert() throws Exception {
    Message response = flowRunner("bulkInsert").withPayload(values()).run().getMessage();
    assertBulkInsert(response.getPayload().getValue());
  }

  @Test
  public void bulkInsertWithOverriddenType() throws Exception {
    Message response = flowRunner("bulkInsertWithOverriddenType").withPayload(values()).run().getMessage();
    assertBulkInsert(response.getPayload().getValue());
  }

  @Test
  @Ignore
  public void bulkInsertUnusedParameterType() throws Exception {
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    expectedException
        .expectMessage(containsString("Query defines parameters ['unused'] but they aren't present in the query"));
    flowRunner("bulkInsertWithUnusedParameterType").withPayload(values()).run().getMessage();
  }

  @Test
  public void bulkInsertInsideForEachScope() throws Exception {
    List<String> planetNames = Arrays.asList("Vogsphere", "Caprica", "Coruscant", "Worlorn", "Ego", "Krypton", "Gallifrey",
            "Cybertron", "Dagobah", "Terra Prime");
    Message response =
        flowRunner("bulkInsertInsideForEachScope").withPayload(valuesWithIncrementalPosition(planetNames)).keepStreamsOpen().run().getMessage();
    response.getPayload().getValue();

    assertRecords(response.getPayload().getValue(),
        new Record(new Field("NAME", planetNames.get(0)), new Field("POSITION", 0)),
        new Record(new Field("NAME", planetNames.get(1)), new Field("POSITION", 1)),
        new Record(new Field("NAME", planetNames.get(2)), new Field("POSITION", 2)),
        new Record(new Field("NAME", planetNames.get(3)), new Field("POSITION", 3)),
        new Record(new Field("NAME", planetNames.get(4)), new Field("POSITION", 4)),
        new Record(new Field("NAME", planetNames.get(5)), new Field("POSITION", 5)),
        new Record(new Field("NAME", planetNames.get(6)), new Field("POSITION", 6)),
        new Record(new Field("NAME", planetNames.get(7)), new Field("POSITION", 7)),
        new Record(new Field("NAME", planetNames.get(8)), new Field("POSITION", 8)),
        new Record(new Field("NAME", planetNames.get(9)), new Field("POSITION", 9))
    );

    //assertThat(resultList.size(), is(planetNames.size()));
   //   assertThat(result, anyOf(equalTo(1), equalTo(SUCCESS_NO_INFO)));
    //}
    //assertPlanetRecordsFromQuery("Vogsphere", "Caprica", "Coruscant", "Worlorn", "Ego", "Krypton", "Gallifrey", "Cybertron",
     //                            "Dagobah", "Terra Prime");
  }

  private List<Map<String, Object>> values() {
    List<Map<String, Object>> values = new ArrayList<>();
    addRecord(values, "Pluto", 777);
    addRecord(values, "Saturn", 777);
    return values;
  }

  private List<Map<String, Object>> valuesWithIncrementalPosition(List<String> planetNames) {
    List<Map<String, Object>> values = new ArrayList<>();
    for (int i = 0; i < planetNames.size(); i++) {
      addRecord(values, planetNames.get(i), 333 + i);
    }
    return values;
  }


  private void addRecord(List<Map<String, Object>> values, String planetName, int position) {
    Map<String, Object> record = new HashMap<>();
    record.put("name", planetName);
    record.put("position", position);
    values.add(record);
  }

  private void assertBulkInsert(Object payload) throws SQLException {
    assertTrue(payload instanceof int[]);
    int[] counters = (int[]) payload;
    assertThat(counters.length, is(2));
    assertThat(counters[0], anyOf(equalTo(1), equalTo(SUCCESS_NO_INFO)));
    assertThat(counters[1], anyOf(equalTo(1), equalTo(SUCCESS_NO_INFO)));
    assertPlanetRecordsFromQuery("Pluto", "Saturn");
  }
}
