/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static org.mule.db.commons.api.param.JdbcType.BLOB;
import static org.mule.db.commons.api.param.JdbcType.CLOB;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection.QUERY_PKG_ATTRS;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class OracleCreateStructTestCase extends AbstractDbFunctionTestCase {

  private static final long CACHE_MAXIMUM_SIZE = 100;

  @Test
  public void createStructResolvingBlobInOracleDbUsingUDTSimpleName() throws Exception {
    createStructResolvingBlobInOracleDb(TYPE_NAME);
  }

  @Test
  public void createStructResolvingBlobInOracleDbUsingUDTFullName() throws Exception {
    createStructResolvingBlobInOracleDb(TYPE_NAME_WITH_OWNER);
  }

  @Test
  public void createStructResolvingClobInOracleDbUsingUDTSimpleName() throws Exception {
    createStructResolvingClobAndClobInOracleDb(TYPE_NAME);
  }

  @Test
  public void createStructResolvingClobInOracleDbUsingUDTFullName() throws Exception {
    createStructResolvingClobAndClobInOracleDb(TYPE_NAME_WITH_OWNER);
  }

  private void createStructResolvingBlobInOracleDb(String typeName) throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Blob blob = mock(Blob.class);
    when(delegate.createBlob()).thenReturn(blob);
    testThroughOracleQuery(delegate, structValues, BLOB.getDbType().getName(), typeName);
    verify(delegate).createStruct(typeName, structValues);
    assertThat(structValues[0], equalTo(blob));
  }

  private void createStructResolvingClobAndClobInOracleDb(String typeName) throws Exception {
    Object[] structValues = {"foo", "bar"};
    Connection delegate = mock(Connection.class);
    Clob clob = mock(Clob.class);
    when(delegate.createClob()).thenReturn(clob);
    testThroughOracleQuery(delegate, structValues, CLOB.getDbType().getName(), typeName);
    verify(delegate).createStruct(typeName, structValues);
    assertThat(structValues[0], equalTo(clob));
  }

  private void testThroughOracleQuery(Connection delegate, Object[] structValues, String dataTypeName, String udtName)
      throws Exception {
    Optional<String> owner = getOwnerFrom(udtName);
    String typeSimpleName = getTypeSimpleName(udtName);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    if (owner.isPresent()) {
      when(delegate.prepareStatement(QUERY_TYPE_ATTRS + QUERY_OWNER_CONDITION + " UNION ALL " + QUERY_PKG_ATTRS))
          .thenReturn(preparedStatement);
    } else {
      when(delegate.prepareStatement(QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
    }

    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(ATTR_NO_PARAM)).thenReturn(1);
    when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(dataTypeName);

    OracleDbConnection oracleConnection = new OracleDbConnection(delegate, new ArrayList<>(), new ConcurrentHashMap<>(),
                                                                 newBuilder().maximumSize(CACHE_MAXIMUM_SIZE).build(),
                                                                 new ConcurrentHashMap<>());

    oracleConnection.createStruct(udtName, structValues);

    verify(preparedStatement).setString(1, typeSimpleName);

    if (owner.isPresent()) {
      verify(preparedStatement).setString(2, owner.get());
    }
  }

}
