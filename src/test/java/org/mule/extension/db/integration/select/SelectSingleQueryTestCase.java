/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import org.junit.Before;
import org.junit.Test;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Map;
import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.model.Planet.VENUS;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecord;
import static org.mule.extension.db.integration.TestRecordUtil.createRecord;

public class SelectSingleQueryTestCase extends AbstractDbIntegrationTestCase {

  private static final int MAX_ITERATIONS = 50;

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureParameterizedUpdatePlanetDescription(getDefaultDataSource());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-single-query.xml"};
  }

  @Test
  public void singleSelectReturnsSingleResult() throws Exception {
    Record venusRecord = getVenusRecord();
    Message message = flowRunner("selectSingleRecord").withVariable("name", VENUS.getName()).run().getMessage();
    Map<String, Object> row = (Map<String, Object>) message.getPayload().getValue();
    assertThat(row.size(), is(3));
    assertRecord(venusRecord, createRecord(row));
  }

  @Test
  public void singleSelectReturnsSingleResultWithPooling() throws Exception {
    Record[] recordList = new Record[MAX_ITERATIONS];
    Message message = flowRunner("selectSingleRecordWithConnectionPool").withVariable("iterations", MAX_ITERATIONS)
        .withVariable("name", VENUS.getName()).run().getMessage();
    for (int i = 0; i < MAX_ITERATIONS; i++) {
      recordList[i] = getVenusRecord();
    }
    assertRecords(message.getPayload().getValue(), recordList);
  }

  @Test
  public void singleSelectReturnsNoResults() throws Exception {
    Message message = flowRunner("querySingleWithNoRecords").run().getMessage();
    assertThat(((Map<String, Object>) message.getPayload().getValue()).size(), is(0));
  }

  @Test
  public void singleSelectReturnsSingleResultEvenIfStatementReturnsMore() throws Exception {
    Message message = flowRunner("querySingleWithManyRecordsReturnsOnlyOne").run().getMessage();
    assertThat(((Map<String, Object>) message.getPayload().getValue()).size(), is(1));
  }

  @Test
  public void querySingleRecordWithClobField() throws Exception {
    String description = "Venus is the second planet from the Sun. It is named after the Roman goddess of love and beauty.";

    Message message = flowRunner("querySingleWithRecordWithClobField").withPayload(description)
        .withVariable("name", VENUS.getName()).run().getMessage();
    Map<String, Object> row = (Map<String, Object>) message.getPayload().getValue();
    assertThat(row.size(), is(1));
    assertThat(((TypedValue<Object>) row.get("DESCRIPTION")).getValue(), is(equalTo(description)));
  }

  @Test
  public void querySingleRecordWithBlobField() throws Exception {
    byte[] picture = new byte[100];
    new Random().nextBytes(picture);

    Message message = flowRunner("querySingleWithRecordWithBlobField").withPayload(picture).withVariable("name", VENUS.getName())
        .run().getMessage();
    Map<String, Object> row = (Map<String, Object>) message.getPayload().getValue();

    assertThat(((TypedValue<Object>) row.get("PICTURE")).getValue(), is(equalTo(picture)));

  }
}
