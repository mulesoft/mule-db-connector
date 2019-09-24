/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.statement.StatementResultIterator;
import org.mule.extension.db.internal.result.statement.StatementResultIteratorFactory;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Wraps a {@link Connection} adding connector's specific functionality
 */
public interface DbConnection extends TransactionalConnection {

  /**
   * Returns the {@link StatementResultIteratorFactory} used to create the {@link StatementResultIterator} for this connection.
   *
   * @param resultSetHandler used to process resultSets created from this connection
   * @return the {@link StatementResultIterator} for this connection.
   */
  StatementResultIteratorFactory getStatementResultIteratorFactory(ResultSetHandler resultSetHandler);

  /**
   * @return A list of customer defined {@link DbType}s
   */
  List<DbType> getCustomDataTypes();

  /**
   * @return A list of {@link DbType}s which are specific to the Database vendor
   */
  List<DbType> getVendorDataTypes();

  /**
   * @return The underlying JDBC connection
   */
  Connection getJdbcConnection();

  /**
   * Closes the underlying JDBC connection, provided that {@link #isStreaming()} is {@code false}
   */
  void release();

  /**
   * Starts streaming. Invoke this method when a streaming resultset generated with {@code this} connection is about to be
   * iterated
   */
  void beginStreaming();

  /**
   * @return whether {@link #beginStreaming()} has been invoked on {@code this} instance but {@link #endStreaming()} has not
   */
  boolean isStreaming();

  /**
   * Marks that the streaming is over
   */
  void endStreaming();

  /**
   * @return a boolean indicating if the current connection is being part of a Transaction.
   */
  boolean isTransactionActive();

  /**
   * @return a boolean indicating if the current connection supports to stream the fetched content
   * @since 1.3.4
   */
  boolean supportsContentStreaming();

  /**
   * Creates an {@link Array} of the given {@code typeName} with the given {@code values}
   *
   * @param typeName The Array type name
   * @param values   The values to convert to an {@link Array}
   * @return The created {@link Array}
   *
   * @throws SQLException if an error occurs trying to create the array.
   * @since 1.5.0
   */
  default Array createArrayOf(String typeName, Object[] values) throws SQLException {
    return getJdbcConnection().createArrayOf(typeName, values);
  }

  /**
   * Creates an {@link Array} of the given {@code typeName} with the given {@code values}
   *
   * @param typeName The Array type name
   * @param value   The values to convert to an {@link Array}
   * @return The created {@link Array}
   *
   * @throws SQLException if an error occurs trying to create the array.
   * @since 1.5.0
   */
  default Array createArrayOf(String typeName, Object value) throws SQLException {
    Object[] values;
    if (value instanceof Object[]) {
      values = (Object[]) value;
    } else if (value instanceof Collection) {
      values = ((Collection) value).toArray();
    } else {
      values = new Object[] {value};
    }
    return createArrayOf(typeName, values);
  }

  /**
   * Creates an {@link Struct} of the given {@code typeName} with the given {@code values}
   *
   * @param typeName The Struct type name
   * @param values   The values to convert to an {@link Struct}
   * @return The created {@link Struct}
   *
   * @throws SQLException if an error occurs trying to create the struct.
   * @since 1.5.2
   */
  default Struct createStruct(String typeName, Object[] values) throws SQLException {
    return getJdbcConnection().createStruct(typeName, values);
  }


  /**
   * Retrieves a description of the given stored procedure parameter and result columns. For more information
   * {@see DatabaseMetaData#getProcedureColumns}
   * </p>
   * Some Databases assign a different meaning to each parameter. You should override this method when needed.
   *
   * @param storedProcedureName the stored procedure name
   * @param storedProcedureOwner the owner of the stored procedure
   * @param storedProcedureParentOwner the owner of the owner of the stored procedure
   * @param catalogName the catalog name where the stored procedure is defined
   * @return <code>ResultSet</code> - each row describes a stored procedure parameter or column
   * @throws SQLException if a database access error occurs
   */
  default ResultSet getProcedureColumns(String storedProcedureName, String storedProcedureOwner,
                                        String storedProcedureParentOwner, String catalogName)
      throws SQLException {
    DatabaseMetaData dbMetaData = getJdbcConnection().getMetaData();
    if (!isBlank(storedProcedureOwner)) {
      return dbMetaData.getProcedureColumns(catalogName, storedProcedureOwner, storedProcedureName, "%");
    } else {
      return dbMetaData.getProcedureColumns(catalogName, null, storedProcedureName, "%");
    }
  }

  /**
   * Returns the type name of a Stored Procedure Column
   *
   * @param procedureName The Stored Procedure name
   * @param columnName    Name of the column name
   * @param owner         The owner of the stored procedure
   * @return An Optional String with the Column type Name
   * @throws SQLException if an error occurs trying to obtain the column name
   * @since 1.5.0
   */
  default Optional<String> getProcedureColumnType(String procedureName, String columnName, String owner) throws SQLException {
    return empty();
  }

  /**
   * Returns all the available tables of the current Database.
   * @return A Set with all the table names
   *
   * @throws SQLException if an error occurs trying to obtain the table names
   *
   * @since 1.5.0
   */
  default Set<String> getTables() throws SQLException {
    Set<String> tableNames = new HashSet<>();

    ResultSet tables = getJdbcConnection()
        .getMetaData()
        .getTables(null, null, "%", null);

    while (tables.next()) {
      tableNames.add(tables.getString(3));
    }

    return tableNames;
  }
}
