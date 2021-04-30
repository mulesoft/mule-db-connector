/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import org.mule.db.commons.api.exception.connection.ConnectionCreationException;
import org.mule.db.commons.internal.domain.connection.JdbcConnectionFactory;
import org.mule.db.commons.internal.domain.type.DbType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

public class OracleJdbcConnectionFactory extends JdbcConnectionFactory {

  protected OracleJdbcConnectionFactory(Builder builder) {
    super(builder);
  }

  @Override
  public Connection createConnection(DataSource dataSource, List<DbType> customDataTypes)
          throws SQLException, ConnectionCreationException {
    Connection connection = super.createConnection(dataSource, customDataTypes);
    return new OracleJdbcConnectionWrapper(connection);
  }

  public static final class Builder extends JdbcConnectionFactory.Builder {

    public OracleJdbcConnectionFactory build() {
      return new OracleJdbcConnectionFactory(this);
    }

  }

}