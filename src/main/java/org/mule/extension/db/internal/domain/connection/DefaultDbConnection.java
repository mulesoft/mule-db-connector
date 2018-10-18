/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.GenericStatementResultIteratorFactory;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;
import org.mule.runtime.api.tx.TransactionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultDbConnection implements DbConnection {

  private final Connection jdbcConnection;
  private final List<DbType> customDataTypes;
  private AtomicInteger streamsCount = new AtomicInteger(0);
  private boolean isTransactionActive = false;

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
}
