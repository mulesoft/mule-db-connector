/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql.tracing;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionParameters;

import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getHostFrom;
import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getProtocolFrom;

import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * a {@link DbConnectionTracingMetadata} for mysql.
 */
public class MySqlConnectionTracingMetadata implements DbConnectionTracingMetadata {

  public static final String MYSQL = "mysql";
  private final MySqlConnectionParameters mySqlParameter;

  public MySqlConnectionTracingMetadata(MySqlConnectionParameters mySqlParameters) {
    this.mySqlParameter = mySqlParameters;
  }

  @Override
  public String getDbSystem() {
    return MYSQL;
  }

  @Override
  public String getConnectionString() {
    return mySqlParameter.getUrl();
  }

  @Override
  public String getUser() {
    return mySqlParameter.getUser();
  }

  @Override
  public Optional<String> getPeerName() {
    return ofNullable(getHostFrom(mySqlParameter.getUrl()));
  }

  @Override
  public Optional<String> getPeerTransport() {
    return ofNullable(getProtocolFrom(mySqlParameter.getUrl()));
  }
}
