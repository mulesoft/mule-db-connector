/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mule.extension.db.api.param.JdbcType.BLOB;
import static org.mule.extension.db.api.param.JdbcType.CLOB;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.internal.domain.connection.type.resolver.ArrayTypeResolver;
import org.mule.extension.db.internal.domain.connection.type.resolver.StructTypeResolver;
import org.mule.extension.db.internal.domain.connection.type.resolver.StructAndArrayTypeResolver;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.GenericStatementResultIteratorFactory;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDbConnection implements DbConnection {

  private final Connection jdbcConnection;
  private final List<DbType> customDataTypes;
  private AtomicInteger streamsCount = new AtomicInteger(0);
  private boolean isTransactionActive = false;

  private static final int DATA_TYPE_INDEX = 5;
  private static final int ATTR_TYPE_NAME_INDEX = 6;
  private static final List<String> LOB_TYPES = asList(BLOB.getDbType().getName(), CLOB.getDbType().getName());

  protected static final int UNKNOWN_DATA_TYPE = -1;

  protected static final Logger logger = LoggerFactory.getLogger(DefaultDbConnection.class);

  public DefaultDbConnection(Connection jdbcConnection, List<DbType> customDataTypes) {
    this.jdbcConnection = jdbcConnection;
    this.customDataTypes = customDataTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler) {
    return new GenericStatementResultIteratorFactory(resultSetHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getJdbcConnection() {
    return jdbcConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getCustomDataTypes() {
    return customDataTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() throws TransactionException {
    try {
      if (jdbcConnection.getAutoCommit()) {
        jdbcConnection.setAutoCommit(false);
      }
      isTransactionActive = true;
    } catch (Exception e) {
      throw new TransactionException(createStaticMessage("Could not start transaction: " + e.getMessage()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws TransactionException {
    try {
      jdbcConnection.commit();
    } catch (Exception e) {
      throw new TransactionException(createStaticMessage("Could not start transaction: " + e.getMessage()), e);
    } finally {
      isTransactionActive = false;
      abortStreaming();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws TransactionException {
    try {
      jdbcConnection.rollback();
    } catch (Exception e) {
      throw new TransactionException(createStaticMessage("Could not start transaction: " + e.getMessage()), e);
    } finally {
      isTransactionActive = false;
      abortStreaming();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    if (isStreaming()) {
      return;
    }
    try {
      jdbcConnection.close();
    } catch (SQLException e) {
      throw new ConnectionClosingException(e);
    }
  }

  @Override
  public void beginStreaming() {
    streamsCount.incrementAndGet();
  }

  @Override
  public boolean isStreaming() {
    return streamsCount.get() > 0;
  }

  @Override
  public void endStreaming() {
    streamsCount.getAndUpdate(operand -> operand <= 0 ? 0 : operand - 1);
  }

  private void abortStreaming() {
    streamsCount.set(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTransactionActive() {
    return isTransactionActive;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsContentStreaming() {
    return true;
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return jdbcConnection.prepareStatement(sql);
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    resolveLobs(typeName, elements, new ArrayTypeResolver(this));
    return jdbcConnection.createArrayOf(typeName, elements);
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    resolveLobs(typeName, attributes, new StructTypeResolver(this));
    return jdbcConnection.createStruct(typeName, attributes);
  }

  public DatabaseMetaData getMetaData() throws SQLException {
    return jdbcConnection.getMetaData();
  }

  private ResultSet getAttributes(String typeName) throws SQLException {
    return getMetaData().getAttributes(jdbcConnection.getCatalog(), null, typeName, null);
  }

  protected void resolveLobs(String typeName, Object[] attributes, StructAndArrayTypeResolver typeResolver) throws SQLException {
    try {
      Map<Integer, ResolvedDbType> dataTypes = getLobFieldsDataTypeInfo(typeName);

      for (Map.Entry entry : dataTypes.entrySet()) {
        Integer key = (Integer) entry.getKey();
        ResolvedDbType dataType = (ResolvedDbType) entry.getValue();

        typeResolver.resolveLobIn(attributes, key, dataType);
      }
    } catch (SQLException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Unable to resolve lobs: {}. Proceeding with original attributes.", e.getMessage());
      }
    }
  }

  protected Map<Integer, ResolvedDbType> getLobFieldsDataTypeInfo(String typeName) throws SQLException {
    Map<Integer, ResolvedDbType> dataTypes = new HashMap<>();

    try (ResultSet resultSet = getAttributes(typeName)) {
      int index = 0;
      while (resultSet.next()) {
        int dataType = resultSet.getInt(DATA_TYPE_INDEX);
        String dataTypeName = resultSet.getString(ATTR_TYPE_NAME_INDEX);

        if (LOB_TYPES.contains(dataTypeName)) {
          dataTypes.put(index, new ResolvedDbType(dataType, dataTypeName));
        }
        index++;
      }
    }
    return dataTypes;
  }

  public void doResolveLobIn(Object[] attributes, int index, int dataType, String dataTypeName) throws SQLException {
    if (shouldResolveAttributeWithJdbcType(dataType, dataTypeName, BLOB.getDbType())) {
      attributes[index] = createBlob(attributes[index]);
    } else if (shouldResolveAttributeWithJdbcType(dataType, dataTypeName, CLOB.getDbType())) {
      attributes[index] = createClob(attributes[index]);
    }
  }

  private boolean shouldResolveAttributeWithJdbcType(int dbDataType, String dbDataTypeName, DbType jdbcType) {
    if (dbDataType == UNKNOWN_DATA_TYPE) {
      return dbDataTypeName.equals(jdbcType.getName());
    } else {
      return dbDataType == jdbcType.getId();
    }
  }

  public void doResolveLobIn(Object[] attributes, int index, String dataTypeName) throws SQLException {
    doResolveLobIn(attributes, index, UNKNOWN_DATA_TYPE, dataTypeName);
  }

  private Blob createBlob(Object attribute) throws SQLException {
    Blob blob = jdbcConnection.createBlob();
    if (attribute instanceof byte[]) {
      blob.setBytes(1, (byte[]) attribute);
    } else if (attribute instanceof InputStream) {
      blob.setBytes(1, toByteArray((InputStream) attribute));
    } else if (attribute instanceof String) {
      blob.setBytes(1, ((String) attribute).getBytes());
    } else {
      throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(),
                                                attribute.getClass()));
    }
    return blob;
  }

  private Clob createClob(Object attribute) throws SQLException {
    Clob clob = jdbcConnection.createClob();
    if (attribute instanceof String) {
      clob.setString(1, (String) attribute);
    } else if (attribute instanceof InputStream) {
      clob.setString(1, IOUtils.toString((InputStream) attribute));
    } else {
      throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(),
                                                attribute.getClass()));
    }
    return clob;
  }

}
