/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver;

import static org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionProvider.DRIVER_CLASS_NAME;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.HashMap;
import java.util.Map;

/**
 * Connection parameters for SQL Server
 *
 * @since 1.1.0
 */
public class SqlServerConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  private static final String SUB_PROTOCOL = "jdbc:sqlserver://";

  /**
   * Configures the host of the database
   */
  @Parameter
  @Placement(order = 1)
  private String host;

  /**
   * Configures the port of the database
   */
  @Parameter
  @Optional(defaultValue = "1433")
  @Placement(order = 2)
  private Integer port;

  /**
   * The user that is used for authentication against the database
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String user;

  /**
   * The password that is used for authentication against the database
   */
  @Parameter
  @Optional
  @Placement(order = 4)
  @Password
  private String password;

  /**
   * Name of the default database to work with
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  private String databaseName;

  /**
   * Specifies a list of custom key-value connectionProperties for the config.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB)
  @NullSafe
  private Map<String, String> connectionProperties = new HashMap<>();

  @Override
  public String getUrl() {
    return SUB_PROTOCOL + host + ":" + port + getProperties();
  }

  private String getProperties() {
    StringBuilder stringBuilder = new StringBuilder();

    if (databaseName != null) {
      stringBuilder.append(";databaseName=");
      stringBuilder.append(databaseName);
    }

    connectionProperties.forEach((key, value) -> {
      stringBuilder.append(";");
      stringBuilder.append(key);
      stringBuilder.append("=");
      stringBuilder.append(value);
    });

    return stringBuilder.toString();
  }

  @Override
  public String getDriverClassName() {
    return DRIVER_CLASS_NAME;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUser() {
    return user;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
