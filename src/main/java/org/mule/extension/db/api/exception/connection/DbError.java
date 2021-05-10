/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import org.mule.extension.db.internal.DbConnector;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * Errors definitions for {@link DbConnector}
 *
 * @since 1.0
 */
@Deprecated
public enum DbError implements ErrorTypeDefinition<DbError> {
  /**
   * Generic error for a connectivity issue with the Database
   */
  CONNECTIVITY(MuleErrors.CONNECTIVITY),

  /**
   * Database rejected the provided credentials
   */
  INVALID_CREDENTIALS(CONNECTIVITY),

  /**
   * Connection to the RDBMS was established, but the Database doesn't exist
   */
  INVALID_DATABASE(CONNECTIVITY),

  /**
   * Cannot establish a connection with the RDBMS
   */
  CANNOT_REACH(CONNECTIVITY),

  /**
   * Could not load the JDBC driver
   */
  CANNOT_LOAD_DRIVER(CONNECTIVITY),

  /**
   * The provided SQL query has bad syntax
   */
  BAD_SQL_SYNTAX,

  /**
   * There was an error executing the query
   */
  QUERY_EXECUTION;

  private ErrorTypeDefinition<? extends Enum<?>> parent;

  DbError(ErrorTypeDefinition<? extends Enum<?>> parent) {
    this.parent = parent;
  }

  DbError() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(parent);
  }
}
