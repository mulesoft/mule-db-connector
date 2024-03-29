/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.generic;

import static org.mule.extension.db.internal.domain.connection.ConnectionUtils.isOracle;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.generic.GenericConnectionProvider;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.api.connection.ConnectionProvider;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ConnectionProvider} that creates connections for any kind of database using a JDBC URL
 * and the required JDBC Driver Class
 */
@DisplayName("Generic Connection")
@Alias("generic")
@ExternalLib(name = "JDBC Driver", description = "A JDBC driver which supports connecting to the Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, type = JAR)
public class DbGenericConnectionProvider extends GenericConnectionProvider {

  private final Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> resolvedDbTypeNamesCache = new ConcurrentHashMap<>();

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    if (isOracle(connection)) {
      return new OracleDbConnection(connection, resolveCustomTypes(), resolvedDbTypesCache, cachedTemplates,
                                    resolvedDbTypeNamesCache);
    } else {
      return super.createDbConnection(connection);
    }
  }

}
