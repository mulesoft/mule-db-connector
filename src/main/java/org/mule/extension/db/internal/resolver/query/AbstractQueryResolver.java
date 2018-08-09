/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.mule.extension.db.api.exception.connection.QueryExecutionException;
import org.mule.extension.db.api.param.ParameterType;
import org.mule.extension.db.api.param.StatementDefinition;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.extension.db.internal.domain.param.InOutQueryParam;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.param.OutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.domain.type.UnknownDbType;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.extension.db.internal.resolver.param.GenericParamTypeResolverFactory;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

abstract class AbstractQueryResolver<T extends StatementDefinition<?>> implements QueryResolver<T> {

  protected Cache<String, QueryTemplate> queryTemplates = CacheBuilder.newBuilder().build();
  private QueryTemplateParser queryTemplateParser = new SimpleQueryTemplateParser();

  @Override
  public Query resolve(T statementDefinition, DbConnector connector, DbConnection connection, StreamingHelper streamingHelper) {
    QueryTemplate queryTemplate = getQueryTemplate(connector, connection, statementDefinition);
    return new Query(queryTemplate, resolveParams(statementDefinition, queryTemplate, streamingHelper));
  }

  protected abstract List<QueryParamValue> resolveParams(T statementDefinition, QueryTemplate template,
                                                         StreamingHelper streamingHelper);

  protected QueryTemplate createQueryTemplate(T statementDefinition, DbConnector connector, DbConnection connection) {
    if (isBlank(statementDefinition.getSql())) {
      throw new IllegalArgumentException("Statement doesn't contain a SQL query. Please provide one or reference a template which does");
    }

    QueryTemplate queryTemplate = queryTemplateParser.parse(statementDefinition.getSql());
    if (needsParamTypeResolution(queryTemplate)) {
      List<ParameterType> parameterTypes = statementDefinition.getParameterTypes();
      Map<Integer, DbType> paramTypes = getParameterTypes(connector, connection, queryTemplate, parameterTypes);
      queryTemplate = resolveQueryTemplate(queryTemplate, paramTypes);
    }

    return queryTemplate;
  }

  private Map<Integer, DbType> getParameterTypes(DbConnector connector, DbConnection connection, QueryTemplate queryTemplate,
                                                 List<ParameterType> types) {
    ParamTypeResolverFactory paramTypeResolverFactory =
        new GenericParamTypeResolverFactory(createTypeManager(connector, connection));

    try {
      return paramTypeResolverFactory.create(queryTemplate).getParameterTypes(connection, queryTemplate, types);
    } catch (SQLException e) {
      throw new QueryResolutionException("Cannot resolve parameter types", e);
    }
  }

  private QueryTemplate getQueryTemplate(DbConnector connector, DbConnection connection, T statementDefinition) {
    try {
      return queryTemplates.get(statementDefinition.getSql(),
                                () -> createQueryTemplate(statementDefinition, connector, connection));
    } catch (UncheckedExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), ModuleException.class);
      throw e;
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(I18nMessageFactory
          .createStaticMessage("Could not resolve query: " + statementDefinition.getSql(), e));
    }
  }

  private QueryTemplate resolveQueryTemplate(QueryTemplate queryTemplate, Map<Integer, DbType> paramTypes) {
    List<QueryParam> newParams = new ArrayList<>();

    for (QueryParam originalParam : queryTemplate.getParams()) {
      DbType type = paramTypes.get(originalParam.getIndex());
      QueryParam newParam;

      if (type == null) {
        throw new QueryExecutionException("Unknown parameter type of " + originalParam.getName());
      }

      if (originalParam instanceof InOutQueryParam) {
        newParam = new DefaultInOutQueryParam(originalParam.getIndex(), type, originalParam.getName(),
                                              ((InOutQueryParam) originalParam).getValue());
      } else if (originalParam instanceof InputQueryParam) {
        newParam =
            new DefaultInputQueryParam(originalParam.getIndex(), type, ((InputQueryParam) originalParam).getValue(),
                                       originalParam.getName());
      } else if (originalParam instanceof OutputQueryParam) {
        newParam = new DefaultOutputQueryParam(originalParam.getIndex(), type, originalParam.getName());
      } else {
        throw new IllegalArgumentException("Unknown parameter type: " + originalParam.getClass().getName());
      }

      newParams.add(newParam);
    }

    return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), newParams);
  }

  private boolean needsParamTypeResolution(QueryTemplate template) {
    return template.getParams().stream()
        .map(QueryParam::getType)
        .anyMatch(type -> type == UnknownDbType.getInstance() || type instanceof DynamicDbType);
  }

  protected DbTypeManager createTypeManager(DbConnector connector, DbConnection connection) {
    List<DbTypeManager> typeManagers = new LinkedList<>();
    List<DbTypeManager> vendorTypeManagers = new LinkedList<>();
    typeManagers.add(connector.getTypeManager());

    collectTypeManager(vendorTypeManagers, connection.getVendorDataTypes());
    collectTypeManager(vendorTypeManagers, connection.getCustomDataTypes());

    return new CompositeDbTypeManager(vendorTypeManagers, typeManagers);
  }

  private void collectTypeManager(List<DbTypeManager> collector, List<DbType> extraDataTypes) {
    if (!isEmpty(extraDataTypes)) {
      collector.add(new StaticDbTypeManager(extraDataTypes));
    }
  }

}
