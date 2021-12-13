/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.util;

import org.mule.db.commons.api.param.BulkQueryDefinition;
import org.mule.db.commons.api.param.BulkScript;

import org.mule.db.commons.api.param.JdbcType;
import org.mule.db.commons.api.param.OutputParameter;
import org.mule.db.commons.api.param.ParameterType;
import org.mule.db.commons.api.param.ParameterizedStatementDefinition;
import org.mule.db.commons.api.param.QueryDefinition;
import org.mule.db.commons.api.param.QuerySettings;
import org.mule.db.commons.api.param.StoredProcedureCall;
import org.mule.db.commons.api.param.TransactionIsolation;
import org.mule.db.commons.api.param.TypeClassifier;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.runtime.api.tls.TlsContextFactory;

import java.util.List;

import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Provides Objects translations between the DB Connector Types and the DB Client Types
 */
public class MigrationUtils {

  public static BulkScript mapBulkScript(org.mule.extension.db.api.param.BulkScript bulkScript) {
    if (isNull(bulkScript)) {
      return null;
    }
    return new BulkScript(bulkScript.getSql(), bulkScript.getFile());
  }

  public static QuerySettings mapQuerySettings(org.mule.extension.db.api.param.QuerySettings querySettings) {
    if (isNull(querySettings)) {
      return null;
    }
    return new QuerySettings(querySettings.getQueryTimeout(), querySettings.getQueryTimeoutUnit(), querySettings.getFetchSize(),
                             querySettings.getMaxRows());
  }

  public static BulkQueryDefinition mapBulkQueryDefinition(org.mule.extension.db.api.param.BulkQueryDefinition queryDefinition) {
    if (isNull(queryDefinition)) {
      return null;
    }
    List<ParameterType> paramTypes =
        queryDefinition.getParameterTypes().stream().map(x -> mapParameterType(x)).collect(Collectors.toList());
    return new BulkQueryDefinition(queryDefinition.getSql(), paramTypes, queryDefinition.getQueryTimeout(),
                                   queryDefinition.getQueryTimeoutUnit(), queryDefinition.getFetchSize(),
                                   queryDefinition.getMaxRows());
  }

  public static ParameterType mapParameterType(org.mule.extension.db.api.param.ParameterType parameterType) {
    if (isNull(parameterType)) {
      return null;
    }
    return new ParameterType(parameterType.getKey(), mapTypeClassifier(parameterType.getTypeClassifier()));
  }

  public static TypeClassifier mapTypeClassifier(org.mule.extension.db.api.param.TypeClassifier typeClassifier) {
    if (isNull(typeClassifier)) {
      return null;
    }
    return new TypeClassifier(mapJdbcType(typeClassifier.getJdbcType()), typeClassifier.getCustomType());
  }

  public static JdbcType mapJdbcType(org.mule.extension.db.api.param.JdbcType jdbcType) {
    if (isNull(jdbcType)) {
      return null;
    }
    switch (jdbcType) {
      case BIT:
        return JdbcType.BIT;
      case REF:
        return JdbcType.REF;
      case BLOB:
        return JdbcType.BLOB;
      case CHAR:
        return JdbcType.CHAR;
      case CLOB:
        return JdbcType.CLOB;
      case DATE:
        return JdbcType.DATE;
      case NULL:
        return JdbcType.NULL;
      case REAL:
        return JdbcType.REAL;
      case TIME:
        return JdbcType.TIME;
      case ARRAY:
        return JdbcType.ARRAY;
      case FLOAT:
        return JdbcType.FLOAT;
      case NCHAR:
        return JdbcType.NCHAR;
      case NCLOB:
        return JdbcType.NCLOB;
      case OTHER:
        return JdbcType.OTHER;
      case ROWID:
        return JdbcType.ROWID;
      case BIGINT:
        return JdbcType.BIGINT;
      case BINARY:
        return JdbcType.BINARY;
      case DOUBLE:
        return JdbcType.DOUBLE;
      case SQLXML:
        return JdbcType.SQLXML;
      case STRUCT:
        return JdbcType.STRUCT;
      case BOOLEAN:
        return JdbcType.BOOLEAN;
      case DECIMAL:
        return JdbcType.DECIMAL;
      case INTEGER:
        return JdbcType.INTEGER;
      case NUMERIC:
        return JdbcType.NUMERIC;
      case TINYINT:
        return JdbcType.TINYINT;
      case UNKNOWN:
        return JdbcType.UNKNOWN;
      case VARCHAR:
        return JdbcType.VARCHAR;
      case DATALINK:
        return JdbcType.DATALINK;
      case DISTINCT:
        return JdbcType.DISTINCT;
      case NVARCHAR:
        return JdbcType.NVARCHAR;
      case SMALLINT:
        return JdbcType.SMALLINT;
      case TIMESTAMP:
        return JdbcType.TIMESTAMP;
      case VARBINARY:
        return JdbcType.VARBINARY;
      case JAVA_OBJECT:
        return JdbcType.JAVA_OBJECT;
      case LONGVARCHAR:
        return JdbcType.LONGVARCHAR;
      case LONGNVARCHAR:
        return JdbcType.LONGNVARCHAR;
      case LONGVARBINARY:
        return JdbcType.LONGVARBINARY;
      default:
        throw new RuntimeException("Invalid JDBC Type Translation");
    }
  }

  public static StoredProcedureCall mapStoredProcedureCall(org.mule.extension.db.api.param.StoredProcedureCall spCall) {

    if (isNull(spCall)) {
      return null;
    }

    List<ParameterType> parameterTypes =
        spCall.getParameterTypes().stream().map(x -> mapParameterType(x)).collect(Collectors.toList());
    List<OutputParameter> outputParameterTypes = spCall.getOutputParameters().stream()
        .map(x -> new OutputParameter(x.getKey(), mapTypeClassifier(x.getTypeClassifier()))).collect(Collectors.toList());

    return new StoredProcedureCall(spCall.getSql(), parameterTypes, spCall.getInputParameters(), spCall.getInOutParameters(),
                                   outputParameterTypes, spCall.getQueryTimeout(), spCall.getQueryTimeoutUnit(),
                                   spCall.getFetchSize(), spCall.getMaxRows());

  }

  public static QueryDefinition mapQueryDefinition(org.mule.extension.db.api.param.QueryDefinition queryDefinition) {
    if (isNull(queryDefinition)) {
      return null;
    }
    List<ParameterType> parameterTypes =
        queryDefinition.getParameterTypes().stream().map(x -> mapParameterType(x)).collect(Collectors.toList());

    return new QueryDefinition(queryDefinition.getSql(), parameterTypes, queryDefinition.getInputParameters(),
                               queryDefinition.getQueryTimeout(), queryDefinition.getQueryTimeoutUnit(),
                               queryDefinition.getFetchSize(), queryDefinition.getMaxRows());
  }

  public static DataSourceConfig mapDataSourceConfig(org.mule.extension.db.internal.domain.connection.DataSourceConfig dsConfig) {
    if (isNull(dsConfig)) {
      return null;
    }
    return new DataSourceConfig() {

      @Override
      public String getUrl() {
        return dsConfig.getUrl();
      }

      @Override
      public String getDriverClassName() {
        return dsConfig.getDriverClassName();
      }

      @Override
      public String getPassword() {
        return dsConfig.getPassword();
      }

      @Override
      public String getUser() {
        return dsConfig.getUser();
      }

      @Override
      public TransactionIsolation getTransactionIsolation() {
        switch (dsConfig.getTransactionIsolation()) {
          case NONE:
            return TransactionIsolation.NONE;

          case SERIALIZABLE:
            return TransactionIsolation.SERIALIZABLE;
          case NOT_CONFIGURED:
            return TransactionIsolation.NOT_CONFIGURED;
          case READ_COMMITTED:
            return TransactionIsolation.READ_COMMITTED;
          case READ_UNCOMMITTED:
            return TransactionIsolation.READ_UNCOMMITTED;
          case REPEATABLE_READ:
            return TransactionIsolation.REPEATABLE_READ;
          default:
            throw new RuntimeException("Invalid Transaction Isolation Value for translation");
        }
      }

      @Override
      public boolean isUseXaTransactions() {
        return dsConfig.isUseXaTransactions();
      }

      public Optional<TlsContextFactory> getTlsContextFactory() {
        return dsConfig.getTlsContextFactory();
      }
    };
  }


  public static ParameterizedStatementDefinition mapParameterizedStatementDefinition(org.mule.extension.db.api.param.ParameterizedStatementDefinition def) {
    if (isNull(def)) {
      return null;
    }
    return new ParameterizedStatementDefinition(def.getSql(), def.getParameterTypes(), def.getInputParameters(),
                                                def.getQueryTimeout(), def.getQueryTimeoutUnit(), def.getFetchSize(),
                                                def.getMaxRows()) {};
  }

}
