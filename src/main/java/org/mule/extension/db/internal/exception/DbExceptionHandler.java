/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.exception;

import org.mule.extension.db.api.exception.connection.BadSqlSyntaxException;
import org.mule.extension.db.api.exception.connection.QueryExecutionException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;

/**
 * Translates {@link SQLException} into connector specific ones.
 *
 * @since 1.0
 */
public class DbExceptionHandler extends ExceptionHandler {

  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof ModuleException) {
      return e;
    }

    return getCauseOfType(e, SQLSyntaxErrorException.class)
        .map(cause -> (Exception) new BadSqlSyntaxException(e.getMessage(), e))
        .orElseGet(() -> getCauseOfType(e, SQLException.class)
            .map(cause -> (Exception) new QueryExecutionException(cause.getMessage(), cause))
            .orElse(e));
  }
}
