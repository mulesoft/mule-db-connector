/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata.getNotResolvedDbConnectionTracingMetadata;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import org.mule.db.commons.internal.domain.connection.DefaultDbConnection;
import org.mule.db.commons.internal.domain.connection.type.resolver.ArrayTypeResolver;
import org.mule.db.commons.internal.domain.connection.type.resolver.StructAndArrayTypeResolver;
import org.mule.db.commons.internal.domain.type.ArrayResolvedDbType;
import org.mule.db.commons.internal.domain.type.DbType;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleOlderXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleOpaqueXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleSQLXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleSYSXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleXMLType;
import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DefaultDbConnection} implementation for Oracle databases
 *
 * @since 1.0
 */
public class OracleDbConnection extends DefaultDbConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleDbConnection.class);

  public static final String TABLE_TYPE_NAME = "TABLE";

  private static final int CURSOR_TYPE_ID = OracleTypes.CURSOR;
  private static final String CURSOR_TYPE_NAME = "CURSOR";

  public static final String ATTR_TYPE_NAME_PARAM = "ATTR_TYPE_NAME";

  private static final String ATTR_NO_PARAM = "ATTR_NO";

  public static final String QUERY_TYPE_ATTRS =
      "SELECT ATTR_NO, ATTR_TYPE_NAME FROM ALL_TYPE_ATTRS WHERE TYPE_NAME = ? AND ATTR_TYPE_NAME IN ('CLOB', 'BLOB')";

  private static final String QUERY_OWNER_CONDITION = " AND OWNER = ?";

  private static final int PROCEDURE_SCHEM_COLUMN_INDEX = 2;
  private static final int PROCEDURE_NAME = 3;
  private static final int PARAM_NAME_COLUMN_INDEX = 4;

  private final Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache;

  public OracleDbConnection(Connection jdbcConnection, List<DbType> customDataTypes,
                            Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache, long cacheQueryTemplateSize) {
    this(jdbcConnection, customDataTypes, resolvedDbTypesCache, cacheQueryTemplateSize,
         getNotResolvedDbConnectionTracingMetadata());
  }

  public OracleDbConnection(Connection jdbcConnection, List<DbType> customDataTypes,
                            Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache, long cacheQueryTemplateSize,
                            DbConnectionTracingMetadata dbConnectionTracingMetadata) {
    super(jdbcConnection, customDataTypes, cacheQueryTemplateSize, dbConnectionTracingMetadata);
    this.resolvedDbTypesCache = resolvedDbTypesCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getVendorDataTypes() {
    List<DbType> dbTypes = new ArrayList<>();
    dbTypes.add(new ResolvedDbType(CURSOR_TYPE_ID, CURSOR_TYPE_NAME));
    dbTypes.add(new OracleOpaqueXMLType());
    dbTypes.add(new OracleSQLXMLType());
    dbTypes.add(new OracleXMLType());
    dbTypes.add(new OracleOlderXMLType());
    dbTypes.add(new OracleSYSXMLType());

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

      Optional<String> columnType = empty();

      if (resultSet.next()) {
        columnType = ofNullable(resultSet.getString(1));
      }
      return columnType;
    }
  }

  @Override
  public Set<String> getTables() throws SQLException {
    try (Statement statement = getJdbcConnection().createStatement()) {
      statement.execute("SELECT table_name FROM user_tables");
      ResultSet resultSet = statement.getResultSet();

      Set<String> tables = new HashSet<>();
      while (resultSet.next()) {
        tables.add(resultSet.getString(1));
      }

      return tables;
    }
  }

  @Override
  public Array createArray(String typeName, Object[] values) throws SQLException {
    // Not using isWrapperFor() because this is expected to always succeed,
    // since we already know that the Database is Oracle.
    OracleConnection oracleConnection = getJdbcConnection().unwrap(OracleConnection.class);

    if (oracleConnection == null) {
      throw new RuntimeException("Can't reach Oracle extensions. Connection class was: "
          + getJdbcConnection().getClass().getName());
    }

    resolveLobs(typeName, values, new ArrayTypeResolver(this));

    values = Arrays.stream(values).map(e -> {
      if (e instanceof Collection) {
        return ((Collection<?>) e).toArray();
      } else {
        return e;
      }
    }).toArray();

    return oracleConnection.createARRAY(typeName, values);
  }

  @Override
  protected void resolveLobs(String typeName, Object[] attributes, StructAndArrayTypeResolver typeResolver) throws SQLException {
    Map<Integer, ResolvedDbType> dataTypes = getLobFieldsDataTypeInfo(typeResolver.resolveType(typeName));

    for (Map.Entry<Integer, ResolvedDbType> entry : dataTypes.entrySet()) {
      int index = entry.getKey();
      ResolvedDbType dataType = entry.getValue();
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

    if (this.resolvedDbTypesCache.containsKey(typeName)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Returning chached LobFieldsDataTypeInfo");
      }
      return resolvedDbTypesCache.get(typeName);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Obtaining LobFieldsDataTypeInfo");
    }
    synchronized (resolvedDbTypesCache) {
      if (this.resolvedDbTypesCache.containsKey(typeName)) {
        return resolvedDbTypesCache.get(typeName);
      }

      Map<Integer, ResolvedDbType> dataTypes = new HashMap<>();

      Optional<String> owner = getOwnerFrom(typeName);
      String type = getTypeSimpleName(typeName);

      String query = QUERY_TYPE_ATTRS + (owner.isPresent() ? QUERY_OWNER_CONDITION : "");

      try (PreparedStatement ps = this.prepareStatement(query)) {
        ps.setString(1, type);
        if (owner.isPresent()) {
          ps.setString(2, owner.get());
        }

        try (ResultSet resultSet = ps.executeQuery()) {
          while (resultSet.next()) {
            ResolvedDbType resolvedDbType = new ResolvedDbType(UNKNOWN_DATA_TYPE, resultSet.getString(ATTR_TYPE_NAME_PARAM));
            dataTypes.put(resultSet.getInt(ATTR_NO_PARAM), resolvedDbType);
          }
        }
      }
      resolvedDbTypesCache.put(typeName, dataTypes);
      return dataTypes;
    }
  }

  @Override
  public ResultSet getProcedureColumns(String storedProcedureName, String storedProcedureOwner, String storedProcedureParentOwner,
                                       String catalogName)
      throws SQLException {
    /*
     * Since Oracle does not have multiples catalog but it has packages, the recommended way to get a procedure description
     * of a procedure within a package is to use the argument named catalog of DatabaseMetaData#getProcedureColumns to
     * specify the package name.
     *
     * Under certain circumstances calling DatabaseMetaData#getProcedureColumns not specifying schema, package, and
     * catalog might take too long to resolve. For this reason we try to call this method avoiding any null value.
     *
     * When the owner is defined but the parent owner it is not, we cannot know whether a stored procedure owner is a
     * schema or a package. In this case we try first considering the owner as the package and using the schema from the
     * connection. If that fails to find the procedure, we considered the owner as the schema.
     *
     * If we cannot find the stored procedure under the specified schema and/or package we try specifying only the stored
     * procedure name.
     */
    DatabaseMetaData dbMetaData = getJdbcConnection().getMetaData();

    String connectionSchema;
    try {
      connectionSchema = getJdbcConnection().getSchema();
    } catch (Throwable t) {
      LOGGER
          .warn("You are using a not supported jdbc driver version. Consider to upgrade to a new version to guarantee a better performance.");
      connectionSchema = null;
    }

    ResultSet procedureColumns;
    if (!isBlank(storedProcedureParentOwner) && !isBlank(storedProcedureOwner)) {
      procedureColumns =
          dbMetaData.getProcedureColumns(storedProcedureParentOwner, storedProcedureOwner, storedProcedureName, "%");
    } else if (!isBlank(storedProcedureOwner)) {
      procedureColumns = dbMetaData.getProcedureColumns(storedProcedureOwner, connectionSchema, storedProcedureName, "%");
      if (!procedureColumns.isBeforeFirst()) {
        procedureColumns.close();
        procedureColumns = dbMetaData.getProcedureColumns(catalogName == null ? "" : catalogName, storedProcedureOwner,
                                                          storedProcedureName, "%");
      }
    } else {
      procedureColumns = dbMetaData.getProcedureColumns(catalogName, connectionSchema, storedProcedureName, "%");
    }

    if (!procedureColumns.isBeforeFirst()) {
      LOGGER
          .debug("Failed to get procedure types with schema {}, package {} and procedure {}. Removing all catalog and schema filters.",
                 storedProcedureOwner, storedProcedureParentOwner, storedProcedureName);
      procedureColumns = dbMetaData.getProcedureColumns(null, null, storedProcedureName, "%");
    }

    return procedureColumns;
  }

  @Override
  public Optional<DbType> getDbTypeByVendor(String typeName, ResultSet procedureColumns) throws SQLException {
    if (TABLE_TYPE_NAME.equals(typeName)) {
      String procedureName = procedureColumns.getString(PROCEDURE_NAME);
      String argumentName = procedureColumns.getString(PARAM_NAME_COLUMN_INDEX);
      String owner = procedureColumns.getString(PROCEDURE_SCHEM_COLUMN_INDEX);

      Optional<String> columnType = getProcedureColumnType(procedureName, argumentName, owner);
      return columnType.map(type -> new ArrayResolvedDbType(Types.ARRAY, type));
    }

    return empty();
  }

}
