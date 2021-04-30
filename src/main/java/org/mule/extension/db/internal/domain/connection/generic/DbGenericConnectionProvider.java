/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.generic;

import static java.util.Collections.emptyList;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import javax.inject.Inject;
import java.util.List;

import org.mule.db.commons.api.config.DbPoolingProfile;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.generic.GenericConnectionProvider;
import org.mule.db.commons.api.param.ColumnType;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * {@link ConnectionProvider} that creates connections for any kind of database using a JDBC URL
 * and the required JDBC Driver Class
 */
@DisplayName("Generic Connection")
@Alias("generic")
@ExternalLib(name = "JDBC Driver", description = "A JDBC driver which supports connecting to the Database",
        nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, type = JAR)
public class DbGenericConnectionProvider implements ConnectionProvider<DbConnection>, Initialisable, Disposable {

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
  private GenericConnectionParameters genericConnectionParameters;

  private GenericConnectionProvider genericConnectionProvider;

  @Override
  public void initialise() throws InitialisationException {
    genericConnectionProvider = new GenericConnectionProvider(configName, registry,
            poolingProfile, columnTypes, genericConnectionParameters);

    genericConnectionProvider.initialise();
  }

  @Override
  public void dispose() {
    genericConnectionProvider.dispose();
  }

  @Override
  public DbConnection connect() throws ConnectionException {
    return genericConnectionProvider.connect();
  }

  @Override
  public void disconnect(DbConnection dbConnection) {
    genericConnectionProvider.disconnect(dbConnection);
  }

  @Override
  public ConnectionValidationResult validate(DbConnection dbConnection) {
    return genericConnectionProvider.validate(dbConnection);
  }

}