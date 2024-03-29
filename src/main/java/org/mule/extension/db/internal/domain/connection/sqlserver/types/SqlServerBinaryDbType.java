/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver.types;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.type.AbstractStructuredDbType;
import org.mule.db.commons.internal.domain.type.DbType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link DbType} implementation of BINARY for SQL Server.
 * <p>
 * SQL Server requires String values that are
 *
 * @since 1.1.0
 */
public class SqlServerBinaryDbType extends AbstractStructuredDbType {

  public SqlServerBinaryDbType() {
    super(-2, "binary");
  }

  @Override
  public Object getParameterValue(CallableStatement statement, int index) throws SQLException {
    return super.getParameterValue(statement, index);
  }

  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value, DbConnection connection)
      throws SQLException {
    if (value instanceof String) {
      value = ((String) value).getBytes();
    }
    super.setParameterValue(statement, index, value, connection);
  }
}
