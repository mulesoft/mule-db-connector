/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.resultset;

import static java.nio.charset.Charset.defaultCharset;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.row.RowHandler;
import org.mule.extension.db.internal.StatementStreamingResultSetCloser;
import org.mule.extension.db.internal.util.ResultCharsetEncodedHandler;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Processes a {@link ResultSet} returning an iterator of maps.
 * <p/>
 * The {@link ResultSet} backing the returned {@link ResultSetIterator} will be closed when the connection it came from is closed.
 */
public class IteratorResultSetHandler implements ResultSetHandler, ResultCharsetEncodedHandler {

  private final RowHandler rowHandler;
  private final StatementStreamingResultSetCloser streamingResultSetCloser;
  private final Charset charset;

  public IteratorResultSetHandler(RowHandler rowHandler, StatementStreamingResultSetCloser streamingResultSetCloser) {
    this.rowHandler = rowHandler;
    this.streamingResultSetCloser = streamingResultSetCloser;
    this.charset = defaultCharset();
  }

  public IteratorResultSetHandler(RowHandler rowHandler, StatementStreamingResultSetCloser streamingResultSetCloser,
                                  Charset charset) {
    this.rowHandler = rowHandler;
    this.streamingResultSetCloser = streamingResultSetCloser;
    this.charset = charset;
  }

  @Override
  public ResultSetIterator processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException {
    streamingResultSetCloser.trackResultSet(resultSet);
    connection.beginStreaming();

    return new ResultSetIterator(resultSet, rowHandler);
  }

  @Override
  public boolean requiresMultipleOpenedResults() {
    return true;
  }

  @Override
  public Charset getCharset() {
    return charset;
  }
}
