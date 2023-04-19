/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby.tracing;

import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getHostFrom;
import static org.mule.db.commons.internal.domain.connection.ConnectionTracingMetadataUtils.getProtocolFrom;

import static java.util.Optional.ofNullable;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionParameters;

import java.util.Optional;

/**
 * a {@link DbConnectionTracingMetadata} for derby.
 */
public class DerbyDbConnectionTracingMetadata implements DbConnectionTracingMetadata {

  public static final String DERBY_DB_SYSTEM = "DERBY";
  private final DerbyConnectionParameters derbyParameters;

  public DerbyDbConnectionTracingMetadata(DerbyConnectionParameters derbyParameters) {
    this.derbyParameters = derbyParameters;
  }

  @Override
  public String getDbSystem() {
    return DERBY_DB_SYSTEM;
  }

  @Override
  public String getConnectionString() {
    return derbyParameters.getUrl();
  }

  @Override
  public String getUser() {
    return derbyParameters.getUser();
  }

  @Override
  public Optional<String> getPeerName() {
    return ofNullable(getHostFrom(derbyParameters.getUrl()));
  }

  @Override
  public Optional<String> getPeerTransport() {
    return ofNullable(getProtocolFrom(derbyParameters.getUser()));
  }
}
