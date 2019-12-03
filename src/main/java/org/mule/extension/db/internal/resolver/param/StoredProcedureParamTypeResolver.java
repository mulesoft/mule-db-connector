/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.param;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.sql.Types.ARRAY;
import static java.sql.Types.STRUCT;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection.TABLE_TYPE_NAME;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoreProcedureOwner;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoredProcedureName;
import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoredProcedureParentOwner;

import org.mule.extension.db.api.param.ParameterType;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StructDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbTypeException;

import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
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

  private static final String RETRIEVE_PARAM_TYPES = "mule.db.connector.retrieve.param.types";

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
  public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate,
                                                List<ParameterType> parameterTypesConfigured)
      throws SQLException {

    if (!shouldRetrieveParamTypesUsingUsingDBMetadata()) {
      return getParameterTypesFromConfiguration(connection, queryTemplate, parameterTypesConfigured);
    } else {
      return getStoredProcedureParamTypesUsingMetadata(connection, queryTemplate);
    }
  }

  private Map<Integer, DbType> getStoredProcedureParamTypesUsingMetadata(DbConnection connection, QueryTemplate queryTemplate)
      throws SQLException {
    DatabaseMetaData dbMetaData = connection.getJdbcConnection().getMetaData();

    String storedProcedureName = getStoredProcedureName(queryTemplate.getSqlText());
    String storedProcedureOwner = getStoreProcedureOwner(queryTemplate.getSqlText()).orElse(null);
    String storedProcedureParentOwner = getStoredProcedureParentOwner(queryTemplate.getSqlText()).orElse(null);

    if (dbMetaData.storesUpperCaseIdentifiers()) {
      storedProcedureName = storedProcedureName.toUpperCase();

      if (storedProcedureOwner != null) {
        storedProcedureOwner = storedProcedureOwner.toUpperCase();
      }

      if (storedProcedureParentOwner != null) {
        storedProcedureParentOwner = storedProcedureParentOwner.toUpperCase();
      }
    }

    try (ResultSet procedureColumns =
        connection.getProcedureColumns(storedProcedureName, storedProcedureOwner, storedProcedureParentOwner,
                                       connection.getJdbcConnection().getCatalog())) {

      Map<Integer, DbType> paramTypes = getStoredProcedureParamTypes(connection, storedProcedureName, procedureColumns);

      List<String> missingParameters = getMissingParameters(queryTemplate, paramTypes);
      if (!missingParameters.isEmpty()) {
        throw new SQLException(format("Could not find query parameters %s.", join(",", missingParameters)));
      }

      return paramTypes;
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
      String parameterName = procedureColumns.getString(PARAM_NAME_COLUMN_INDEX);

      LOGGER.debug("Resolved parameter type: Store procedure: {}, Name: {}, Index: {}, Type ID: {}, Type Name: {}",
                   storedProcedureName, parameterName, position, typeId, typeName);

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

  private boolean shouldRetrieveParamTypesUsingUsingDBMetadata() {
    return Boolean.valueOf(getProperty(RETRIEVE_PARAM_TYPES, "true"));
  }

  private Map<Integer, DbType> getParameterTypesFromConfiguration(DbConnection connection, QueryTemplate queryTemplate,
                                                                  List<ParameterType> parameterTypesConfigured)
      throws SQLException {
    Map<Integer, DbType> paramTypes = new HashMap<>();
    PreparedStatement statement = null;
    try {
      statement = connection.getJdbcConnection().prepareCall(queryTemplate.getSqlText());
      ParameterMetaData parameterMetaData = statement.getParameterMetaData();

      for (QueryParam queryParam : queryTemplate.getParams()) {
        int parameterTypeId = parameterMetaData.getParameterType(queryParam.getIndex());
        Optional<ParameterType> type =
            parameterTypesConfigured.stream().filter(p -> p.getKey().equals(queryParam.getName())).findAny();
        String parameterTypeName =
            type.isPresent() ? type.get().getDbType().getName() : parameterMetaData.getParameterTypeName(queryParam.getIndex());
        DbType dbType;

        if (parameterTypeName == null) {
          // Use unknown data type
          dbType = UnknownDbType.getInstance();
        } else if (type.isPresent() && !(type.get().getDbType() instanceof DynamicDbType)) {
          dbType = type.get().getDbType();
        } else {
          dbType = ParameterTypeResolverUtils.resolveDbType(dbTypeManager, connection, parameterTypeId, parameterTypeName);
        }

        paramTypes.put(queryParam.getIndex(), dbType);
      }
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOGGER.warn("Could not close statement", e);
        }
      }
    }

    return paramTypes;
  }
}
