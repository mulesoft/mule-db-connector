/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import static org.mule.extension.db.api.exception.connection.DbError.BAD_SQL_SYNTAX;

import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Signals that the SQL text in a query was invalid
 *
 * @since 1.0
 */
@Deprecated
@ExcludeFromGeneratedCoverage
public class BadSqlSyntaxException extends ModuleException {

  public BadSqlSyntaxException(String message) {
    super(message, BAD_SQL_SYNTAX);
  }

  public BadSqlSyntaxException(String message, Throwable cause) {
    super(message, BAD_SQL_SYNTAX, cause);
  }
}
