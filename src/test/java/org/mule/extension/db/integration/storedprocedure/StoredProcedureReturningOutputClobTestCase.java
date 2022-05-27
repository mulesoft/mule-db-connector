/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.extension.db.integration.model.MySqlTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.TestRecordUtil.*;

public class StoredProcedureReturningOutputClobTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-oracle-with-clob-output.xml"};
  }

  @Test
  public void verifyConnectionIsReleasedOutputClob() throws Exception {
    flowRunner("getClobOutputFromStoredProcedurePackage").run();
    flowRunner("getClobOutputFromStoredProcedurePackage").run();
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureOutputClob(getDefaultDataSource());
  }

}
