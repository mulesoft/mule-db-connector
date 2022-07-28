/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.teradata;

import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionParameters;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

/**
 * {@link ConnectionProvider} that creates connections for any kind of database using a JDBC URL
 * and the required JDBC Driver Class
 */
@DisplayName("Teradata Connection")
@Alias("teradata")
public class TeradataConnectionProvider extends DbConnectionProvider {

  private final Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache = new ConcurrentHashMap<>();
  @ParameterGroup(name = CONNECTION)
  private TeradataConnectionParameters teradataConnectionParameters;

  @Override
  public java.util.Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(mapDataSourceConfig(teradataConnectionParameters));
  }
}
