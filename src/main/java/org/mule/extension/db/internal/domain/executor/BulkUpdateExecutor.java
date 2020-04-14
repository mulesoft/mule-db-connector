/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.executor;

import org.mule.extension.db.api.exception.connection.QueryExecutionException;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.logger.BulkQueryLogger;
import org.mule.extension.db.internal.domain.logger.PreparedBulkQueryLogger;
import org.mule.extension.db.internal.domain.query.BulkQuery;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.statement.StatementFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.BatchUpdateException;
import java.util.List;

/**
 * Executes bulk queries
 */
public class BulkUpdateExecutor extends AbstractExecutor implements BulkExecutor {

  public BulkUpdateExecutor(StatementFactory statementFactory) {
    super(statementFactory);
  }

  @Override
  public Object execute(DbConnection connection, BulkQuery bulkQuery) throws SQLException {
    Statement statement = statementFactory.create(connection, bulkQuery.getQueryTemplates().get(0));

    try {
      BulkQueryLogger queryLogger = queryLoggerFactory.createBulkQueryLogger(LOGGER);

      for (QueryTemplate queryTemplate : bulkQuery.getQueryTemplates()) {
        String sql = queryTemplate.getSqlText();

        statement.addBatch(sql);
        queryLogger.addQuery(sql);
      }

      queryLogger.logQuery();

      return statement.executeBatch();
    } finally {
      statement.clearBatch();
      statement.close();
    }
  }

  @Override
  public Object execute(DbConnection connection, Query query, List<List<QueryParamValue>> paramValues) throws SQLException {
    Statement statement = statementFactory.create(connection, query.getQueryTemplate());

    if (!(statement instanceof PreparedStatement)) {
      throw new QueryExecutionException("The given query can't be executed in bulk, bulk queries must take parameters.");
    }

    PreparedStatement preparedStatement = (PreparedStatement) statement;
    PreparedBulkQueryLogger queryLogger =
        queryLoggerFactory.createBulkQueryLogger(LOGGER, query.getQueryTemplate(), paramValues.size());
    try {
      for (List<QueryParamValue> params : paramValues) {
        doProcessParameters(preparedStatement, query.getQueryTemplate(), params, queryLogger, connection);
        preparedStatement.addBatch();
        queryLogger.addParameterSet();
      }

      queryLogger.logQuery();

      return preparedStatement.executeBatch();
    } catch (BatchUpdateException batchEx) {
      int[] updateCounts = batchEx.getUpdateCounts();
      int successfulRecords, failedRecords, noInfoAvailable;
      successfulRecords = 0;
      failedRecords = 0;
      noInfoAvailable = 0;
      for (int i = 0; i < updateCounts.length; i++) {
        if (updateCounts[i] >= 0) {
          successfulRecords++;
        } else if (updateCounts[i] == Statement.SUCCESS_NO_INFO) {
          noInfoAvailable++;
        } else if (updateCounts[i] == Statement.EXECUTE_FAILED) {
          failedRecords++;
        }
      }
      LOGGER.error(String
          .format("BULK UPDATE EXCEPTION: %d SUCCESSFUL OPERATIONS, %d FAILED OPERATIONS, %d SUCCESSFULLY EXECUTED OPERATIONS BUT NO INFO ON AFFECTED ROW COUNT",
                  successfulRecords, failedRecords));
      throw new SQLException(batchEx);
    } catch (Exception e) {
      throw new SQLException(e);
    } finally {
      preparedStatement.clearParameters();
      statement.close();
    }
  }
}
