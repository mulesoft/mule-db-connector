/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.sql.Types.ARRAY;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mule.db.commons.internal.domain.type.ArrayResolvedDbType.createUnsupportedTypeErrorMessage;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.type.ArrayResolvedDbType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ArrayResolvedDbTypeTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;
  private static final String TYPE_NAME = "testStruct";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ArrayResolvedDbType dataType;
  private PreparedStatement statement;
  private Connection connection;
  private DbConnection dbConnection;
  private Array dbArray;

  @Before
  public void setUp() throws Exception {
    dataType = new ArrayResolvedDbType(ARRAY, TYPE_NAME);
    statement = mock(PreparedStatement.class);
    connection = mock(Connection.class);
    dbConnection = mock(DbConnection.class);
    dbArray = mock(Array.class);

    when(statement.getConnection()).thenReturn(connection);
  }

  @Test
  public void convertsJavaArray() throws Exception {
    Object[] value = new Object[] {"foo", "bar"};

    when(dbConnection.createArrayOf(TYPE_NAME, value)).thenReturn(dbArray);

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(statement).setArray(PARAM_INDEX, dbArray);
  }

  @Test
  public void convertsList() throws Exception {
    List value = new ArrayList<>();
    value.add("foo");
    value.add("bar");


    when(dbConnection.createArrayOf(argThat(equalTo(TYPE_NAME)), argThat(arrayContaining("foo", "bar")))).thenReturn(dbArray);

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(statement).setArray(PARAM_INDEX, dbArray);
  }

  @Test
  public void failsToConvertUnsupportedType() throws Exception {
    Object value = new Object();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString(createUnsupportedTypeErrorMessage(value)));

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);
  }

  @Test
  public void getParameterValue_WhenGetArrayIsNull_ThenNoException() throws SQLException {
    CallableStatement callableStatementMock = mock(CallableStatement.class);

    Object parameterValue = dataType.getParameterValue(callableStatementMock, 1);

    assertNull(parameterValue);
  }
}
