/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.param;

import static java.lang.String.format;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection.TABLE_TYPE_NAME;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoreProcedureSchema;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoredProcedureName;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoredProcedurePackage;

import org.mule.extension.db.api.param.ParameterType;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbTypeException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves parameter types for stored procedure queries
 */
public class StoredProcedureParamTypeResolver implements ParamTypeResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredProcedureParamTypeResolver.class);

  private static final int PROCEDURE_SCHEM_COLUMN_INDEX = 2;
  private static final int PROCEDURE_NAME = 3;
  private static final int PARAM_NAME_COLUMN_INDEX = 4;
  private static final int TYPE_ID_COLUMN_INDEX = 6;
  private static final int TYPE_NAME_COLUMN_INDEX = 7;
  private static final int COLUMN_TYPE_INDEX = 5;

  private static final short PROCEDURE_COLUMN_RETURN_COLUMN_TYPE = 5;

  private final DbTypeManager dbTypeManager;

  public StoredProcedureParamTypeResolver(DbTypeManager dbTypeManager) {
    this.dbTypeManager = dbTypeManager;
  }

  @Override
  public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate, List<ParameterType> types)
      throws SQLException {
    ResultSet procedureColumns = null;
    DatabaseMetaData dbMetaData = connection.getJdbcConnection().getMetaData();

    String storedProcedureName = getStoredProcedureName(queryTemplate.getSqlText());
    String storedProcedureSchemaName = getStoreProcedureSchema(queryTemplate.getSqlText()).orElse(null);
    String storedProcedurePackage = getStoredProcedurePackage(queryTemplate.getSqlText()).orElse(null);

    if (dbMetaData.storesUpperCaseIdentifiers()) {
      storedProcedureName = storedProcedureName.toUpperCase();

      if (storedProcedureSchemaName != null) {
        storedProcedureSchemaName = storedProcedureSchemaName.toUpperCase();
      }

      if (storedProcedurePackage != null) {
        storedProcedurePackage = storedProcedurePackage.toUpperCase();
      }
    }

    try {
      if (connection instanceof OracleDbConnection && storedProcedurePackage != null) {
        // Since oracle does not have multiples catalog but it has packages the following is the recommend way to
        // retrieve a stored procedure description of a stored procedure within a package
        procedureColumns =
            dbMetaData.getProcedureColumns(storedProcedurePackage, storedProcedureSchemaName, storedProcedureName, "%");
      } else {
        procedureColumns = dbMetaData.getProcedureColumns(connection.getJdbcConnection().getCatalog(), storedProcedureSchemaName,
                                                          storedProcedureName, "%");
      }

      Map<Integer, DbType> paramTypes = getStoredProcedureParamTypes(connection, storedProcedureName, procedureColumns);

      // If still unable to resolve, remove all catalog and schema filters and use only stored procedure name and column
      // pattern.
      if (!getMissingParameters(queryTemplate, paramTypes).isEmpty()) {
        LOGGER
            .debug("Failed to get procedure types with schema {}, package {} and procedure {}. Removing all catalog and schema filters.",
                   storedProcedureSchemaName, storedProcedurePackage, storedProcedureName);

        // In some cases invoke the following method can take a long time to return. For example with Oracle XE 11g
        // using ojdbc7.
        // Also, if there is more than one stored procedure on the DB you may not get the correct stored procedure
        // description. This can happen with Oracle, where you can have stored procedures within packages and also
        // overloaded procedures.
        procedureColumns =
            dbMetaData.getProcedureColumns(null, null, storedProcedureName, "%");

        paramTypes = getStoredProcedureParamTypes(connection, storedProcedureName, procedureColumns);
      }

      List<String> missingParameters = getMissingParameters(queryTemplate, paramTypes);
      if (!missingParameters.isEmpty()) {
        throw new SQLException(format("Could not find query parameters %s.", String.join(",", missingParameters)));
      } else {
        return paramTypes;
      }

    } finally {
      if (procedureColumns != null) {
        procedureColumns.close();
      }
    }

  }

  private Map<Integer, DbType> getStoredProcedureParamTypes(DbConnection connection, String storedProcedureName,
                                                            ResultSet procedureColumns)
      throws SQLException {
    Map<Integer, DbType> paramTypes = new HashMap<>();

    int position = 1;

    while (procedureColumns.next()) {
      if (procedureColumns.getShort(COLUMN_TYPE_INDEX) == PROCEDURE_COLUMN_RETURN_COLUMN_TYPE) {
        continue;
      }

      int typeId = procedureColumns.getInt(TYPE_ID_COLUMN_INDEX);
      String typeName = procedureColumns.getString(TYPE_NAME_COLUMN_INDEX);

      if (LOGGER.isDebugEnabled()) {
        String name = procedureColumns.getString(PARAM_NAME_COLUMN_INDEX);
        LOGGER.debug("Resolved parameter type: Store procedure: {} Name: {} Index: {} Type ID: {} Type Name: {}",
                     storedProcedureName, name, position, typeId, typeName);
      }

      DbType dbType = null;
      try {
        // TODO - MULE-15241 : Fix how DB Connector chooses ResolvedTypes
        if (TABLE_TYPE_NAME.equals(typeName)) {
          String procedureName = procedureColumns.getString(PROCEDURE_NAME);
          String argumentName = procedureColumns.getString(PARAM_NAME_COLUMN_INDEX);
          String owner = procedureColumns.getString(PROCEDURE_SCHEM_COLUMN_INDEX);

          Optional<String> columnType = connection.getProcedureColumnType(procedureName, argumentName, owner);
          dbType = columnType.map(type -> (DbType) new ArrayResolvedDbType(Types.ARRAY, type)).orElse(null);
        }

        if (dbType == null) {
          dbType = dbTypeManager.lookup(connection, typeId, typeName);
        }

      } catch (UnknownDbTypeException e) {
        // Type was not found in the type manager, but the DB knows about it
        dbType = new ResolvedDbType(typeId, typeName);
      }
      paramTypes.put(position, dbType);
      position++;
    }

    return paramTypes;
  }

  private List<String> getMissingParameters(QueryTemplate queryTemplate, Map<Integer, DbType> paramTypes) {
    return queryTemplate.getParams().stream()
        .filter(queryParam -> !paramTypes.containsKey(queryParam.getIndex()))
        .map(QueryParam::getName)
        .collect(Collectors.toList());
  }
}
