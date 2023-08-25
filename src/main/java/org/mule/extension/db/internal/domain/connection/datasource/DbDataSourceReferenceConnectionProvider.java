/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static org.mule.extension.db.internal.domain.connection.ConnectionUtils.isOracle;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;

import javax.sql.DataSource;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.datasource.DataSourceReferenceConnectionProvider;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ConnectionProvider} implementation which creates DB connections from a referenced {@link
 * DataSource}
 *
 * @since 1.0
 */
@DisplayName("Data Source Reference Connection")
@Alias("data-source")
@ExternalLib(name = "JDBC Driver", description = "A JDBC driver which supports connecting to the Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, type = JAR, optional = true)
public class DbDataSourceReferenceConnectionProvider extends DataSourceReferenceConnectionProvider {

  private final Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache = new ConcurrentHashMap<>();

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    if (isOracle(connection)) {
      return new OracleDbConnection(connection, super.resolveCustomTypes(), resolvedDbTypesCache,
                                    cachedTemplates);
    } else {
      return super.createDbConnection(connection);
    }
  }

}
