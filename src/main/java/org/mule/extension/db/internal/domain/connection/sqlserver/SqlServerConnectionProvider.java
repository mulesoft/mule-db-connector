/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.extension.db.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.extension.db.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.extension.db.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionProvider.DRIVER_CLASS_NAME;
import static org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionProvider.MSSQL_COORDINATE;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import org.mule.extension.db.api.exception.connection.DbError;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * {@link DbConnectionProvider} implementation for Microsoft SQL Server Databases
 *
 * @since 1.1.0
 */
@DisplayName("Microsoft SQL Server Connection")
@Alias("mssql")
@ExternalLib(name = "Microsoft SQL Server Driver",
    description = "A JDBC driver which supports connecting to an Microsoft SQL Server Database",
    requiredClassName = DRIVER_CLASS_NAME, type = JAR, coordinates = MSSQL_COORDINATE)
public class SqlServerConnectionProvider extends DbConnectionProvider {

  static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  static final String MSSQL_COORDINATE = "com.microsoft.sqlserver:mssql-jdbc:6.2.2.jre8";

  @ParameterGroup(name = CONNECTION)
  private SqlServerConnectionParameters connectionParameters;

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new SqlServerConnection(connection, super.resolveCustomTypes());
  }

  @Override
  public Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(connectionParameters);
  }

  @Override
  protected Optional<DbError> getDbVendorErrorType(SQLException e) {
    String message = e.getMessage();
    if (message.contains("Login failed for user")) {
      return of(INVALID_CREDENTIALS);
    } else if (message.contains("Cannot open database")) {
      return of(INVALID_DATABASE);
    } else if (message.contains("invalidHost")) {
      return of(CANNOT_REACH);
    }
    return empty();
  }

}
