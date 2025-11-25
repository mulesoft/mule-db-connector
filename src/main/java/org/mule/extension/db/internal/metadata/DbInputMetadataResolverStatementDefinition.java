/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;

import org.mule.extension.db.api.param.StatementDefinition;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

/**
 * Input metadata resolver for StatementDefinition that supports column numbers.
 * Resolves the input parameter types for all statement types including:
 * - Parameterized queries (QueryDefinition)
 * - Bulk operations (BulkQueryDefinition)
 * - Stored procedures (StoredProcedureCall)
 */
public class DbInputMetadataResolverStatementDefinition extends BaseInputMetadataResolverStatementDefinition
    implements InputTypeResolver<StatementDefinition<?>> {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "DbInputResolverQueryDefinition";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, StatementDefinition statementDefinition)
      throws MetadataResolvingException, ConnectionException {
    return resolveInputMetadata(context, statementDefinition);
  }
}
