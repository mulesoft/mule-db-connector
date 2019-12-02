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
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbType;
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

  public static final String RETRIEVE_PARAM_TYPES = "mule.db.connector.retrieve.param.types";

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

    Map<Integer, DbType> parameters;
    if (!shouldRetrieveParamTypesUsingUsingDBMetadata()) {
      parameters = getParameterTypesFromConfiguration(queryTemplate, parameterTypesConfigured);
      List<String> missingParameters = getMissingParameters(queryTemplate, parameters);
      if (!missingParameters.isEmpty()) {
        LOGGER
            .warn("Failed to resolve Stored Procedure parameters types. Be sure all parameters are configured. Using DB metadata to retrieve Stored Procedure parameters types");
        parameters = getStoredProcedureParamTypesUsingMetadata(connection, queryTemplate);
        missingParameters = getMissingParameters(queryTemplate, parameters);
        if (!missingParameters.isEmpty()) {
          throw new SQLException(format("Could not find query parameters %s.", join(",", missingParameters)));
        }
      }
      return parameters;
    } else {
      parameters = getStoredProcedureParamTypesUsingMetadata(connection, queryTemplate);
      List<String> missingParameters = getMissingParameters(queryTemplate, parameters);
      if (!missingParameters.isEmpty()) {
        throw new SQLException(format("Could not find query parameters %s.", join(",", missingParameters)));
      }

      return parameters;
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

      return getStoredProcedureParamTypes(connection, storedProcedureName, procedureColumns);
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

  private Map<Integer, DbType> getParameterTypesFromConfiguration(QueryTemplate queryTemplate,
                                                                  List<ParameterType> parameterTypesConfigured) {
    Map<Integer, DbType> paramTypes = new HashMap<>();

    for (QueryParam queryParam : queryTemplate.getParams()) {

      Optional<ParameterType> type =
          parameterTypesConfigured.stream().filter(p -> p.getKey().equals(queryParam.getName())).findAny();

      if (type.isPresent()) {
        String parameterTypeName = type.get().getDbType().getName();

        DbType dbType;

        if (parameterTypeName == null) {
          // Use unknown data type
          dbType = UnknownDbType.getInstance();
        } else {
          dbType = type.get().getDbType();
        }

        paramTypes.put(queryParam.getIndex(), dbType);
      }
    }

    return paramTypes;
  }

}
