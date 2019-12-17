/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.type;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.sql.Types.VARCHAR;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.OTHER;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResolvedDbTypeTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;
  private static final String TYPE_NAME_VARCHAR = "VARCHAR";
  private static final String TYPE_NAME_NUMERIC = "NUMERIC";
  private static final String EXCEPTION_MSG = "Error setting parameter!";

  private PreparedStatement statement;
  private Connection connection;
  private DbConnection dbConnection;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    statement = spy(PreparedStatement.class);
    connection = mock(Connection.class);
    dbConnection = mock(DbConnection.class);
    when(statement.getConnection()).thenReturn(connection);
  }

  @Test
  public void resolvedDbTypeResolvesBasicType() throws SQLException {
    String paramValue = "myTestString";
    ResolvedDbType resolvedDbType = new ResolvedDbType(VARCHAR, TYPE_NAME_VARCHAR);
    resolvedDbType.setParameterValue(statement, PARAM_INDEX, paramValue, dbConnection);

    verify(statement).setObject(PARAM_INDEX, paramValue, VARCHAR);
  }

  @Test
  public void resolvedDbTypeOnSqlExceptionResolvesTypeOther() throws SQLException {
    int paramValue = 8;
    ResolvedDbType resolvedDbType = new ResolvedDbType(NUMERIC, TYPE_NAME_NUMERIC);

    doThrow(new SQLException(EXCEPTION_MSG)).when(statement).setObject(eq(PARAM_INDEX), eq(paramValue), eq(NUMERIC));

    expectedException.reportMissingExceptionWithMessage(EXCEPTION_MSG);
    resolvedDbType.setParameterValue(statement, PARAM_INDEX, paramValue, dbConnection);

    verify(statement).setObject(PARAM_INDEX, paramValue, OTHER);
  }

  @Test
  public void resolvedDbTypeOnSqlExceptionSettingTypeOtherThrowsSqlException() throws SQLException {
    int paramValue = 8;
    ResolvedDbType resolvedDbType = new ResolvedDbType(NUMERIC, TYPE_NAME_NUMERIC);

    doThrow(new SQLException(EXCEPTION_MSG)).when(statement).setObject(eq(PARAM_INDEX), eq(paramValue), eq(NUMERIC));
    doThrow(new SQLException(EXCEPTION_MSG)).when(statement).setObject(eq(PARAM_INDEX), eq(paramValue), eq(OTHER));

    expectedException.expect(SQLException.class);
    expectedException.expectMessage(EXCEPTION_MSG);
    resolvedDbType.setParameterValue(statement, PARAM_INDEX, paramValue, dbConnection);

    verify(statement).setObject(PARAM_INDEX, paramValue, OTHER);
  }
}
