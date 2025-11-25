/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.metadata;

import org.mule.db.commons.internal.domain.metadata.BaseDbMetadataResolver;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Metadata resolver for stored procedure operations.
 * Resolves output types for stored procedure calls.
 */
public class StoredProcedureMetadataResolverStoreProcedureCall extends BaseDbMetadataResolver
    implements OutputTypeResolver<StoredProcedureCall> {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "StoredProcedureResolver";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, StoredProcedureCall storedProcedureCall)
      throws MetadataResolvingException, ConnectionException {
    typeLoader = context.getTypeLoader();
    typeBuilder = context.getTypeBuilder();

    // Stored procedures return an object type containing output parameters and result sets
    return typeBuilder.objectType().build();
  }
}
