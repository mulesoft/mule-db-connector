/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import static java.util.Collections.emptyList;
import static java.util.Optional.*;

import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionProvider.MYSQL_GAV;
import static org.mule.extension.db.internal.domain.logger.MuleMySqlLoggerEnhancerFactory.MYSQL_DRIVER_CLASS;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.mule.db.commons.api.config.DbPoolingProfile;
import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.db.commons.api.param.ColumnType;
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
 * Creates connections to a MySQL database.
 *
 * @since 1.0
 */
@DisplayName("MySQL Connection")
@Alias("my-sql")
@ExternalLib(name = "MySQL JDBC Driver", description = "A JDBC driver which supports connecting to the MySQL Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, requiredClassName = MYSQL_DRIVER_CLASS, type = JAR,
    coordinates = MYSQL_GAV)
public class MySqlConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

  private static final String ACCESS_DENIED = "Access denied";
  private static final String UNKNOWN_DATABASE = "Unknown database";
  private static final String COMMUNICATIONS_LINK_FAILURE = "Communications link failure";
  static final String MYSQL_GAV = "mysql:mysql-connector-java:5.1.48";

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
  private MySqlConnectionParameters mySqlParameters;

  private DbConnectionProvider dbConnectionProvider;

  @Override
  public void initialise() throws InitialisationException {
    dbConnectionProvider = new DbConnectionProvider(configName, registry, poolingProfile, columnTypes) {

      @Override
      public java.util.Optional<DataSource> getDataSource() {
        return empty();
      }

      @Override
      public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
        return java.util.Optional.ofNullable(mySqlParameters);
      }

      @Override
      public java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
        String message = e.getMessage();
        if (message.contains(ACCESS_DENIED)) {
          return of(INVALID_CREDENTIALS);
        } else if (message.contains(UNKNOWN_DATABASE)) {
          return of(INVALID_DATABASE);
        } else if (message.contains(COMMUNICATIONS_LINK_FAILURE)) {
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
