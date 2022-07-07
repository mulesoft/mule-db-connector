/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select.oracle;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;

public class OracleSelectClobTestCase extends AbstractDbIntegrationTestCase {


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
    return new String[] {"integration/vendor/oracle/oracle-select-clob.xml"};
  }

  //DB Config used has a pool of 1 connection. keepStreamsOpen ensures connection is not closed.
  //If method is not used, even if there is a leak, the flow runner closes the connection as part of cleanup.
  @Test
  public void selectWith() throws Exception {
    flowRunner("select-clob").keepStreamsOpen().run();
    flowRunner("select-clob").run();
  }

  @Override
  protected void doTearDown() {

  }
}
