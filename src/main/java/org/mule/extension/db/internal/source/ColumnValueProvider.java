/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.source;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * {@link ValueProvider} implementation to suggest the available columns for a given Table
 *
 * @since 1.4.1
 */
public class ColumnValueProvider implements ValueProvider {

  @Parameter
  private String table;

  @Connection
  private DbConnection dbConnection;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    Set<Value> values = new HashSet<>();

    try {
      ResultSet tables = dbConnection
              .getJdbcConnection()
              .getMetaData()
              .getColumns(null, null, table, null);

      while (tables.next()) {
        values.add(ValueBuilder.newValue(tables.getString("COLUMN_NAME")).build());
      }
    } catch (SQLException e) {
      throw new ValueResolvingException(format("Unexpected error occurred trying to obtain Column Names for table [%s]", table),
              "UNKNOWN", e);
    }

    return values;
  }
}