/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.db.commons.AbstractDbConnector;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.db.commons.internal.domain.metadata.DbInputMetadataResolver;
import org.mule.db.commons.internal.operation.BulkOperations;
import org.mule.db.commons.internal.operation.OperationErrorTypeProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.extension.db.api.param.BulkQueryDefinition;
import org.mule.extension.db.api.param.BulkScript;
import org.mule.extension.db.api.param.QuerySettings;
import org.mule.sdk.api.runtime.parameter.CorrelationInfo;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.mule.db.commons.internal.domain.query.QueryType.DELETE;
import static org.mule.db.commons.internal.domain.query.QueryType.UPDATE;
import static org.mule.db.commons.internal.operation.BaseDbOperations.QUERY_GROUP;
import static org.mule.db.commons.internal.operation.BaseDbOperations.QUERY_SETTINGS;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.setAttributesForDbClientOperation;
import static org.mule.extension.db.internal.util.MigrationUtils.mapBulkQueryDefinition;
import static org.mule.extension.db.internal.util.MigrationUtils.mapBulkScript;
import static org.mule.extension.db.internal.util.MigrationUtils.mapQuerySettings;


/**
 * Contains a set of operations for performing bulk DML operations from a single statement.
 *
 * @since 1.0
 */
@Throws(OperationErrorTypeProvider.class)
public class DbBulkOperations implements Initialisable {

  private BulkOperations bulkOperations;

  @Inject
  private java.util.Optional<ForwardCompatibilityHelper> forwardCompatibilityHelper;

  @Override
  public void initialise() throws InitialisationException {
    this.bulkOperations = new BulkOperations.Builder().build();
  }

  /**
   * Allows executing one insert statement various times using different parameter bindings. This happens using one single
   * Database statement, which has performance advantages compared to executing one single update operation various times.
   *
   * @param query a {@link BulkQueryDefinition} as a parameter group
   * @param bulkInputParameters A {@link List} of {@link Map}s in which every list item represents a row to be inserted, and the map
   *        contains the parameter names as keys and the value the parameter is bound to.
   * @param connector the acting connector
   * @param connection the acting connection
   * @return an array of update counts containing one element for each executed command. The elements of the array are ordered
   *         according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkInsert(@DisplayName("Input Parameters") @Content @Placement(
      order = 1) @TypeResolver(DbInputMetadataResolver.class) List<Map<String, Object>> bulkInputParameters,
                          @ParameterGroup(name = QUERY_GROUP) BulkQueryDefinition query,
                          @Config AbstractDbConnector connector,
                          @Connection DbConnection connection,
                          CorrelationInfo correlationInfo,
                          StreamingHelper streamingHelper)
      throws SQLException {
    forwardCompatibilityHelper.ifPresent(fwh -> {
      DbConnectionTracingMetadata dbConnectionTracingMetadata = connection.getDbConnectionTracingMetadata();
      setAttributesForDbClientOperation(dbConnectionTracingMetadata,
                                        fwh.getDistributedTraceContextManager(correlationInfo),
                                        query.getSql());
    });
    return bulkOperations.bulkInsert(bulkInputParameters, mapBulkQueryDefinition(query), connector, connection, streamingHelper);
  }


  /**
   * Allows executing one update statement various times using different parameter bindings. This happens using one single
   * Database statement, which has performance advantages compared to executing one single update operation various times.
   *
   * @param query a {@link BulkQueryDefinition} as a parameter group
   * @param bulkInputParameters A {@link List} of {@link Map}s in which every list item represents a row to be inserted, and the map
   *        contains the parameter names as keys and the value the parameter is bound to.
   * @param connector the acting connector
   * @param connection the acting connection
   * @return an array of update counts containing one element for each executed command. The elements of the array are ordered
   *         according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkUpdate(@DisplayName("Input Parameters") @Content @Placement(
      order = 1) @TypeResolver(DbInputMetadataResolver.class) List<Map<String, Object>> bulkInputParameters,
                          @ParameterGroup(name = QUERY_GROUP) BulkQueryDefinition query,
                          @Config AbstractDbConnector connector,
                          @Connection DbConnection connection,
                          CorrelationInfo correlationInfo,
                          StreamingHelper streamingHelper)
      throws SQLException {
    forwardCompatibilityHelper.ifPresent(fwh -> {
      DbConnectionTracingMetadata dbConnectionTracingMetadata = connection.getDbConnectionTracingMetadata();
      setAttributesForDbClientOperation(dbConnectionTracingMetadata,
                                        UPDATE.name() + " " + dbConnectionTracingMetadata.getDbSystem(),
                                        fwh.getDistributedTraceContextManager(correlationInfo),
                                        query.getSql());
    });
    return bulkOperations.bulkUpdate(bulkInputParameters, mapBulkQueryDefinition(query), connector, connection, streamingHelper);
  }

  /**
   * Allows executing one delete statement various times using different parameter bindings. This happens using one single
   * Database statement, which has performance advantages compared to executing one single delete operation various times.
   *
   * @param query a {@link BulkQueryDefinition} as a parameter group
   * @param bulkInputParameters A {@link List} of {@link Map}s in which every list item represents a row to be inserted, and the map
   *        contains the parameter names as keys and the value the parameter is bound to.
   * @param connector the acting connector
   * @param connection the acting connection
   * @return an array of update counts containing one element for each executed command. The elements of the array are ordered
   *         according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] bulkDelete(@DisplayName("Input Parameters") @Content @Placement(
      order = 1) @TypeResolver(DbInputMetadataResolver.class) List<Map<String, Object>> bulkInputParameters,
                          @ParameterGroup(name = QUERY_GROUP) BulkQueryDefinition query,
                          @Config AbstractDbConnector connector,
                          @Connection DbConnection connection,
                          CorrelationInfo correlationInfo,
                          StreamingHelper streamingHelper)
      throws SQLException {
    forwardCompatibilityHelper.ifPresent(fwh -> {
      DbConnectionTracingMetadata dbConnectionTracingMetadata = connection.getDbConnectionTracingMetadata();
      setAttributesForDbClientOperation(dbConnectionTracingMetadata,
                                        DELETE.name() + " " + dbConnectionTracingMetadata.getDbSystem(),
                                        fwh.getDistributedTraceContextManager(correlationInfo),
                                        query.getSql());
    });
    return bulkOperations.bulkDelete(bulkInputParameters, mapBulkQueryDefinition(query), connector, connection, streamingHelper);
  }

  /**
   * Executes a SQL script in one single Database statement. The script is executed as provided by the user, without any parameter
   * binding.
   *
   * @param script a {@link BulkScript} as a parameter group
   * @param settings a {@link QuerySettings} as a parameter group
   * @param connection the acting connection
   * @return an array of update counts containing one element for each executed command. The elements of the array are ordered
   *         according to the order in which commands were added to the batch.
   * @throws SQLException if an error is produced
   */
  public int[] executeScript(@ParameterGroup(name = QUERY_GROUP) BulkScript script,
                             @ParameterGroup(name = QUERY_SETTINGS) QuerySettings settings,
                             @Connection DbConnection connection,
                             CorrelationInfo correlationInfo)
      throws SQLException {
    forwardCompatibilityHelper.ifPresent(fwh -> {
      DbConnectionTracingMetadata dbConnectionTracingMetadata = connection.getDbConnectionTracingMetadata();
      setAttributesForDbClientOperation(dbConnectionTracingMetadata,
                                        dbConnectionTracingMetadata.getDbSystem(),
                                        fwh.getDistributedTraceContextManager(correlationInfo),
                                        "<script>");
    });
    return bulkOperations.executeScript(mapBulkScript(script), mapQuerySettings(settings), connection);
  }

}
