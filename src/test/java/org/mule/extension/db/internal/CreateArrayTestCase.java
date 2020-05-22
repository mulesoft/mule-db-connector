/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.db.api.param.JdbcType.BLOB;
import static org.mule.extension.db.api.param.JdbcType.CLOB;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;

import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;


public class CreateArrayTestCase extends AbstractDbFunctionTestCase {

  private static final String QUERY_ALL_COLL_TYPES = "SELECT * FROM SYS.ALL_COLL_TYPES WHERE TYPE_NAME = ?";

  @Test
  public void createsDbArrayResolvingBlobWithOracleConnectionUsingSimpleName() throws Exception {
    createsDbArrayResolvingBlobWithOracleConnection(TYPE_NAME);
  }

  @Test
  public void createsDbArrayResolvingBlobWithOracleConnectionUsingFullName() throws Exception {
    createsDbArrayResolvingBlobWithOracleConnection(TYPE_NAME_WITH_OWNER);
  }

  private void createsDbArrayResolvingBlobWithOracleConnection(String typeName) throws Exception {
    Object[] structValues = {"blob", "foo"};
    Object[] structValues1 = {"blob1", "foo1"};
    Object[] params = {structValues, structValues1};

    Connection connection = mock(Connection.class);
    Blob blob = mock(Blob.class);
    when(connection.createBlob()).thenReturn(blob);

    Array array = mock(Array.class);
    when(connection.createArrayOf(typeName, params)).thenReturn(array);

    testThroughOracleQuery(connection, params, BLOB.getDbType().getName(), typeName);

    verify(connection).createArrayOf(typeName, params);
    assertThat(((Object[]) params[0])[0], equalTo(blob));
    assertThat(((Object[]) params[1])[0], equalTo(blob));
  }

  @Test
  public void createsDbArrayResolvingClobWithOracleConnectionUsingSimpleName() throws Exception {
    createsDbArrayResolvingClobWithOracleConnection(TYPE_NAME);
  }

  @Test
  public void createsDbArrayResolvingClobWithOracleConnectionUsingFullName() throws Exception {
    createsDbArrayResolvingClobWithOracleConnection(TYPE_NAME_WITH_OWNER);
  }

  private void createsDbArrayResolvingClobWithOracleConnection(String typeName) throws Exception {
    Object[] structValues = {"clob", "foo"};
    Object[] structValues1 = {"clob1", "foo1"};
    Object[] params = {structValues, structValues1};

    Connection connection = mock(Connection.class);
    Clob clob = mock(Clob.class);
    when(connection.createClob()).thenReturn(clob);

    Array array = mock(Array.class);
    when(connection.createArrayOf(typeName, params)).thenReturn(array);

    testThroughOracleQuery(connection, params, CLOB.getDbType().getName(), typeName);

    verify(connection).createArrayOf(typeName, params);
    assertThat(((Object[]) params[0])[0], equalTo(clob));
    assertThat(((Object[]) params[1])[0], equalTo(clob));
  }

  private void testThroughOracleQuery(Connection delegate, Object[] values, String dataTypeName, String udtName)
      throws Exception {
    Optional<String> owner = getOwnerFrom(udtName);
    String typeSimpleName = getTypeSimpleName(udtName);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    if (owner.isPresent()) {
      when(delegate.prepareStatement(QUERY_TYPE_ATTRS + QUERY_OWNER_CONDITION)).thenReturn(preparedStatement);
      when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES + QUERY_OWNER_CONDITION)).thenReturn(preparedStatement);
    } else {
      when(delegate.prepareStatement(QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
      when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);
    }

    when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(ATTR_NO_PARAM)).thenReturn(1);
    when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(dataTypeName);

    OracleDbConnection oracleConnection = new OracleDbConnection(delegate, new ArrayList<>(), new ConcurrentHashMap<>());

    oracleConnection.createArrayOf(udtName, values);

    verify(preparedStatement, times(2)).setString(1, typeSimpleName);
    if (owner.isPresent()) {
      verify(preparedStatement, times(2)).setString(2, owner.get());
    }
  }

  @Test
  public void createsDbArrayResolvingClobWithDefaultConnection() throws Exception {
    Object[] structValues = {"clob", "foo"};
    Object[] structValues1 = {"clob1", "foo1"};
    Object[] params = {structValues, structValues1};

    Connection connection = mock(Connection.class);
    Clob clob = mock(Clob.class);
    when(connection.createClob()).thenReturn(clob);

    Array array = mock(Array.class);
    when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

    DefaultDbConnection defaultDbConnection =
        mockDefaultDbConnectionMetadata(connection, CLOB.getDbType().getId(), CLOB.getDbType().getName());

    defaultDbConnection.createArrayOf(TYPE_NAME, params);

    verify(connection).createArrayOf(TYPE_NAME, params);
    assertThat(((Object[]) params[0])[0], equalTo(clob));
    assertThat(((Object[]) params[1])[0], equalTo(clob));
  }

  @Test
  public void createsDbArrayResolvingBlobWithDefaultConnection() throws Exception {
    Object[] structValues = {"blob", "foo"};
    Object[] structValues1 = {"blob1", "foo1"};
    Object[] params = {structValues, structValues1};

    Connection connection = mock(Connection.class);
    Blob blob = mock(Blob.class);
    when(connection.createBlob()).thenReturn(blob);

    Array array = mock(Array.class);
    when(connection.createArrayOf(TYPE_NAME, params)).thenReturn(array);

    DefaultDbConnection defaultDbConnection =
        mockDefaultDbConnectionMetadata(connection, BLOB.getDbType().getId(), BLOB.getDbType().getName());

    defaultDbConnection.createArrayOf(TYPE_NAME, params);

    verify(connection).createArrayOf(TYPE_NAME, params);
    assertThat(((Object[]) params[0])[0], equalTo(blob));
    assertThat(((Object[]) params[1])[0], equalTo(blob));
  }

}
