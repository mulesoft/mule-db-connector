/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import static org.mule.extension.db.api.exception.connection.DbError.BAD_SQL_SYNTAX;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Signals that the SQL text in a query was invalid
 *
 * @since 1.0
 * @deprecated since 1.9.4. Replace with equivalent on mule-db-client. To be removed in the next major (2.0).
 */
@Deprecated
public class BadSqlSyntaxException extends ModuleException {

  public BadSqlSyntaxException(String message) {
    super(message, BAD_SQL_SYNTAX);
  }

  public BadSqlSyntaxException(String message, Throwable cause) {
    super(message, BAD_SQL_SYNTAX, cause);
  }
}
