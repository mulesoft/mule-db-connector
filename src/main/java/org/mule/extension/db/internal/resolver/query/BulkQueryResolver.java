/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.query;

import org.mule.db.commons.BaseDbConnector;
import org.mule.db.commons.api.param.BulkQueryDefinition;
import org.mule.db.commons.api.param.ParameterType;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.param.DefaultInputQueryParam;
import org.mule.db.commons.internal.domain.param.InputQueryParam;
import org.mule.db.commons.internal.domain.param.QueryParam;
import org.mule.db.commons.internal.domain.query.BulkQuery;
import org.mule.db.commons.internal.domain.query.Query;
import org.mule.db.commons.internal.domain.query.QueryParamValue;
import org.mule.db.commons.internal.domain.query.QueryTemplate;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.db.commons.internal.resolver.query.AbstractQueryResolver;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a {@link BulkQuery} for a given {@link BulkQueryDefinition}
 */
public class BulkQueryResolver extends AbstractQueryResolver<BulkQueryDefinition> {

  @Override
  public Query resolve(BulkQueryDefinition definition, BaseDbConnector connector, DbConnection connection,
                       StreamingHelper streamingHelper) {
    Query query = super.resolve(definition, connector, connection, streamingHelper);
    List<QueryParam> queryParams = new LinkedList<>();

    final QueryTemplate queryTemplate = query.getQueryTemplate();
    queryTemplate.getParams().forEach(inputParam -> {
      if (inputParam instanceof InputQueryParam) {
        String paramName = inputParam.getName();
        Optional<ParameterType> parameterType = definition.getParameterType(paramName);
        if (parameterType.isPresent()) {
          queryParams
              .add(new DefaultInputQueryParam(inputParam.getIndex(), parameterType.get().getDbType(), null, paramName));
          return;
        }
      }

      queryParams.add(inputParam);
    });

    return new Query(new QueryTemplate(queryTemplate.getSqlText(),
                                       queryTemplate.getType(),
                                       queryParams,
                                       queryTemplate.isDynamic()));
  }

  @Override
  protected List<QueryParamValue> resolveParams(BulkQueryDefinition statementDefinition,
                                                QueryTemplate template,
                                                StreamingHelper streamingHelper) {
    return new LinkedList<>();
  }
}
