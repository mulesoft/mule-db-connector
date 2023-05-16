/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;
import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionParameters.DERBY_DRIVER_CLASS;
import static org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionProvider.DERBY_GAV;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.extension.db.internal.domain.connection.derby.tracing.DerbyDbConnectionTracingMetadata;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Creates connections to a Derby database
 *
 * @since 1.0
 */
@DisplayName("Derby Connection")
@Alias("derby")
@ExternalLib(name = "Derby JDBC Driver", description = "A JDBC driver which supports connecting to a Derby Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, requiredClassName = DERBY_DRIVER_CLASS, type = JAR, coordinates = DERBY_GAV)
public class DerbyConnectionProvider extends DbConnectionProvider {

  private static final String FAILED_TO_START_DATABASE = "Failed to start database";
  private static final String NOT_FOUND = "not found";
  static final String DERBY_GAV = "org.apache.derby:derby:10.14.2.0";


  @ParameterGroup(name = CONNECTION)
  private DerbyConnectionParameters derbyParameters;


  @Override
  public java.util.Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(mapDataSourceConfig(derbyParameters));
  }

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new DerbyConnection(connection, resolveCustomTypes(), super.getCacheQueryTemplateSize(),
                               new DerbyDbConnectionTracingMetadata(derbyParameters));
  }

  @Override
  public java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
    if (Arrays.stream(new String[] {FAILED_TO_START_DATABASE, NOT_FOUND}).anyMatch(e.getMessage()::contains)) {
      return java.util.Optional.of(CANNOT_REACH);
    }

    return empty();
  }

}
