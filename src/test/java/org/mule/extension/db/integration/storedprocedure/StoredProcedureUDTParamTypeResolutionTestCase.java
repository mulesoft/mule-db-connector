/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureUDTParamTypeResolutionTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      params.add(getOracleResource().get(0));
    }

    if (!getDerbyResource().isEmpty()) {
      params.add(getDerbyResource().get(0));
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-udt-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetManagerDetails(getDefaultDataSource());
  }

  @After
  public void afterTest() {
    clearProperty("mule.db.connector.retrieve.param.types");
  }

  @Test
  public void runStoredProcedureThatReturnsCustomObjectResolvingParamNotUsingDBMetadata() throws Exception {
    setProperty("mule.db.connector.retrieve.param.types", "false");
    Message response = flowRunner("returnsObjectConfiguredTypes").run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }

  @Test
  public void runStoredProcedureThatReturnsCustomObjectResolvingParamUsingDBMetadata() throws Exception {
    setProperty("mule.db.connector.retrieve.param.types", "true");
    Message response = flowRunner("returnsObjectConfiguredTypes").run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }
}
