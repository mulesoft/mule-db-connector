/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureWithinPackageTestCase extends AbstractDbIntegrationTestCase {

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

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-oracle-within-package.xml"};
  }

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).createPackages(getDefaultDataSource().getConnection());
  }

  @Test
  public void callStoredProceduresWithTheSameNameOnDifferentPackages() throws Exception {
    Map<String, Object> payloadSP1 = runProcedure("magicPackageVersionOneAddMagicalNumber7");
    Map<String, Object> payloadSP2 = runProcedure("magicPackageVersionTwoAddMagicalNumber9");
    assertThat(payloadSP1.get("num").toString(), equalTo("17"));
    assertThat(payloadSP2.get("num").toString(), equalTo("19"));
  }

  @Test
  public void callStoredProcedureWithAUniqueIdentifier() throws Exception {
    Map<String, Object> payloadSP = runProcedure("addMagicalNumberAndACardNumber11WithStoredProcedurePackage");
    assertThat(payloadSP.get("num").toString(), equalTo("21"));
  }
}
