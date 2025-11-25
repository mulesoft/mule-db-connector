/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;


import org.mule.db.commons.internal.domain.metadata.BaseDbMetadataResolver;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Metadata resolver for select operations that supports column number mapping to avoid duplicate column label issues. This
 * resolver extends the base functionality to support QueryDefinition with useColumnNumbers flag.
 */
public class SelectMetadataResolverQueryDefinition extends BaseDbMetadataResolver
    implements OutputTypeResolver<QueryDefinition> {

  public static final String DUPLICATE_COLUMN_LABEL_ERROR =
      "Query metadata contains multiple columns with the same label. Define column aliases to resolve this problem";

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "SelectResolverWithColumnNumbers";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, QueryDefinition queryDefinition)
      throws MetadataResolvingException, ConnectionException {

    typeLoader = context.getTypeLoader();
    typeBuilder = context.getTypeBuilder();

    String query = queryDefinition.getSql();
    if (isEmpty(query)) {
      throw new MetadataResolvingException("No Metadata available for an empty query", FailureCode.INVALID_METADATA_KEY);
    }

    PreparedStatement statement = getStatement(context, parseQuery(query));
    ResultSetMetaData statementMetaData;
    try {
      statementMetaData = statement.getMetaData();
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }

    if (statementMetaData == null) {
      throw new MetadataResolvingException(format("Driver did not return metadata for the provided SQL: [%s]", query),
                                           FailureCode.INVALID_METADATA_KEY);
    }

    ObjectTypeBuilder record = typeBuilder.objectType();
    Map<String, MetadataType> recordModels = new HashMap<>();
    try {
      for (int i = 1; i <= statementMetaData.getColumnCount(); i++) {
        int columnType = statementMetaData.getColumnType(i);
        MetadataType columnMetadataType = getDataTypeMetadataModel(columnType, statementMetaData.getColumnClassName(i));

        // Use column numbers or column labels based on the flag
        String columnKey;
        if (queryDefinition.isUseColumnNumbers()) {
          columnKey = String.valueOf(i);
        } else {
          columnKey = statementMetaData.getColumnLabel(i);
        }

        recordModels.put(columnKey, columnMetadataType);

        ObjectFieldTypeBuilder columnBuilder = record.addField();
        columnBuilder.key(columnKey);

        if (statementMetaData.isNullable(i) == ResultSetMetaData.columnNoNulls) {
          columnBuilder.required(true);
        }

        columnBuilder.value(columnMetadataType);
      }

      // Only check for duplicate column labels if not using column numbers
      if (!queryDefinition.isUseColumnNumbers() && statementMetaData.getColumnCount() != recordModels.size()) {
        throw new MetadataResolvingException(DUPLICATE_COLUMN_LABEL_ERROR, FailureCode.INVALID_METADATA_KEY);
      }
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }

    return record.build();
  }
}
