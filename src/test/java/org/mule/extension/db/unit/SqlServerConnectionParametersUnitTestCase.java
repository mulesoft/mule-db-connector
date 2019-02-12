/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SqlServerConnectionParametersUnitTestCase {

  private SqlServerConnectionParameters sqlServerConnectionParameters;

  @Before
  public void setUpSqlServerConnectionParameters() {
    sqlServerConnectionParameters = new SqlServerConnectionParameters();
    sqlServerConnectionParameters.setHost("HOST");
    sqlServerConnectionParameters.setInstanceName(null);
    sqlServerConnectionParameters.setPort(1443);
    sqlServerConnectionParameters.setUser("max");
    sqlServerConnectionParameters.setPassword("secretWord");
    sqlServerConnectionParameters.setDatabaseName(null);
    sqlServerConnectionParameters.setConnectionProperties(Collections.emptyMap());
  }

  @Test
  public void defaultUrl() {
    validate("jdbc:sqlserver://HOST:1443");
  }

  @Test
  public void implicitPortUrl() {
    sqlServerConnectionParameters.setPort(null);
    validate("jdbc:sqlserver://HOST");
  }

  @Test
  public void instanceNameNoPortUrl() {
    sqlServerConnectionParameters.setInstanceName("INSTANCE");
    sqlServerConnectionParameters.setPort(null);
    validate("jdbc:sqlserver://HOST\\INSTANCE");
  }

  @Test
  public void instanceNameAndPortUrl() {
    sqlServerConnectionParameters.setInstanceName("INSTANCE");
    validate("jdbc:sqlserver://HOST\\INSTANCE:1443");
  }

  @Test
  public void dDbNameUrl() {
    sqlServerConnectionParameters.setDatabaseName("databaseName");
    validate("jdbc:sqlserver://HOST:1443;databaseName=databaseName");
  }

  @Test
  public void withExplicitConnectionPropertiesAndDbName() {
    Map<String, String> connectionProperties = new HashMap<>();
    connectionProperties.put("integratedSecurity", "true");
    sqlServerConnectionParameters.setPort(3789);
    sqlServerConnectionParameters.setDatabaseName("databseName");
    sqlServerConnectionParameters.setConnectionProperties(connectionProperties);
    validate("jdbc:sqlserver://HOST:3789;databaseName=databseName;integratedSecurity=true");
  }

  public void validate(String expectedUrl) {
    assertThat(sqlServerConnectionParameters.getUrl(), is(expectedUrl));
  }

}
