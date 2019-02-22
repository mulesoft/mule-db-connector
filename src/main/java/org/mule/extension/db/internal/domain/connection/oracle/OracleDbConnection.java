/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.ofNullable;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;
import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.connection.type.resolver.CollectionTypeResolver;
import org.mule.extension.db.internal.domain.connection.type.resolver.TypeResolver;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.oracle.OracleXmlType;

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private static final String ATTR_TYPE_NAME_PARAM = "ATTR_TYPE_NAME";

  private static final String ATTR_NO_PARAM = "ATTR_NO";

  private static final String QUERY_TYPE_ATTRS =
      "SELECT ATTR_NO, ATTR_TYPE_NAME FROM ALL_TYPE_ATTRS WHERE TYPE_NAME = ? AND ATTR_TYPE_NAME IN ('CLOB', 'BLOB')";

  public static final String QUERY_OWNER_CONDITION = " AND OWNER = ?";

  private Method createArrayMethod;

  OracleDbConnection(Connection jdbcConnection, List<DbType> customDataTypes) {
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

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return super.createStruct(typeName, attributes);
  }

  @Override
  public Array createArrayOf(String typeName, Object[] values) throws SQLException {
    if (getCreateArrayMethod() == null) {
      return super.createArrayOf(typeName, values);
    } else {
      try {
        resolveLobType(typeName, values, new CollectionTypeResolver(this));
        return (Array) getCreateArrayMethod().invoke(getJdbcConnection(), typeName, values);
      } catch (Exception e) {
        throw new SQLException("Error creating ARRAY", e);
      }
    }
  }

  private Method getCreateArrayMethod() {
    if (createArrayMethod == null) {
      try {
        createArrayMethod = getJdbcConnection().getClass().getMethod("createARRAY", String.class, Object.class);
        createArrayMethod.setAccessible(true);
      } catch (NoSuchMethodException e) {
        // Ignore, will use the standard method
      }
    }
    return createArrayMethod;
  }

  private void resolveLobType(String typeName, Object[] attributes, TypeResolver typeResolver) throws SQLException {
    Map<Integer, ResolvedDbType> dataTypes = getLobFieldsDataTypeInfo(typeResolver.resolveType(typeName));

    for (Map.Entry entry : dataTypes.entrySet()) {
      Integer index = (Integer) entry.getKey();
      ResolvedDbType dataType = (ResolvedDbType) entry.getValue();
      // In Oracle we do not have the data type for structs or arrays, as the
      // the driver does not provide the getAttributes functionality
      // in their DatabaseMetaData.
      // It has to be taken into account that the data type depends on JDBC, so the
      // driver is the unit responsible for the mapping and we do not have that information
      // in the DB catalog. We resolve the lobs depending on the name only.
      typeResolver.resolveLobs(attributes, index - 1, dataType.getName());
    }
  }

  @Override
  protected Map<Integer, ResolvedDbType> getLobFieldsDataTypeInfo(String typeName) throws SQLException {
    Map<Integer, ResolvedDbType> dataTypes = new HashMap<>();

    String owner = getOwnerFrom(typeName);
    String type = getTypeSimpleName(typeName);

    String query = QUERY_TYPE_ATTRS + (owner != null ? QUERY_OWNER_CONDITION : "");

    try (PreparedStatement ps = this.prepareStatement(query)) {
      ps.setString(1, type);
      if (owner != null) {
        ps.setString(2, owner);
      }

      try (ResultSet resultSet = ps.executeQuery()) {
        while (resultSet.next()) {
          ResolvedDbType resolvedDbType = new ResolvedDbType(UNKNOWN_DATA_TYPE, resultSet.getString(ATTR_TYPE_NAME_PARAM));
          dataTypes.put(resultSet.getInt(ATTR_NO_PARAM), resolvedDbType);
        }
      }
    }
    return dataTypes;
  }

}
