/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import static java.sql.Types.ARRAY;
import static java.sql.Types.STRUCT;
import static org.slf4j.LoggerFactory.getLogger;
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

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * Resolves parameter types for standard queries
 */
public class QueryParamTypeResolver implements ParamTypeResolver {

  private static final Logger LOGGER = getLogger(QueryParamTypeResolver.class);

  private final DbTypeManager dbTypeManager;

  public QueryParamTypeResolver(DbTypeManager dbTypeManager) {
    this.dbTypeManager = dbTypeManager;
  }

  @Override
  public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate, List<ParameterType> types)
      throws SQLException {
    Map<Integer, DbType> paramTypes = new HashMap<>();
    PreparedStatement statement = null;
    try {
      statement = connection.getJdbcConnection().prepareStatement(queryTemplate.getSqlText());
      ParameterMetaData parameterMetaData = statement.getParameterMetaData();

      for (QueryParam queryParam : queryTemplate.getParams()) {
        int parameterTypeId = parameterMetaData.getParameterType(queryParam.getIndex());
        Optional<ParameterType> type = types.stream().filter(p -> p.getKey().equals(queryParam.getName())).findAny();
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
