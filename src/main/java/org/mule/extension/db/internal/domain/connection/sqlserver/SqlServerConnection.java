/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.connection.sqlserver.type.SqlServerBinaryDbType;
import org.mule.extension.db.internal.domain.connection.sqlserver.type.SqlServerVarBinaryDbType;
import org.mule.extension.db.internal.domain.type.DbType;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DbConnection} implementation for SQL Server which configures out of the box custom
 *
 * @since 1.1.0
 */
public class SqlServerConnection extends DefaultDbConnection {

  SqlServerConnection(Connection jdbcConnection, List<DbType> customDataTypes) {
    super(jdbcConnection, customDataTypes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DbType> getCustomDataTypes() {
    List<DbType> dbTypes = new ArrayList<>();
    dbTypes.add(new SqlServerVarBinaryDbType());
    dbTypes.add(new SqlServerBinaryDbType());

    return dbTypes;
  }
}
