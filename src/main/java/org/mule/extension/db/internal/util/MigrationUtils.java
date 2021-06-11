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

import org.mule.runtime.api.util.Reference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides Objects translations between the DB Connector Types and the DB Client Types
 */
public class MigrationUtils {

  public static BulkScript mapBulkScript(org.mule.extension.db.api.param.BulkScript bulkScript) {
    ObjectMapper mapper = new AutoObjectMapper();
    return mapper.map(bulkScript, BulkScript.class);
  }

  public static QuerySettings mapQuerySettings(org.mule.extension.db.api.param.QuerySettings querySettings) {
    ObjectMapper mapper = new AutoObjectMapper();
    return mapper.map(querySettings, QuerySettings.class);
  }

  public static BulkQueryDefinition mapBulkQueryDefinition(org.mule.extension.db.api.param.BulkQueryDefinition queryDefinition) {
    ObjectMapper mapper = new AutoObjectMapper();
    return mapper.map(queryDefinition, BulkQueryDefinition.class);
  }

  public static StoredProcedureCall mapStoredProcedureCall(org.mule.extension.db.api.param.StoredProcedureCall spCall) {
    if (Objects.isNull(spCall)) {
      return null;
    }
    ObjectMapper mapper = new AutoObjectMapper();
    return new StoredProcedureCall() {


      @Override
      public List<ParameterType> getOutputParameters() {
        return spCall.getOutputParameters().stream().map(x -> mapper.map(x, ParameterType.class)).collect(Collectors.toList());
      }

      @Override
      public int getQueryTimeout() {
        return spCall.getQueryTimeout();
      }

      @Override
      public List<ParameterType> getParameterTypes() {
        return spCall.getParameterTypes().stream().map(x -> mapper.map(x, ParameterType.class)).collect(Collectors.toList());
      }

      @Override
      public Optional<ParameterType> getParameterType(String paramName) {
        if (spCall.getParameterType(paramName).isPresent()) {
          return Optional.of(mapper.map(spCall.getParameterType(paramName).get(), ParameterType.class));
        }
        return Optional.empty();
      }

      @Override
      public Integer getFetchSize() {
        return spCall.getFetchSize();
      }

      @Override
      public Integer getMaxRows() {
        return spCall.getMaxRows();
      }

      @Override
      public String getSql() {
        return spCall.getSql();
      }

      @Override
      public Map<String, Object> getInputParameters() {
        return spCall.getInputParameters();
      }

      @Override
      public TimeUnit getQueryTimeoutUnit() {
        return spCall.getQueryTimeoutUnit();
      }

      @Override
      public void setSql(String sql) {
        spCall.setSql(sql);
      }

      @Override
      public void addInputParameter(String paramName, Object value) {
        spCall.addInputParameter(paramName, value);
      }

      @Override
      public Map<String, Object> getInOutParameters() {
        return spCall.getInOutParameters();
      }

      @Override
      public Optional<OutputParameter> getOutputParameter(String name) {

        if (spCall.getOutputParameter(name).isPresent()) {
          org.mule.extension.db.api.param.OutputParameter source = spCall.getOutputParameter(name).get();
          TypeClassifier targetTClassifier = new TypeClassifier();

          if (Objects.nonNull(source) && Objects.nonNull(source.getTypeClassifier())
              && Objects.nonNull(source.getTypeClassifier().getJdbcType())) {
            switch (source.getTypeClassifier().getJdbcType()) {
              case BIT:
                targetTClassifier.setType(JdbcType.BIT);
                break;
              case REF:
                targetTClassifier.setType(JdbcType.REF);
                break;
              case BLOB:
                targetTClassifier.setType(JdbcType.BLOB);
                break;
              case CHAR:
                targetTClassifier.setType(JdbcType.CHAR);
                break;
              case CLOB:
                targetTClassifier.setType(JdbcType.CLOB);
                break;
              case DATE:
                targetTClassifier.setType(JdbcType.DATE);
                break;
              case NULL:
                targetTClassifier.setType(JdbcType.NULL);
                break;
              case REAL:
                targetTClassifier.setType(JdbcType.REAL);
                break;
              case TIME:
                targetTClassifier.setType(JdbcType.TIME);
                break;
              case ARRAY:
                targetTClassifier.setType(JdbcType.ARRAY);
                break;
              case FLOAT:
                targetTClassifier.setType(JdbcType.FLOAT);
                break;
              case NCHAR:
                targetTClassifier.setType(JdbcType.NCHAR);
                break;
              case NCLOB:
                targetTClassifier.setType(JdbcType.NCLOB);
                break;
              case OTHER:
                targetTClassifier.setType(JdbcType.OTHER);
                break;
              case ROWID:
                targetTClassifier.setType(JdbcType.ROWID);
                break;
              case BIGINT:
                targetTClassifier.setType(JdbcType.BIGINT);
                break;
              case BINARY:
                targetTClassifier.setType(JdbcType.BINARY);
                break;
              case DOUBLE:
                targetTClassifier.setType(JdbcType.DOUBLE);
                break;
              case SQLXML:
                targetTClassifier.setType(JdbcType.SQLXML);
                break;
              case STRUCT:
                targetTClassifier.setType(JdbcType.STRUCT);
                break;
              case BOOLEAN:
                targetTClassifier.setType(JdbcType.BOOLEAN);
                break;
              case DECIMAL:
                targetTClassifier.setType(JdbcType.DECIMAL);
                break;
              case INTEGER:
                targetTClassifier.setType(JdbcType.INTEGER);
                break;
              case NUMERIC:
                targetTClassifier.setType(JdbcType.NUMERIC);
                break;
              case TINYINT:
                targetTClassifier.setType(JdbcType.TINYINT);
                break;
              case UNKNOWN:
                targetTClassifier.setType(JdbcType.UNKNOWN);
                break;
              case VARCHAR:
                targetTClassifier.setType(JdbcType.VARCHAR);
                break;
              case DATALINK:
                targetTClassifier.setType(JdbcType.DATALINK);
                break;
              case DISTINCT:
                targetTClassifier.setType(JdbcType.DISTINCT);
                break;
              case NVARCHAR:
                targetTClassifier.setType(JdbcType.NVARCHAR);
                break;
              case SMALLINT:
                targetTClassifier.setType(JdbcType.SMALLINT);
                break;
              case TIMESTAMP:
                targetTClassifier.setType(JdbcType.TIMESTAMP);
                break;
              case VARBINARY:
                targetTClassifier.setType(JdbcType.VARBINARY);
                break;
              case JAVA_OBJECT:
                targetTClassifier.setType(JdbcType.JAVA_OBJECT);
                break;
              case LONGVARCHAR:
                targetTClassifier.setType(JdbcType.LONGVARCHAR);
                break;
              case LONGNVARCHAR:
                targetTClassifier.setType(JdbcType.LONGNVARCHAR);
                break;
              case LONGVARBINARY:
                targetTClassifier.setType(JdbcType.LONGVARBINARY);
                break;
              default:
                throw new RuntimeException("Invalid JDBC Type Translation");
            }
          }
          targetTClassifier.setCustomType(source.getTypeClassifier().getCustomType());
          OutputParameter target = new OutputParameter(source.getKey(), targetTClassifier);
          return Optional.of(target);
        }
        return Optional.empty();
      }

      @Override
      public Optional<Reference<Object>> getInOutParameter(String name) {
        return spCall.getInOutParameter(name);
      }

      @Override
      public Optional<Reference<Object>> getInputParameter(String name) {
        return spCall.getInputParameter(name);
      }
    };

  }

  public static QueryDefinition mapQueryDefinition(org.mule.extension.db.api.param.QueryDefinition queryDefinition) {
    ObjectMapper mapper = new AutoObjectMapper();
    return mapper.map(queryDefinition, QueryDefinition.class);
  }

  public static DataSourceConfig mapDataSourceConfig(org.mule.extension.db.internal.domain.connection.DataSourceConfig dsConfig) {
    if (Objects.isNull(dsConfig)) {
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
    };
  }

  public static ParameterizedStatementDefinition mapParameterizedStatementDefinition(org.mule.extension.db.api.param.ParameterizedStatementDefinition def) {

    return new ParameterizedStatementDefinition() {

      @Override
      public Optional<Reference<Object>> getInputParameter(String name) {
        return def.getInputParameter(name);
      }

      @Override
      public int getQueryTimeout() {
        return def.getQueryTimeout();
      }

      @Override
      public List<ParameterType> getParameterTypes() {
        return def.getParameterTypes();
      }

      @Override
      public Map<String, Object> getInputParameters() {
        return def.getInputParameters();
      }

      @Override
      public Optional<ParameterType> getParameterType(String paramName) {
        return def.getParameterType(paramName);
      }

      @Override
      public Integer getFetchSize() {
        return def.getFetchSize();
      }

      @Override
      public String getSql() {
        return def.getSql();
      }

      @Override
      public void addInputParameter(String paramName, Object value) {
        def.addInputParameter(paramName, value);
      }

      @Override
      public Integer getMaxRows() {
        return def.getMaxRows();
      }

      @Override
      public TimeUnit getQueryTimeoutUnit() {
        return def.getQueryTimeoutUnit();
      }

      @Override
      public void setSql(String sql) {
        def.setSql(sql);
      }

      @Override
      public String toString() {
        return def.toString();
      }
    };

  }

}
