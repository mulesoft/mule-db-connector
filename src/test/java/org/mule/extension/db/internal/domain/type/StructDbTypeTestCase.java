/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.sql.Types.STRUCT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.db.api.param.JdbcType.BLOB;
import static org.mule.extension.db.api.param.JdbcType.CLOB;
import static org.mule.extension.db.internal.domain.connection.oracle.AbstractJdbcConnectionWrapper.ATTR_TYPE_NAME_INDEX;
import static org.mule.extension.db.internal.domain.connection.oracle.AbstractJdbcConnectionWrapper.DATA_TYPE_INDEX;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleJdbcConnectionWrapper.ATTR_NO_PARAM;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleJdbcConnectionWrapper.ATTR_TYPE_NAME_PARAM;
import static org.mule.extension.db.internal.domain.type.StructDbType.createUnsupportedTypeErrorMessage;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.db.internal.domain.connection.oracle.AbstractJdbcConnectionWrapper;
import org.mule.extension.db.internal.domain.connection.oracle.OracleJdbcConnectionWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class StructDbTypeTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;
  private static final String TYPE_NAME = "testStruct";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private StructDbType dataType;
  private PreparedStatement statement;
  private Connection connection;
  private Struct struct;

  @Before
  public void setUp() throws Exception {
    dataType = new StructDbType(STRUCT, TYPE_NAME);
    statement = mock(PreparedStatement.class);
    connection = mock(Connection.class);
    struct = mock(Struct.class);

    when(statement.getConnection()).thenReturn(connection);
  }

  @Test
  public void convertsArrayToStruct() throws Exception {
    Object[] value = new Object[] {"foo", "bar"};

    when(connection.createStruct(TYPE_NAME, value)).thenReturn(struct);

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(statement).setObject(PARAM_INDEX, struct, STRUCT);
  }

  @Test
  public void convertsListToStruct() throws Exception {
    List value = new ArrayList<>();
    value.add("foo");
    value.add("bar");

    when(connection.createStruct(argThat(equalTo(TYPE_NAME)), argThat(arrayContaining("foo", "bar")))).thenReturn(struct);

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(statement).setObject(PARAM_INDEX, struct, STRUCT);
  }

  @Test
  public void createStructResolvingBlobInDefaultDbConnection() throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Blob blob = mock(Blob.class);
    when(delegate.createBlob()).thenReturn(blob);
    testThroughMetadata(delegate, structValues, BLOB.getDbType().getId(), BLOB.getDbType().getName());
    verify(delegate).createStruct(TYPE_NAME, structValues);
    assertThat(structValues[0], Matchers.<Object>equalTo(blob));
  }

  @Test
  public void createStructResolvingClobInDefaultDbConnection() throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Clob clob = mock(Clob.class);
    when(delegate.createClob()).thenReturn(clob);
    testThroughMetadata(delegate, structValues, CLOB.getDbType().getId(), CLOB.getDbType().getName());
    verify(delegate).createStruct(TYPE_NAME, structValues);
    assertThat(structValues[0], Matchers.<Object>equalTo(clob));
  }

  @Test
  public void createStructResolvingBlobInOracleDb() throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Blob blob = mock(Blob.class);
    when(delegate.createBlob()).thenReturn(blob);
    testThroughOracleQuery(delegate, structValues, BLOB.getDbType().getName());
    verify(delegate).createStruct(TYPE_NAME, structValues);
    assertThat(structValues[0], Matchers.<Object>equalTo(blob));
  }

  @Test
  public void createStructResolvingClobInOracleDb() throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Clob clob = mock(Clob.class);
    when(delegate.createClob()).thenReturn(clob);
    testThroughOracleQuery(delegate, structValues, CLOB.getDbType().getName());
    verify(delegate).createStruct(TYPE_NAME, structValues);
    assertThat(structValues[0], Matchers.<Object>equalTo(clob));
  }

  private void testThroughOracleQuery(Connection delegate, Object[] structValues, String dataTypeName) throws Exception {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(delegate.prepareStatement(OracleJdbcConnectionWrapper.QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(ATTR_NO_PARAM)).thenReturn(1);
    when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(dataTypeName);

    OracleJdbcConnectionWrapper defaultDbConnection = new OracleJdbcConnectionWrapper(delegate);
    defaultDbConnection.createStruct(TYPE_NAME, structValues);
    defaultDbConnection.close();
  }

  private void testThroughMetadata(Connection delegate, Object[] structValues, int dataType, String dataTypeName)
      throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(delegate.getMetaData()).thenReturn(metadata);
    when(delegate.getCatalog()).thenReturn("catalog");
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(DATA_TYPE_INDEX)).thenReturn(dataType);
    when(resultSet.getString(ATTR_TYPE_NAME_INDEX)).thenReturn(dataTypeName);
    when(metadata.getAttributes("catalog", null, TYPE_NAME, null)).thenReturn(resultSet);
    TestJdbcConnectionWrapper defaultWrapper = new TestJdbcConnectionWrapper(delegate);
    defaultWrapper.createStruct(TYPE_NAME, structValues);
    defaultWrapper.close();
  }

  @Test
  public void failsToConvertUnsupportedType() throws Exception {
    Object value = new Object();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString(createUnsupportedTypeErrorMessage(value)));

    dataType.setParameterValue(statement, PARAM_INDEX, value);
  }

  /**
   * This class is used to test the default BLOB and CLOB resolver default behaviour.
   */
  private static class TestJdbcConnectionWrapper extends AbstractJdbcConnectionWrapper {

    public TestJdbcConnectionWrapper(Connection delegate) {
      super(delegate);
    }

  }
}
