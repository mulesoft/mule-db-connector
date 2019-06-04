/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static org.mule.extension.db.api.exception.connection.DbError.BAD_SQL_SYNTAX;
import static org.mule.extension.db.api.exception.connection.DbError.CONNECTIVITY;
import static org.mule.extension.db.api.exception.connection.DbError.QUERY_EXECUTION;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Generic {@link ErrorTypeProvider} for DB operations
 *
 * @since 1.0
 */
public class OperationErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(CONNECTIVITY);
    errors.add(BAD_SQL_SYNTAX);
    errors.add(QUERY_EXECUTION);

    return errors;
  }
}
