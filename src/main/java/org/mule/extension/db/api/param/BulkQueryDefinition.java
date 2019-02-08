/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.extension.db.internal.domain.metadata.DbInputMetadataResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.List;
import java.util.Map;

/**
 * The definition of a bulk operations defined around one single SQL command
 *
 * @since 1.0
 */
public class BulkQueryDefinition extends StatementDefinition<BulkQueryDefinition> {

  @DisplayName("Input Parameters")
  @Content
  @Placement(order = 10)
  @TypeResolver(DbInputMetadataResolver.class)
  @Parameter
  List<Map<String, Object>> bulkInputParameters;

  public List<Map<String, Object>> getBulkInputParameters() {
    return bulkInputParameters;
  }
}
