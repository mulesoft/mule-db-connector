/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
//@formatter:off
/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
//@formatter:on

package org.mule.extension.db.internal.exception;

import org.mule.extension.db.api.exception.BadSqlGrammarException;
import org.mule.extension.db.api.exception.ConcurrencyFailureException;
import org.mule.extension.db.api.exception.DataAccessException;
import org.mule.extension.db.api.exception.DataAccessResourceFailureException;
import org.mule.extension.db.api.exception.DataIntegrityViolationException;
import org.mule.extension.db.api.exception.InvalidDataAccessApiUsageException;
import org.mule.extension.db.api.exception.PermissionDeniedDataAccessException;
import org.mule.extension.db.api.exception.QueryTimeoutException;
import org.mule.extension.db.api.exception.RecoverableDataAccessException;
import org.mule.extension.db.api.exception.TransientDataAccessResourceException;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;

import javax.annotation.Nullable;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link java.sql.SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLRecoverableException
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

  public SQLExceptionSubclassTranslator() {
    setFallbackTranslator(new SQLStateSQLExceptionTranslator());
  }

  @Override
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    if (ex instanceof SQLTransientException) {
      if (ex instanceof SQLTransientConnectionException) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLTransactionRollbackException) {
        return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLTimeoutException) {
        return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
      }
    } else if (ex instanceof SQLNonTransientException) {
      if (ex instanceof SQLNonTransientConnectionException) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLDataException) {
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLIntegrityConstraintViolationException) {
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLInvalidAuthorizationSpecException) {
        return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLSyntaxErrorException) {
        return new BadSqlGrammarException(buildMessage(task, sql, ex), ex);
      } else if (ex instanceof SQLFeatureNotSupportedException) {
        return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
      }
    } else if (ex instanceof SQLRecoverableException) {
      return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
    }

    // Fallback to Spring's own SQL state translation...
    return null;
  }

}
