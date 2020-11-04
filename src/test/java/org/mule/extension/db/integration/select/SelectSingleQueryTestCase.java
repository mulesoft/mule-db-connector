/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import org.junit.Ignore;
import org.junit.Test;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.model.Planet.VENUS;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecord;
import static org.mule.extension.db.integration.TestRecordUtil.createRecord;

public class SelectSingleQueryTestCase extends AbstractDbIntegrationTestCase {

  private static final int MAX_ITERATIONS = 50;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-single-query.xml"};
  }

  @Test
  public void singleSelectReturnsSingleResult() throws Exception {
    Record venusRecord = getVenusRecord();
    Message message = flowRunner("selectSingleRecord").withVariable("name", VENUS.getName()).run().getMessage();
    assertRecord(venusRecord, createRecord((Map<String,Object>) message.getPayload().getValue()));
  }

  @Test
  public void singleSelectReturnsSingleResultWithPooling() throws Exception {
    Record[] recordList = new Record[MAX_ITERATIONS];
    Message message = flowRunner("selectSingleRecordWithConnectionPool").withVariable("iterations", MAX_ITERATIONS).withVariable("name", VENUS.getName()).run().getMessage();
    for (int i = 0; i < MAX_ITERATIONS; i++) {
      recordList[i] = getVenusRecord();
    }
    assertRecords(message.getPayload().getValue(), recordList);
  }

  @Test
  public void singleSelectReturnsNoResults() throws Exception {
    Message message = flowRunner("querySingleWithNoRecords").run().getMessage();
    assertThat(((Map<String,Object>) message.getPayload().getValue()).size(), is(0));
  }

  @Test
  public void singleSelectReturnsSingleResultEvenIfStatementReturnsMore() throws Exception {
    Message message = flowRunner("querySingleWithManyRecordsReturnsOnlyOne").run().getMessage();
    assertThat(((Map<String,Object>) message.getPayload().getValue()).size(), is(1));
  }
}
