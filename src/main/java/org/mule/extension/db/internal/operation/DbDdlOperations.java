/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.db.commons.AbstractDbConnector;
import org.mule.extension.db.api.param.QuerySettings;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.operation.DdlOperations;
import org.mule.db.commons.internal.operation.OperationErrorTypeProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.sql.SQLException;

import static org.mule.extension.db.api.param.DbNameConstants.SQL_QUERY_TEXT;
import static org.mule.db.commons.internal.operation.BaseDbOperations.QUERY_SETTINGS;
import static org.mule.extension.db.internal.util.MigrationUtils.mapQuerySettings;

/**
 * Operations to manipulate data definitions in a relational Database
 * @since 1.0
 */
@Throws(OperationErrorTypeProvider.class)
public class DbDdlOperations implements Initialisable {

  private DdlOperations ddlOperations;

  @Override
  public void initialise() throws InitialisationException {
    ddlOperations = new DdlOperations.Builder().build();
  }

  /**
   * Enables execution of DDL queries against a database.
   *
   * @param sql        The text of the SQL query to be executed
   * @param settings   Parameters to configure the query
   * @param connector  the acting connector
   * @param connection the acting connection
   * @return the number of affected rows
   */
  @DisplayName("Execute DDL")
  public int executeDdl(@DisplayName(SQL_QUERY_TEXT) @Text String sql,
                        @ParameterGroup(name = QUERY_SETTINGS) QuerySettings settings,
                        @Config AbstractDbConnector connector,
                        @Connection DbConnection connection,
                        StreamingHelper streamingHelper)
      throws SQLException {
    return ddlOperations.executeDdl(sql, mapQuerySettings(settings), connector, connection, streamingHelper);
  }

}
