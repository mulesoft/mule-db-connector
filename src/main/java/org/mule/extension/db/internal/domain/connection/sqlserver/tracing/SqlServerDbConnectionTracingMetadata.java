/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.sqlserver.tracing;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.extension.db.internal.domain.connection.sqlserver.SqlServerConnectionParameters;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getHostFrom;
import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getProtocolFrom;

/**
 * a {@link DbConnectionTracingMetadata} for sql server.
 */
public class SqlServerDbConnectionTracingMetadata implements DbConnectionTracingMetadata {

  public static final String SQL_SERVER_DB_SYSTEM = "SQL_SERVER";
  private final SqlServerConnectionParameters connectionParameters;

  public SqlServerDbConnectionTracingMetadata(SqlServerConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;
  }

  @Override
  public String getDbSystem() {
    return SQL_SERVER_DB_SYSTEM;
  }

  @Override
  public String getConnectionString() {
    return connectionParameters.getUrl();
  }

  @Override
  public String getUser() {
    return connectionParameters.getUser();
  }

  @Override
  public Optional<String> getPeerName() {
    return ofNullable(getHostFrom(connectionParameters.getUrl()));
  }

  @Override
  public Optional<String> getPeerTransport() {
    return ofNullable(getProtocolFrom(connectionParameters.getUrl()));
  }
}
