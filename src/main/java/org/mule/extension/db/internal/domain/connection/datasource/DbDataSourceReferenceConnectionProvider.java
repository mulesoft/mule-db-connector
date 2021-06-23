/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static java.util.Collections.emptyList;

import static java.util.Optional.ofNullable;
import static java.util.Optional.empty;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDbPoolingProfile;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;

import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.api.param.ColumnType;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

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
public class DbDataSourceReferenceConnectionProvider extends DbConnectionProvider {

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
  private DbDataSourceConnectionSettings connectionSettings;


  @Override
  public void initialise() throws InitialisationException {
    super.columnTypes = columnTypes;
    super.configName = configName;
    super.registry = registry;
    super.poolingProfile = mapDbPoolingProfile(poolingProfile);
    super.initialise();
  }

  @Override
  public java.util.Optional<DataSource> getDataSource() {
    return ofNullable(connectionSettings.getDataSourceRef());
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return empty();
  }
}
