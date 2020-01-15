/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.config;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.runtime.api.config.DatabasePoolingProfile;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.display.Example;

import java.util.Objects;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Pooling configuration for JDBC Data Sources capable of pooling connections
 *
 * @since 1.0
 */
@Alias("pooling-profile")
public class DbPoolingProfile implements DatabasePoolingProfile {

  /**
   * Maximum number of connections a pool maintains at any given time
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Expression(NOT_SUPPORTED)
  private int maxPoolSize = 5;

  /**
   * Minimum number of connections a pool maintains at any given time
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private int minPoolSize = 0;

  /**
   * Determines how many connections at a time to try to acquire when the pool is exhausted
   */
  @Parameter
  @Optional(defaultValue = "1")
  @Expression(NOT_SUPPORTED)
  private int acquireIncrement = 1;

  /**
   * Determines how many statements are cached per pooled connection. Setting this to zero will disable statement caching
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Expression(NOT_SUPPORTED)
  private int preparedStatementCacheSize = 5;

  /**
   * The amount of time a client trying to obtain a connection waits for it to be acquired when the pool is
   * exhausted. Zero (default) means wait indefinitely
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private int maxWait = 0;

  /**
   * A {@link TimeUnit} which qualifies the {@link #maxWait}.
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Expression(NOT_SUPPORTED)
  private TimeUnit maxWaitUnit;

  /**
   * Determines how many seconds a Connection can remain pooled but unused before being discarded.
   * Zero means idle connections never expire.
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private int maxIdleTime;

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB)
  @Summary("Additional properties used to configure pooling profile. Does not support expressions.")
  @Example("#[{ \"idleConnectionTestPeriod\" : 100 }]")
  private Map<String, Object> additionalProperties = emptyMap();

  @Override
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  @Override
  public int getMinPoolSize() {
    return minPoolSize;
  }

  @Override
  public int getAcquireIncrement() {
    return acquireIncrement;
  }

  @Override
  public int getPreparedStatementCacheSize() {
    return preparedStatementCacheSize;
  }

  @Override
  public int getMaxWait() {
    return maxWait;
  }

  @Override
  public TimeUnit getMaxWaitUnit() {
    return maxWaitUnit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(minPoolSize, maxPoolSize, acquireIncrement, preparedStatementCacheSize, maxWaitUnit, maxWait);
  }

  public int getMaxIdleTime() {
    return maxIdleTime;
  }

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DbPoolingProfile)) {
      return false;
    }

    DbPoolingProfile that = (DbPoolingProfile) obj;

    return maxPoolSize == that.maxPoolSize &&
        minPoolSize == that.minPoolSize &&
        acquireIncrement == that.acquireIncrement &&
        preparedStatementCacheSize == that.preparedStatementCacheSize &&
        maxWait == that.maxWait &&
        maxWaitUnit == that.maxWaitUnit;
  }
}
