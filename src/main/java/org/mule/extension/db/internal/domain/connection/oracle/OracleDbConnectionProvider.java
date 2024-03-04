/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionParameters.DRIVER_CLASS_NAME;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Creates connections to a Oracle database
 *
 * @since 1.0
 */
@DisplayName("Oracle Connection")
@Alias("oracle")
@ExternalLib(name = "Oracle JDBC Driver", description = "A JDBC driver which supports connecting to an Oracle Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, requiredClassName = DRIVER_CLASS_NAME, type = JAR)
public class OracleDbConnectionProvider extends DbConnectionProvider {

  private static final String INVALID_CREDENTIALS_ORACLE_CODE = "ORA-01017";
  private static final String UNKNOWN_SID_ORACLE_CODE = "ORA-12505";
  private static final String UNKNOWN_SERVICE_ORACLE_CODE = "ORA-12514";
  private static final String UNKNOWN_HOST_ORACLE_CODE = "ORA-17868";
  private static final String IO_ERROR = "IO Error: The Network Adapter could not establish the connection";

  @ParameterGroup(name = CONNECTION)
  private OracleConnectionParameters oracleConnectionParameters;

  Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> resolvedDbTypeNamesCache = new ConcurrentHashMap<>();

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();

    initialiseIfNeeded(oracleConnectionParameters.getTlsContextFactory());
  }

  @Override
  public java.util.Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(mapDataSourceConfig(oracleConnectionParameters));
  }

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new OracleDbConnection(connection, super.resolveCustomTypes(), resolvedDbTypesCache,
                                  super.cachedTemplates, resolvedDbTypeNamesCache);
  }

  @Override
  public java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
    String message = e.getMessage();
    if (message.contains(INVALID_CREDENTIALS_ORACLE_CODE)) {
      return of(INVALID_CREDENTIALS);
    } else if (message.contains(UNKNOWN_SID_ORACLE_CODE) || message.contains(UNKNOWN_SERVICE_ORACLE_CODE)) {
      return of(INVALID_DATABASE);
    } else if (message.contains(IO_ERROR) || message.contains(UNKNOWN_HOST_ORACLE_CODE)) {
      return of(CANNOT_REACH);
    }
    return empty();
  }

  @Override
  @ExcludeFromGeneratedCoverage
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!super.equals(o)) {
      return false;
    }

    if (!(o instanceof OracleDbConnectionProvider)) {
      return false;
    }

    OracleDbConnectionProvider that = (OracleDbConnectionProvider) o;

    return Objects.equals(this.oracleConnectionParameters, that.oracleConnectionParameters) &&
        Objects.equals(this.resolvedDbTypesCache, that.resolvedDbTypesCache);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), oracleConnectionParameters, resolvedDbTypesCache);
  }

}
