/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure.oracle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Alien;
import org.mule.extension.db.integration.model.OracleTestDatabase;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;

public class StoredProcedureXMLTypeTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      params.add(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList()});
    }

    return params;
  }

  @Before
  public void init() throws SQLException {
    this.testDatabase.createStoredProcedureGetAlienDescription(getDefaultDataSource());
    this.testDatabase.createStoredProcedureUpdateAlienDescription(getDefaultDataSource());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-oracle-xmltype-config.xml"};
  }

  @Test
  public void testXMLTypeInputAndOutputParam() throws Exception {
    Alien firstAlien = AbstractTestDatabase.ALIEN_TEST_VALUES[1];
    Alien alienToUpdate = new Alien("SomePlanet", firstAlien.getName(), firstAlien.getGender(), firstAlien.isFriendly());
    Map<String, String> payload = new HashMap<>();
    payload.put("name", alienToUpdate.getName());
    payload.put("description", alienToUpdate.getXml());

    runProcedure("xmlTypeInputParam", payload);

    Map<String, Object> result = runProcedure("xmlTypeOutputParam", payload);
    assertEquals(result.get("description"), alienToUpdate.getXml());
  }

}
