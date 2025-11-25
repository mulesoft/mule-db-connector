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

/**
 * Metadata resolver for querySingle operations that supports column number mapping.
 * Extends SelectMetadataResolverWithColumnNumbers to reuse the same logic.
 */
public class QuerySingleMetadataResolverQueryDefinition extends SelectMetadataResolverQueryDefinition {

  @Override
  public String getResolverName() {
    return "QuerySingleResolverWithColumnNumbers";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, QueryDefinition queryDefinition)
      throws MetadataResolvingException, ConnectionException {
    // Reuse the same logic from SelectMetadataResolverWithColumnNumbers
    return super.getOutputType(context, queryDefinition);
  }
}
