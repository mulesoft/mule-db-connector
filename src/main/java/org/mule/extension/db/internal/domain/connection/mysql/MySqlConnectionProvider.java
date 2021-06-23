/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionProvider.MYSQL_GAV;
import static org.mule.extension.db.internal.domain.logger.MuleMySqlLoggerEnhancerFactory.MYSQL_DRIVER_CLASS;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

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
public class MySqlConnectionProvider extends DbConnectionProvider {

  private static final String ACCESS_DENIED = "Access denied";
  private static final String UNKNOWN_DATABASE = "Unknown database";
  private static final String COMMUNICATIONS_LINK_FAILURE = "Communications link failure";
  static final String MYSQL_GAV = "mysql:mysql-connector-java:5.1.48";


  @ParameterGroup(name = CONNECTION)
  private MySqlConnectionParameters mySqlParameters;

  @Override
  public java.util.Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return java.util.Optional.ofNullable(mapDataSourceConfig(mySqlParameters));
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

}
