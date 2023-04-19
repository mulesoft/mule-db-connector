/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getHostFrom;
import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getProtocolFrom;
import static org.mule.extension.db.internal.domain.connection.datasource.NoConnectionMetadata.getMetadata;
import static org.slf4j.LoggerFactory.getLogger;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * A {@link DbConnectionTracingMetadata} based on the connection metadata
 */
public class ConnectionBasedDbConnectionTracingMetadata implements DbConnectionTracingMetadata {

  public static final String UNSET = "unset";
  private static final Logger LOGGER = getLogger(ConnectionBasedDbConnectionTracingMetadata.class);
  private final Connection connection;
  private DatabaseMetaData metadata;

  public ConnectionBasedDbConnectionTracingMetadata(Connection connection) {
    this.connection = connection;
  }

  @Override
  public String getDbSystem() {
    try {
      return getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      LOGGER.debug("Unable to retrieve connection metadata for tracing.", e);
    }

    return UNSET;
  }

  private DatabaseMetaData getMetaData() throws SQLException {
    // In case the information cannot be retrieved it will not be present.
    if (metadata == null) {
      try {
        metadata = connection.getMetaData();
      } catch (Throwable e) {
        LOGGER.debug("Unable to retrieve database metadata from connection", e);
        metadata = getMetadata();
      }

    }

    return metadata;
  }

  @Override
  public String getConnectionString() {
    try {
      return getMetaData().getURL();
    } catch (SQLException e) {
      LOGGER.debug("Unable to retrieve connection metadata for tracing.", e);
    }

    return UNSET;
  }

  @Override
  public String getUser() {
    try {
      return getMetaData().getUserName();
    } catch (SQLException e) {
      LOGGER.debug("Unable to retrieve connection metadata for tracing.", e);
    }

    return UNSET;
  }

  @Override
  public Optional<String> getPeerName() {
    try {
      return ofNullable(getHostFrom(getMetaData().getURL()));
    } catch (SQLException e) {
      LOGGER.debug("Unable to retrieve connection metadata for tracing.", e);
    }

    return empty();
  }

  @Override
  public Optional<String> getPeerTransport() {
    try {
      return ofNullable(getProtocolFrom(getMetaData().getURL()));
    } catch (SQLException e) {
      LOGGER.debug("Unable to retrieve connection metadata for tracing.", e);
    }

    return empty();
  }
}
