/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class StoredProcedureStreamingTestCase extends AbstractDbIntegrationTestCase {

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureDoubleMyInt(getDefaultDataSource());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-streaming-config.xml"};
  }

  @Test
  public void streamingInOutParam() throws Exception {
    Map<String, Object> payload = runProcedure("streamingInOutParam");
    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)
    assertThat("6", equalTo(payload.get("myInt").toString()));
  }


}
