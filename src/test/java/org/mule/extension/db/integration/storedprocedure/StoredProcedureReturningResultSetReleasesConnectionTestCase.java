/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static org.junit.Assume.assumeThat;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;

public class StoredProcedureReturningResultSetReleasesConnectionTestCase extends AbstractDbIntegrationTestCase {

  private static final int TIMES_TO_CALL_STORED_PROCEDURE = 20;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-returning-resultset-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }

  @Test
  @Description("This test ensures that connections are returned to the connection pool by calling a stored procedure more" +
      " times than the size of the pools, if the connections are not getting released this test will timeout")
  public void connectionsReleasesToPoolWithStreamedResponse() throws Exception {
    for (int i = 0; i < TIMES_TO_CALL_STORED_PROCEDURE; i++) {
      flowRunner("getResultSet").run();
    }
  }

}
