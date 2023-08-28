/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import static org.mule.db.commons.internal.resolver.param.StoredProcedureParamTypeResolver.FORCE_SP_PARAM_TYPES;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
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
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList()});
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
    clearProperty(FORCE_SP_PARAM_TYPES);
  }

  @Test
  public void runStoredProcedureThatReturnsCustomObjectResolvingParamNotUsingDBMetadata() throws Exception {
    setProperty(FORCE_SP_PARAM_TYPES, "true");
    Message response = flowRunner("returnsObjectWithAllConfiguredTypes").run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }

  @Test
  public void runStoredProcedureThatReturnsCustomObjectResolvingParamUsingDBMetadata() throws Exception {
    setProperty(FORCE_SP_PARAM_TYPES, "false");
    Message response = flowRunner("returnsObjectWithAllConfiguredTypes").run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }

  @Test
  public void runStoredProcedureThatReturnsCustomObjectResolvingParamNotUsingDBMetadataWhenNotAllParameterTypesAreConfigured()
      throws Exception {
    setProperty(FORCE_SP_PARAM_TYPES, "true");
    Message response = flowRunner("returnsObjectWitSomeParameterTypesConfigured").run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }


}
