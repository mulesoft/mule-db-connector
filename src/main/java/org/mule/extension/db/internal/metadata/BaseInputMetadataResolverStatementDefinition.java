/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.metadata.BaseDbMetadataResolver;
import org.mule.db.commons.internal.domain.param.InputQueryParam;
import org.mule.db.commons.internal.domain.query.QueryTemplate;
import org.mule.db.commons.internal.parser.SimpleQueryTemplateParser;
import org.mule.extension.db.api.param.StatementDefinition;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for input metadata resolvers that support column numbers.
 * Contains shared logic for resolving input parameter types.
 */

public class BaseInputMetadataResolverStatementDefinition extends BaseDbMetadataResolver {

  public static final Logger LOGGER = LoggerFactory.getLogger(BaseInputMetadataResolverStatementDefinition.class);

  public String getCategoryName() {
    return "DbCategory";
  }

  protected MetadataType resolveInputMetadata(MetadataContext context, StatementDefinition<?> statementDefinition)
      throws MetadataResolvingException, ConnectionException {
    this.typeLoader = context.getTypeLoader();
    this.typeBuilder = context.getTypeBuilder();
    QueryTemplate queryTemplate = this.parseQuery(statementDefinition.getSql());
    List<InputQueryParam> inputParams = queryTemplate.getInputParams();
    if (inputParams.isEmpty()) {
      return this.typeBuilder.nullType().build();
    } else {
      PreparedStatement statement = this.getStatement(context, queryTemplate);
      List<String> fieldNames = new ArrayList();

      for (InputQueryParam inputParam : inputParams) {
        String name = inputParam.getName();
        if (name == null) {
          return this.typeBuilder.anyType().build();
        }

        fieldNames.add(name);
      }

      try {
        return this.getInputMetadataUsingStatementMetadata(statement, fieldNames);
      } catch (SQLException var10) {
        return this.getStaticInputMetadata(fieldNames);
      }
    }
  }

  protected QueryTemplate parseQuery(String query) {
    return (new SimpleQueryTemplateParser()).parse(query);
  }

  protected PreparedStatement getStatement(MetadataContext context, QueryTemplate query)
      throws ConnectionException, MetadataResolvingException {
    DbConnection connection = (DbConnection) context.getConnection()
        .orElseThrow(() -> new MetadataResolvingException("A connection is required to resolve Metadata but none was provided",
                                                          FailureCode.INVALID_CONFIGURATION));

    try {
      PreparedStatement statement = connection.getJdbcConnection().prepareStatement(query.getSqlText());
      return statement;
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }
  }

  private MetadataType getStaticInputMetadata(List<String> fieldNames) {
    Map<String, MetadataType> recordModels = new HashMap();

    for (String fieldName : fieldNames) {
      recordModels.put(fieldName, this.getDataTypeMetadataModel(12));
    }

    ObjectTypeBuilder record = this.typeBuilder.objectType();
    recordModels.entrySet().forEach((e) -> record.addField().key((String) e.getKey()).value((MetadataType) e.getValue()));
    return record.build();
  }

  private MetadataType getInputMetadataUsingStatementMetadata(PreparedStatement statement, List<String> fieldNames)
      throws SQLException {
    ParameterMetaData parameterMetaData = statement.getParameterMetaData();
    Map<String, MetadataType> recordModels = new HashMap();
    int i = 1;
    ObjectTypeBuilder record = this.typeBuilder.objectType();

    for (String fieldName : fieldNames) {
      int dataType = parameterMetaData.getParameterType(i);
      ObjectFieldTypeBuilder fieldTypeBuilder = record.addField();
      fieldTypeBuilder.key(fieldName);
      String parameterClassName = null;

      try {
        parameterClassName = parameterMetaData.getParameterClassName(i);
      } catch (Exception var15) {
        LOGGER.debug("Could not get the class name for field name {} and data type id {}", fieldName, dataType);
      }

      try {
        if (parameterClassName != null) {
          fieldTypeBuilder.value(this.getDataTypeMetadataModel(dataType, parameterClassName));
        } else {
          fieldTypeBuilder.value(this.getDataTypeMetadataModel(dataType));
        }
      } catch (Exception var14) {
        LOGGER.error("Could not map the data type for field name {}, data type id {} and parameter class name {}",
                     new Object[] {fieldName, dataType, parameterClassName});
        fieldTypeBuilder.value(this.typeBuilder.anyType().build());
      }

      try {
        int nullableCode = parameterMetaData.isNullable(i);
        if (nullableCode == 0) {
          fieldTypeBuilder.required();
        }
      } catch (Exception var13) {
      }

      ++i;
    }

    recordModels.entrySet().forEach((ex) -> record.addField().key((String) ex.getKey()).value((MetadataType) ex.getValue()));
    return record.build();
  }
}
