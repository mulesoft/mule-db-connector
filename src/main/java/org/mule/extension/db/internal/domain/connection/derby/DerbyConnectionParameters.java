/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DataSourceConfig} implementation for Derby databases.
 *
 * @since 1.0
 */
public final class DerbyConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  private static final String JDBC_URL_PREFIX = "jdbc:derby";
  static final String DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";

  /**
   * Name of the database
   */
  @Parameter
  @Optional
  @Placement(order = 1)
  private String database;

  /**
   * If your application uses an embedded database, the subsubprotocol parameter specifies where Derby should look for the database. The available options are:
   * 'directory', 'memory', 'classpath' and 'jar'.
   */
  @Parameter
  @Optional(defaultValue = "directory")
  @Placement(order = 2)
  @OfValues(DerbySubsubProtocolValueProvider.class)
  private String subsubProtocol;

  /**
   * Indicates if the database should be created if it does not exist.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Placement(order = 3)
  private boolean create;

  /**
   * Specifies a list of custom key-value connectionProperties for the config.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB)
  private Map<String, String> connectionProperties = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUrl() {
    StringBuilder buf = new StringBuilder();
    buf.append(JDBC_URL_PREFIX);

    if (subsubProtocol != null) {
      buf.append(":");
      buf.append(subsubProtocol);
    }
    if (database != null) {
      buf.append(":");
      buf.append(database);
    }

    if (create) {
      buf.append(";create=true");
    }

    if (connectionProperties != null) {
      connectionProperties.entrySet().forEach(entry -> {
        buf.append(";");
        buf.append(entry.getKey());
        buf.append("=");
        buf.append(entry.getValue());
      });
    }

    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDriverClassName() {
    return DERBY_DRIVER_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUser() {
    return null;
  }
}
