/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionProvider.DRIVER_CLASS_NAME;
import static org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionProvider.MSSQL_GAV;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDbPoolingProfile;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.extension.db.api.param.ColumnType;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * {@link DbConnectionProvider} implementation for Microsoft SQL Server Databases
 *
 * @since 1.1.0
 */
@DisplayName("Microsoft SQL Server Connection")
@Alias("mssql")
@ExternalLib(name = "Microsoft SQL Server Driver",
    description = "A JDBC driver which supports connecting to an Microsoft SQL Server Database",
    requiredClassName = DRIVER_CLASS_NAME, type = JAR, coordinates = MSSQL_GAV,
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN)
public class SqlServerConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

  static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  static final String MSSQL_GAV = "com.microsoft.sqlserver:mssql-jdbc:6.2.2.jre8";

  @RefName
  private String configName;

  @Inject
  private Registry registry;

  /**
   * Provides a way to configure database connection pooling.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private DbPoolingProfile poolingProfile;

  /**
   * Specifies non-standard column types
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private final List<ColumnType> columnTypes = emptyList();

  @ParameterGroup(name = CONNECTION)
  private SqlServerConnectionParameters connectionParameters;

  private DbConnectionProvider dbConnectionProvider;

  @Override
  public void initialise() throws InitialisationException {
    dbConnectionProvider =
        new DbConnectionProvider(configName, registry, mapDbPoolingProfile(poolingProfile), columnTypes) {

          @Override
          protected DbConnection createDbConnection(Connection connection) throws Exception {
            return new SqlServerConnection(connection, super.resolveCustomTypes());
          }

          @Override
          public java.util.Optional<DataSource> getDataSource() {
            return empty();
          }

          @Override
          public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
            return ofNullable(mapDataSourceConfig(connectionParameters));
          }

          @Override
          protected java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
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

        };

    dbConnectionProvider.initialise();
  }

  @Override
  public void dispose() {
    dbConnectionProvider.dispose();
  }

  @Override
  public DbConnection connect() throws ConnectionException {
    return dbConnectionProvider.connect();
  }

  @Override
  public void disconnect(DbConnection dbConnection) {
    dbConnectionProvider.disconnect(dbConnection);
  }

  @Override
  public ConnectionValidationResult validate(DbConnection dbConnection) {
    return dbConnectionProvider.validate(dbConnection);
  }

  public DataSource getConfiguredDataSource() {
    return dbConnectionProvider.getConfiguredDataSource();
  }

}
