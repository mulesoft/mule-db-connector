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

import org.mule.extension.db.api.exception.DataAccessException;
import org.mule.extension.db.api.exception.UncategorizedSQLException;

import java.sql.SQLException;

/**
 * Strategy interface for translating between {@link SQLException SQLExceptions}
 * and Spring's data access strategy-agnostic {@link DataAccessException}
 * hierarchy.
 *
 * <p>Implementations can be generic (for example, using
 * {@link java.sql.SQLException#getSQLState() SQLState} codes for JDBC) or wholly
 * proprietary (for example, using Oracle error codes) for greater precision.
 *
 * @see DataAccessException
 */
@FunctionalInterface
public interface SQLExceptionTranslator {

  /**
   * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
   * <p>The returned DataAccessException is supposed to contain the original
   * {@code SQLException} as root cause. However, client code may not generally
   * rely on this due to DataAccessExceptions possibly being caused by other resource
   * APIs as well. That said, a {@code getRootCause() instanceof SQLException}
   * check (and subsequent cast) is considered reliable when expecting JDBC-based
   * access to have happened.
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (if known)
   * @param ex the offending {@code SQLException}
   * @return the DataAccessException wrapping the {@code SQLException},
   * or {@code null} if no translation could be applied
   * (in a custom translator; the default translators always throw an
   * {@link UncategorizedSQLException} in such a case)
   */
  DataAccessException translate(String task, String sql, SQLException ex);

}
