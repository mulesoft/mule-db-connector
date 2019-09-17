/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.DbType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

import static org.mule.extension.db.internal.util.StoredProcedureUtils.getStoreProcedureSchema;

/**
 * {@link DefaultDbConnection} implementation for Derby databases
 *
 * @since 1.3.4
 */
public class DerbyConnection extends DefaultDbConnection {

  DerbyConnection(Connection connection, List<DbType> dbTypes) {
    super(connection, dbTypes);
  }

  // We are disabling content streaming for Derby because of a incompatibility between the connector logic and the
  // driver. When the connector is iterating a ResultSet and hits the end of this one, the Derby driver automatically
  // closes all the created streams.
  @Override
  public boolean supportsContentStreaming() {
    return false;
  }


  @Override
  public Optional<String> getProcedureCatalog(QueryTemplate queryTemplate) throws SQLException {
    Connection conn = this.getJdbcConnection();
    return conn != null && conn.getCatalog() != null ? Optional.of(conn.getCatalog()) : Optional.empty();
  }

  /**
   * Get procedure schema
   */
  public Optional<String> getProcedureSchema(QueryTemplate queryTemplate) throws SQLException {
    return getStoreProcedureSchema(queryTemplate.getSqlText());
  }
}
