/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionParameters.DERBY_DRIVER_CLASS;
import static org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionProvider.DERBY_GAV;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
 * Creates connections to a Derby database
 *
 * @since 1.0
 */
@DisplayName("Derby Connection")
@Alias("derby")
@ExternalLib(name = "Derby JDBC Driver", description = "A JDBC driver which supports connecting to a Derby Database",
        nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, requiredClassName = DERBY_DRIVER_CLASS, type = JAR, coordinates = DERBY_GAV)
public class DerbyConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

  private static final String FAILED_TO_START_DATABASE = "Failed to start database";
  private static final String NOT_FOUND = "not found";
  static final String DERBY_GAV = "org.apache.derby:derby:10.13.1.1";

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
  private DerbyConnectionParameters derbyParameters;

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
        return ofNullable(derbyParameters);
      }

      @Override
      protected DbConnection createDbConnection(Connection connection) throws Exception {
        return new DerbyConnection(connection, dbConnectionProvider.resolveCustomTypes());
      }

      @Override
      public java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
        if (e.getMessage().contains(FAILED_TO_START_DATABASE)) {
          return java.util.Optional.of(CANNOT_REACH);
        } else if (e.getMessage().contains(NOT_FOUND)) {
          return java.util.Optional.of(CANNOT_REACH);
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

}