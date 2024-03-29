/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static java.util.Collections.emptyList;
import static org.mule.extension.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.model.RegionManager.NORTHWEST_MANAGER;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectJavaUdtTestCase extends AbstractDbIntegrationTestCase {

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
    return new String[] {"integration/select/select-udt-config.xml"};
  }

  @Test
  public void returnsMappedObject() throws Exception {
    Message response = flowRunner("returnsUDT").keepStreamsOpen().run().getMessage();

    assertRecords(response.getPayload().getValue(),
                  new Record(new Field("REGION_NAME", SOUTHWEST_MANAGER.getRegionName()),
                             new Field("MANAGER_NAME", SOUTHWEST_MANAGER.getName()),
                             new Field("DETAILS", SOUTHWEST_MANAGER.getContactDetails())),
                  new Record(new Field("REGION_NAME", NORTHWEST_MANAGER.getRegionName()),
                             new Field("MANAGER_NAME", NORTHWEST_MANAGER.getName()),
                             new Field("DETAILS", NORTHWEST_MANAGER.getContactDetails())));
  }
}
