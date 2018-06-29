/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.source;

import static java.lang.String.format;
import static org.mule.extension.db.internal.operation.BaseDbOperations.DEFAULT_FETCH_SIZE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ADVANCED;
import org.mule.extension.db.api.param.ParameterizedStatementDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.QuerySettings;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.SelectExecutor;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.statement.QueryStatementFactory;
import org.mule.extension.db.internal.resolver.query.ParameterizedQueryResolver;
import org.mule.extension.db.internal.resolver.query.QueryResolver;
import org.mule.extension.db.internal.result.resultset.ListResultSetHandler;
import org.mule.extension.db.internal.result.resultset.ResultSetHandler;
import org.mule.extension.db.internal.result.row.InsensitiveMapRowHandler;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollContext.PollItem;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects from a table at a regular interval and generates one message per each obtained row.
 * <p>
 * Optionally, watermark and id columns can be provided. If a watermark column is provided, the values taken from that column
 * will be used to filter the contents of the next poll, so that only rows with a greater watermark value are returned. If an
 * id column is provided, this component will automatically make sure that the same row is not picked twice by concurrent polls
 *
 * @since 1.3
 */
@MetadataScope(outputResolver = RowListenerMetadataResolver.class)
@DisplayName("On Table Row")
@Summary("Triggers a message per each row in a table")
@Alias("listener")
public class RowListener extends PollingSource<Map<String, Object>, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RowListener.class);
  public static final String WATERMARK_PARAM_NAME = "watermark";

  /**
   * The name of the table to select from
   */
  @Parameter
  @MetadataKeyId
  private String table;

  /**
   * The name of the column to use for watermark. Values taken from this column will be used to filter the contents of the next
   * poll, so that only rows with a greater watermark value are processed.
   */
  @Parameter
  @Optional
  @Summary("The name of the column used for watermark")
  private String watermarkColumn;

  /**
   * The name of the column to consider as row ID. If provided, this component will make sure that the same row is not
   * processed twice by concurrent polls.
   */
  @Parameter
  @Optional
  @Summary("The name of the column to consider as row ID")
  private String idColumn;

  @ParameterGroup(name = ADVANCED)
  @NullSafe
  private QuerySettings settings;

  @Config
  private DbConnector config;

  @Connection
  private ConnectionProvider<DbConnection> connectionProvider;

  private final QueryResolver<ParameterizedStatementDefinition> queryResolver = new ParameterizedQueryResolver<>();
  private ItemHandler idHandler;
  private ItemHandler watermarkHandler;

  @Override
  protected void doStart() throws MuleException {
    if (idColumn != null) {
      idHandler = (item, row) -> {
        Object id = row.get(idColumn);
        if (id != null) {
          item.setId(id.toString());
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format(
                                "A null ID value was obtained for row %s. Idempotency will not be enforced for this row", row));
          }
        }
      };
    } else {
      idHandler = new NullItemHandler();
    }

    if (watermarkColumn != null) {
      watermarkHandler = (item, row) -> {
        Object watermark = row.get(watermarkColumn);
        if (watermark == null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format(
                                "A null watermark value was obtained for row %s. Watermark value won't be updated for this row",
                                row));
          }

          return;
        }

        if (!(watermark instanceof Serializable)) {
          LOGGER.error(format("Watermark values need to be serializable, but a value of type %s was found instead for row %s",
                              watermark.getClass().getName(), row));
        }

        item.setWatermark((Serializable) watermark);
      };
    } else {
      watermarkHandler = new NullItemHandler();
    }
  }

  @Override
  protected void doStop() {

  }

  @Override
  public void poll(PollContext<Map<String, Object>, Void> pollContext) {
    if (pollContext.isSourceStopping()) {
      return;
    }

    DbConnection connection;
    try {
      connection = connectionProvider.connect();
    } catch (Exception e) {
      if (e instanceof ConnectionException) {
        pollContext.onConnectionException((ConnectionException) e);
      }
      LOGGER.error(format("Could not obtain connection while trying to poll table '%s'. %s", table, e.getMessage()), e);
      return;
    }

    try {
      QueryDefinition queryDefinition = new QueryDefinition();
      StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table);
      pollContext.getWatermark().ifPresent(w -> {
        sql.append(" WHERE ").append(watermarkColumn).append(" > :").append(WATERMARK_PARAM_NAME);
        queryDefinition.addInputParameter(WATERMARK_PARAM_NAME, w);
      });

      queryDefinition.setSql(sql.toString());
      Query query = queryResolver.resolve(queryDefinition, config, connection, null);

      QueryStatementFactory statementFactory = new QueryStatementFactory();
      statementFactory.setFetchSize(settings.getFetchSize() != null ? settings.getFetchSize() : DEFAULT_FETCH_SIZE);
      statementFactory.setQueryTimeout(new Long(settings.getQueryTimeoutUnit().toSeconds(settings.getQueryTimeout())).intValue());

      ResultSetHandler resultSetHandler = new ListResultSetHandler(new InsensitiveMapRowHandler(connection));

      List<Map<String, Object>> rows =
          (List<Map<String, Object>>) new SelectExecutor(statementFactory, resultSetHandler).execute(connection, query);

      rows.forEach(row -> pollContext.accept(item -> {
        idHandler.accept(item, row);
        watermarkHandler.accept(item, row);

        item.setResult(Result.<Map<String, Object>, Void>builder()
            .output(row)
            .build());
      }));

    } catch (Exception e) {
      LOGGER.error(format("Failed to query table '%s' for new rows. %s", table, e.getMessage()), e);
    } finally {
      connectionProvider.disconnect(connection);
    }
  }

  @Override
  public void onRejectedItem(Result<Map<String, Object>, Void> result, SourceCallbackContext sourceCallbackContext) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Row has been rejected for processing: " + result.getOutput());
    }
  }

  @FunctionalInterface
  private interface ItemHandler extends BiConsumer<PollItem<Map<String, Object>, Void>, Map<String, Object>> {

  }


  private final class NullItemHandler implements ItemHandler {

    @Override
    public void accept(PollItem<Map<String, Object>, Void> mapVoidPollItem, Map<String, Object> stringObjectMap) {

    }
  }
}
