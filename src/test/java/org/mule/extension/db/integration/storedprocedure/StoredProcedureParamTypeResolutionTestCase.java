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

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StoredProcedureParamTypeResolutionTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureAddOne(getDefaultDataSource());
  }

  @After
  public void afterTest() {
    clearProperty("mule.db.connector.retrieve.param.types");
  }

  @Test
  public void runStoredProcedureResolvingParamUsingDBMetadata() throws Exception {
    setProperty("mule.db.connector.retrieve.param.types", "true");
    Map<String, Object> payload = runProcedure("addOneInputParameterWithTypedConfigured");
    assertThat("7", equalTo(payload.get("num").toString()));
  }

  @Test
  public void runStoredProcedureResolvingParamUsingConfiguredTypes() throws Exception {
    setProperty("mule.db.connector.retrieve.param.types", "false");
    Map<String, Object> payload = runProcedure("addOneInputParameterWithTypedConfigured");
    assertThat("7", equalTo(payload.get("num").toString()));
  }
}
