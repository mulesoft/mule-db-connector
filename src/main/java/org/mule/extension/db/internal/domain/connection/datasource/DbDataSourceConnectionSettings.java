/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.sql.DataSource;
import javax.sql.XADataSource;

/**
 * Groups DataSource related parameters
 *
 * @since 1.0
 */
public final class DbDataSourceConnectionSettings
        implements org.mule.db.commons.internal.domain.connection.datasource.DataSourceConnectionSettings {

  /**
   * Reference to a JDBC {@link DataSource} object. This object is typically created using Spring.
   * When using XA transactions, an {@link XADataSource} object must be provided.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private DataSource dataSourceRef;

  public DataSource getDataSourceRef() {
    return dataSourceRef;
  }

}