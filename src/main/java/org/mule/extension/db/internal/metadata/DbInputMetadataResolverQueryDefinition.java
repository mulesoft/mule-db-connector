/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;

import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

/**
 * Input metadata resolver for QueryDefinition that supports column numbers.
 * Resolves the input parameter types for parameterized queries.
 */
public class DbInputMetadataResolverQueryDefinition extends BaseInputMetadataResolverStatementDefinition
    implements InputTypeResolver<QueryDefinition> {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "DbInputResolverQueryDefinition";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, QueryDefinition queryDefinition)
      throws MetadataResolvingException, ConnectionException {
    return resolveInputMetadata(context, queryDefinition);
  }
}
