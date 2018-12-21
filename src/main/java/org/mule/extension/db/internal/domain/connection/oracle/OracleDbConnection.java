/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.ofNullable;

import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.oracle.OracleXmlType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link DefaultDbConnection} implementation for Oracle databases
 *
 * @since 1.0
 */
public class OracleDbConnection extends DefaultDbConnection {

  public static final String TABLE_TYPE_NAME = "TABLE";

  private static final int CURSOR_TYPE_ID = -10;
  private static final String CURSOR_TYPE_NAME = "CURSOR";

  public OracleDbConnection(Connection jdbcConnection, List<DbType> customDataTypes) {
    super(jdbcConnection, customDataTypes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    List<DbType> dbTypes = new ArrayList<>();
    dbTypes.add(new ResolvedDbType(CURSOR_TYPE_ID, CURSOR_TYPE_NAME));
    dbTypes.add(new OracleXmlType());

    return dbTypes;
  }

  @Override
  public Optional<String> getProcedureColumnType(String procedureName, String columnName, String owner) throws SQLException {
    try (PreparedStatement statement = getJdbcConnection().prepareStatement("SELECT TYPE_NAME FROM SYS.ALL_ARGUMENTS \n" +
        "WHERE OWNER= ? \n" +
        "AND OBJECT_NAME= ?\n" +
        "AND ARGUMENT_NAME = ?\n" +
        "ORDER BY SEQUENCE")) {

      statement.setString(1, owner);
      statement.setString(2, procedureName);
      statement.setString(3, columnName);

      ResultSet resultSet = statement.executeQuery();

      Optional<String> columnType = Optional.empty();

      if (resultSet.next()) {
        columnType = ofNullable(resultSet.getString(1));
      }
      return columnType;
    }
  }

  @Override
  public Set<String> getTables() throws SQLException {
    Statement statement = getJdbcConnection().createStatement();
    statement.execute("SELECT table_name FROM user_tables");
    ResultSet resultSet = statement.getResultSet();

    Set<String> tables = new HashSet<>();
    while (resultSet.next()) {
      tables.add(resultSet.getString(1));
    }
    return tables;
  }
}
