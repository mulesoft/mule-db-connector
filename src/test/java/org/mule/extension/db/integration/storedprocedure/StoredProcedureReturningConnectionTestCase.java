/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

public class StoredProcedureReturningConnectionTestCase extends AbstractDbIntegrationTestCase {

  public static final String CYCLED_SUCCESSFULLY = "CYCLED SUCCESSFULLY";
  public static final String CONNECTIVITY = "CONNECTIVITY";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-returning-connections.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureParameterizedUpdatePlanetDescription(getDefaultDataSource());
  }

  @Test
  public void returnsConnectionsWhenNoStreamNorTransaction() throws Exception {
    Message response = flowRunner("storedProcedureReturnsConnectionsWhenNoStreamNorTransaction")
        .withPayload(TEST_MESSAGE).run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(CYCLED_SUCCESSFULLY));
    assertThat(response.getPayload().getValue(), not(CONNECTIVITY));
  }

}
