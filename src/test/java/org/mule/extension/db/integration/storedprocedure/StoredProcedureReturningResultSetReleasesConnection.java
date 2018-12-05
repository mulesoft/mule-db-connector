package org.mule.extension.db.integration.storedprocedure;

import static org.junit.Assume.assumeThat;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;

import org.junit.Before;
import org.junit.Test;

public class StoredProcedureReturningResultSetReleasesConnection extends AbstractDbIntegrationTestCase {

  private static final int TIMES_TO_CALL_STORED_PROCEDURE = 1;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-returning-resultset-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureGetSplitRecords(getDefaultDataSource());
  }

  @Test
  public void connectionsReleasesToPoolWithStreamedResponse() throws Exception {
    for (int i = 0; i < TIMES_TO_CALL_STORED_PROCEDURE; i++) {
      flowRunner("getResultSet").keepStreamsOpen().run().getMessage();
    }
  }

}
