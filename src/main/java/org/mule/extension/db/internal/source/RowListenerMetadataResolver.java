/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.source;

import org.mule.db.commons.internal.domain.metadata.SelectMetadataResolver;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

/**
 * Metadata resolver for the {@link RowListener}
 *
 * @since 1.3
 */
public class RowListenerMetadataResolver extends SelectMetadataResolver {

  @Override
  public MetadataType getOutputType(MetadataContext context, String table)
      throws MetadataResolvingException, ConnectionException {
    return super.getOutputType(context, "SELECT * FROM " + table);
  }

  @Override
  public String getResolverName() {
    return "RowListenerResolver";
  }
}
