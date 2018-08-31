/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.ddl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class ExecuteDdlTestCase extends AbstractDbIntegrationTestCase {

  @ClassRule
  public static SystemProperty queryTimeout = new SystemProperty("queryTimeout", "10");
  @ClassRule
  public static SystemProperty queryTimeoutUnit = new SystemProperty("queryTimeoutUnit", "SECONDS");

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/ddl/execute-ddl-config.xml"};
  }

  @Before
  public void deleteTestDdlTable() throws Exception {

    DataSource dataSource = getDefaultDataSource();
    try (Connection connection = dataSource.getConnection()) {
      QueryRunner qr = new QueryRunner(dataSource);
      qr.update(connection, "DROP TABLE TestDdl");
    } catch (SQLException e) {
      // Ignore: table does not exist
    }
  }

  @Test
  public void executeDdl() throws Exception {
    Message response = flowRunner("executeDdl").run().getMessage();
    assertTableCreation((int) response.getPayload().getValue());
  }

  protected void assertTableCreation(int affectedRows) throws SQLException {
    assertThat(affectedRows, is(0));
    List<Map<String, String>> result = selectData("select * from TestDdl", getDefaultDataSource());
    assertRecords(result);
  }

  @Test
  public void executeStructCreation() throws Exception {
    Object[] structValues = {"foo", "bar"};

    DataSource dataSource = getDefaultDataSource();
    try (Connection connection = dataSource.getConnection()) {
      connection.createStruct("BLOB_AND_CLOB_TYPE", structValues);
      assertThat(structValues[0], instanceOf(Blob.class));
      assertThat(structValues[1], instanceOf(Clob.class));
    } catch (SQLException e) {
      // Ignore: table does not exist
    }
  }
}
