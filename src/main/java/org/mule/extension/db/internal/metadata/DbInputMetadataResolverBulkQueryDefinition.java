/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;

import org.mule.extension.db.api.param.BulkQueryDefinition;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

/**
 * Input metadata resolver for BulkQueryDefinition that supports column numbers.
 * Resolves the input parameter types for bulk parameterized queries.
 * Returns an array type since bulk operations accept a list of parameter objects.
 */
public class DbInputMetadataResolverBulkQueryDefinition extends BaseInputMetadataResolverStatementDefinition
    implements InputTypeResolver<BulkQueryDefinition> {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "DbInputResolverBulkQueryDefinition";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, BulkQueryDefinition bulkQueryDefinition)
      throws MetadataResolvingException, ConnectionException {
    return resolveInputMetadata(context, bulkQueryDefinition);
  }
}
