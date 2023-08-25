/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure.oracle;

import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static java.util.Collections.emptyList;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureReturningOutputClobTestCase extends AbstractDbIntegrationTestCase {

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
