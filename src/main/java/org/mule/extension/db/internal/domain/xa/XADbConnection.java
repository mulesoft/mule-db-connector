/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.xa;

import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoreProcedureSchema;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XADbConnection implements DbConnection, XATransactionalConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(XADbConnection.class);

  private final DbConnection connection;
  private final XAConnection xaConnection;
  private boolean isXaResourceProvided = false;

  public XADbConnection(DbConnection connection, XAConnection xaConnection) {
    this.connection = connection;
    this.xaConnection = xaConnection;
  }

  @Override
  public XAResource getXAResource() {
    try {
      XAResource xaResource = xaConnection.getXAResource();
      isXaResourceProvided = true;
      return xaResource;
    } catch (SQLException e) {
      throw new MuleRuntimeException(new TransactionException(createStaticMessage("Could not obtain XA Resource"), e));
    }
  }

  @Override
  public void close() {
    try {
      connection.release();
      Connection jdbcConnection = connection.getJdbcConnection();
      if (!jdbcConnection.isClosed()) {
        jdbcConnection.close();
      }
    } catch (SQLException | ConnectionClosingException e) {
      LOGGER.info("Exception while explicitly closing the xaConnection (some providers require this). "
          + "The exception will be ignored and only logged: " + e.getMessage(), e);
    } finally {
      isXaResourceProvided = false;
    }
  }

  @Override
  public void begin() throws TransactionException {
    connection.begin();
  }

  @Override
  public void commit() throws TransactionException {
    connection.commit();
  }

  @Override
  public void rollback() throws TransactionException {
    connection.rollback();
  }

  @Override
  public StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler) {
    return connection.getStatementResultIteratorFactory(resultSetHandler);
  }

  @Override
  public List<DbType> getVendorDataTypes() {
    return connection.getVendorDataTypes();
  }

  @Override
  public List<DbType> getCustomDataTypes() {
    return connection.getCustomDataTypes();
  }

  @Override
  public Connection getJdbcConnection() {
    return connection.getJdbcConnection();
  }

  @Override
  public void release() {
    connection.release();
  }

  @Override
  public void beginStreaming() {
    connection.beginStreaming();
  }

  @Override
  public boolean isStreaming() {
    return connection.isStreaming();
  }

  @Override
  public void endStreaming() {
    connection.endStreaming();
  }

  @Override
  public boolean isTransactionActive() {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction == null) {
      return false;
    } else if (transaction.isXA() && isXaResourceProvided) {
      return TransactionCoordination.isTransactionActive();
    } else {
      return connection.isTransactionActive();
    }
  }

  @Override
  public boolean supportsContentStreaming() {
    return connection.supportsContentStreaming();
  }

  @Override
  public Optional<String> getProcedureCatalog(QueryTemplate queryTemplate) throws SQLException {
    return Optional.of(this.getJdbcConnection().getCatalog());
  }

  /**
   * Get procedure schema
   */
  public Optional<String> getProcedureSchema(QueryTemplate queryTemplate) throws SQLException {
    return getStoreProcedureSchema(queryTemplate.getSqlText());
  }
}
