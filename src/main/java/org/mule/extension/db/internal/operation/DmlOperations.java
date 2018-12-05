/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.util.Arrays.asList;
import static org.mule.extension.db.internal.domain.query.QueryType.DELETE;
import static org.mule.extension.db.internal.domain.query.QueryType.INSERT;
import static org.mule.extension.db.internal.domain.query.QueryType.MERGE;
import static org.mule.extension.db.internal.domain.query.QueryType.SELECT;
import static org.mule.extension.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import static org.mule.extension.db.internal.domain.query.QueryType.TRUNCATE;
import static org.mule.extension.db.internal.domain.query.QueryType.UPDATE;
import static org.mule.extension.db.internal.operation.AutoGenerateKeysAttributes.AUTO_GENERATE_KEYS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.StatementStreamingResultSetCloser;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.SelectExecutor;
import org.mule.extension.db.internal.domain.executor.StoredProcedureExecutor;
import org.mule.extension.db.internal.domain.metadata.SelectMetadataResolver;
import org.mule.extension.db.internal.domain.metadata.StoredProcedureMetadataResolver;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryType;
import org.mule.extension.db.internal.domain.statement.QueryStatementFactory;
import org.mule.extension.db.internal.resolver.query.StoredProcedureQueryResolver;
import org.mule.extension.db.internal.result.resultset.IteratorResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ListResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetIterator;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;
import org.mule.extension.db.internal.result.statement.StatementResultHandler;
import org.mule.extension.db.internal.result.statement.StreamingStatementResultHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a set of operations for performing single statement DML operations
 *
 * @since 1.0
 */
@Throws(OperationErrorTypeProvider.class)
public class DmlOperations extends BaseDbOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(DmlOperations.class);

  private final StoredProcedureQueryResolver storedProcedureResolver = new StoredProcedureQueryResolver();

  /**
   * Selects data from a database.
   *
   * Streaming is automatically applied to avoid preemptive consumption of such results, which may lead
   * to performance and memory issues.
   *
   * @param query     a {@link QueryDefinition} as a parameter group
   * @param connector the acting connector
   * @return depending on the value of {@code streaming}, it can be a {@link List} or {@link Iterator} of maps
   * @throws SQLException if an error is produced
   */
  @OutputResolver(output = SelectMetadataResolver.class)
  public PagingProvider<DbConnection, Map<String, Object>> select(
                                                                  @ParameterGroup(name = QUERY_GROUP) @Placement(
                                                                      tab = ADVANCED_TAB) QueryDefinition query,
                                                                  @Config DbConnector connector,
                                                                  StreamingHelper streamingHelper,
                                                                  FlowListener flowListener)
      throws SQLException {

    return new PagingProvider<DbConnection, Map<String, Object>>() {

      private final AtomicBoolean initialised = new AtomicBoolean(false);
      private ResultSetIterator iterator;
      private StatementStreamingResultSetCloser resultSetCloser;

      @Override
      public List<Map<String, Object>> getPage(DbConnection connection) {
        ResultSetIterator iterator = getIterator(connection);
        final int fetchSize = getFetchSize(query);
        final List<Map<String, Object>> page = new ArrayList<>(fetchSize);
        for (int i = 0; i < fetchSize && iterator.hasNext(); i++) {
          page.add(resolveResultStreams(iterator.next(), streamingHelper));
        }

        return page;
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(DbConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(DbConnection connection) throws MuleException {
        resultSetCloser.closeResultSets();
      }

      private ResultSetIterator getIterator(DbConnection connection) {
        if (initialised.compareAndSet(false, true)) {
          resultSetCloser = new StatementStreamingResultSetCloser(connection);
          flowListener.onError(e -> {
            try {
              close(connection);
            } catch (Exception t) {
              if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format("Exception was found closing connection for select operation: %s. Error was: %s",
                                          query.getSql(), t.getMessage()),
                            e);
              }
            }
          });
          final Query resolvedQuery = resolveQuery(query, connector, connection, streamingHelper, SELECT, STORE_PROCEDURE_CALL);

          QueryStatementFactory statementFactory = getStatementFactory(query);
          InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler(connection);
          ResultSetHandler resultSetHandler = new IteratorResultSetHandler(recordHandler, resultSetCloser);

          try {
            iterator =
                (ResultSetIterator) new SelectExecutor(statementFactory, resultSetHandler).execute(connection, resolvedQuery);
          } catch (SQLException e) {
            throw new MuleRuntimeException(e);
          }
        }

        return iterator;
      }

      @Override
      public boolean useStickyConnections() {
        return true;
      }
    };
  }

  /**
   * Inserts data into a Database
   *
   * @param query                      {@link QueryDefinition} as a parameter group
   * @param autoGenerateKeysAttributes an {@link AutoGenerateKeysAttributes} as a parameter group
   * @param connector                  the acting connector
   * @param connection                 the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult insert(@ParameterGroup(name = QUERY_GROUP) @Placement(tab = ADVANCED_TAB) QueryDefinition query,
                                @ParameterGroup(name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                @Config DbConnector connector,
                                @Connection DbConnection connection,
                                StreamingHelper streamingHelper)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, streamingHelper, INSERT);
    return executeUpdate(query, autoGenerateKeysAttributes, connection, resolvedQuery);
  }

  /**
   * Updates data in a database.
   *
   * @param query                      {@link QueryDefinition} as a parameter group
   * @param autoGenerateKeysAttributes an {@link AutoGenerateKeysAttributes} as a parameter group
   * @param connector                  the acting connector
   * @param connection                 the acting connection
   * @return a {@link StatementResult}
   * @throws SQLException if an error is produced
   */
  public StatementResult update(@ParameterGroup(name = QUERY_GROUP) QueryDefinition query,
                                @ParameterGroup(name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                @Config DbConnector connector,
                                @Connection DbConnection connection,
                                StreamingHelper streamingHelper)
      throws SQLException {

    final Query resolvedQuery =
        resolveQuery(query, connector, connection, streamingHelper, UPDATE, TRUNCATE, MERGE, STORE_PROCEDURE_CALL);
    return executeUpdate(query, autoGenerateKeysAttributes, connection, resolvedQuery);
  }

  /**
   * Deletes data in a database.
   *
   * @param query      {@link QueryDefinition} as a parameter group
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return the number of affected rows
   * @throws SQLException if an error is produced
   */
  public int delete(@ParameterGroup(name = QUERY_GROUP) QueryDefinition query,
                    @Config DbConnector connector,
                    @Connection DbConnection connection,
                    StreamingHelper streamingHelper)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(query, connector, connection, streamingHelper, DELETE);
    return executeUpdate(query, null, connection, resolvedQuery).getAffectedRows();
  }

  /**
   * Invokes a Stored Procedure on the database.
   * <p>
   * When the stored procedure returns one or more {@link ResultSet} instances, streaming
   * is automatically applied to avoid preemptive consumption of such results, which may lead
   * to performance and memory issues.
   *
   * @param call       a {@link StoredProcedureCall} as a parameter group
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return A {@link Map} with the procedure's output
   * @throws SQLException if an error is produced
   */
  @OutputResolver(output = StoredProcedureMetadataResolver.class)
  public Map<String, Object> storedProcedure(@ParameterGroup(name = QUERY_GROUP) StoredProcedureCall call,
                                             @ParameterGroup(
                                                 name = AUTO_GENERATE_KEYS) AutoGenerateKeysAttributes autoGenerateKeysAttributes,
                                             @Config DbConnector connector,
                                             @Connection DbConnection connection,
                                             StreamingHelper streamingHelper,
                                             FlowListener flowListener)
      throws SQLException {

    final Query resolvedQuery = resolveQuery(call, connector, connection, streamingHelper, STORE_PROCEDURE_CALL);

    QueryStatementFactory statementFactory = getStatementFactory(call);

    InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler(connection);

    StatementStreamingResultSetCloser resultSetCloser = new StatementStreamingResultSetCloser(connection);
    flowListener.onError(e -> resultSetCloser.closeResultSets());

    StatementResultHandler resultHandler = connection.getJdbcConnection().getMetaData().supportsMultipleOpenResults()
        ? new StreamingStatementResultHandler(new IteratorResultSetHandler(recordHandler, resultSetCloser))
        : new StreamingStatementResultHandler(new ListResultSetHandler(recordHandler));

    Map<String, Object> result = (Map<String, Object>) new StoredProcedureExecutor(statementFactory, resultHandler)
        .execute(connection, resolvedQuery, getAutoGeneratedKeysStrategy(autoGenerateKeysAttributes));

    return resolveResultStreams(result, streamingHelper);
  }


  protected Query resolveQuery(StoredProcedureCall call,
                               DbConnector connector,
                               DbConnection connection,
                               StreamingHelper streamingHelper,
                               QueryType... validTypes) {

    final Query resolvedQuery = storedProcedureResolver.resolve(call, connector, connection, streamingHelper);
    validateQueryType(resolvedQuery.getQueryTemplate(), asList(validTypes));

    return resolvedQuery;
  }

  private Map<String, Object> resolveResultStreams(Map<String, Object> map, StreamingHelper streamingHelper) {
    return resolveMap(map, true, streamingHelper);
  }

  //TODO MULE-14616: This is a copy of the StreamingHelper adding support for TypedValue properties.
  private <K> Map<K, Object> resolveMap(Map<K, Object> map, boolean recursive, StreamingHelper streamingHelper) {
    checkArgument(map != null, "Map cannot be null");
    Map<K, Object> resolved;
    try {
      resolved = ClassUtils.instantiateClass(map.getClass());
    } catch (Exception e) {
      resolved = new LinkedHashMap<>();
    }

    for (Map.Entry<K, Object> entry : map.entrySet()) {
      Object value = resolveCursorProvider(entry.getValue(), streamingHelper);

      if (recursive && value instanceof Map) {
        value = streamingHelper.resolveCursors((Map) value, recursive);
      }

      resolved.put(entry.getKey(), value);
    }

    return resolved;
  }

  private static Object resolveCursorProvider(Object value, StreamingHelper streamingHelper) {
    if (value instanceof TypedValue) {
      TypedValue typedValue = (TypedValue) value;
      Object newValue = streamingHelper.resolveCursorProvider(typedValue.getValue());
      return new TypedValue<>(newValue, typedValue.getDataType());
    } else {
      return streamingHelper.resolveCursorProvider(value);
    }
  }
}
