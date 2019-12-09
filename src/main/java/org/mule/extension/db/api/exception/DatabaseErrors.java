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

package org.mule.extension.db.api.exception;

import org.mule.extension.db.internal.DbConnector;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

/**
 * Errors definitions for {@link DbConnector}
 *
 * @since 1.0
 */
public enum DatabaseErrors implements ErrorTypeDefinition<DatabaseErrors> {

  DATA_ACCESS_EXCEPTION,

  TRANSIENT_DATA_ACCESS_EXCEPTION(DATA_ACCESS_EXCEPTION),

  NON_TRANSIENT_DATA_ACCESS_EXCEPTION(DATA_ACCESS_EXCEPTION),

  SCRIPT_EXCEPTION(DATA_ACCESS_EXCEPTION),

  RECOVERABLE_DATA_ACCESS_EXCEPTION(DATA_ACCESS_EXCEPTION),

  CONCURRENCY_FAILURE_EXCEPTION(TRANSIENT_DATA_ACCESS_EXCEPTION),

  QUERY_TIMEOUT_EXCEPTION(TRANSIENT_DATA_ACCESS_EXCEPTION),

  TRANSIENT_DATA_ACCESS_RESOURCE_EXCEPTION(TRANSIENT_DATA_ACCESS_EXCEPTION),

  PESSIMISTIC_LOCKING_FAILURE_EXCEPTION(CONCURRENCY_FAILURE_EXCEPTION),

  CANNOT_ACQUIRE_LOCK_EXCEPTION(PESSIMISTIC_LOCKING_FAILURE_EXCEPTION),

  CANNOT_SERIALIZE_TRANSACTION_EXCEPTION(PESSIMISTIC_LOCKING_FAILURE_EXCEPTION),

  DEADLOCK_LOSER_DATA_ACCESS_EXCEPTION(PESSIMISTIC_LOCKING_FAILURE_EXCEPTION),

  INVALID_DATA_ACCESS_RESOURCE_USAGE_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  NON_TRANSIENT_DATA_ACCESS_RESOURCE_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  CLEANUP_FAILURE_DATA_ACCESS_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  DATA_INTEGRITY_VIOLATION_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  INVALID_DATA_ACCESS_API_USAGE_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  PERMISSION_DENIED_DATA_ACCESS_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  UNCATEGORIZED_DATA_ACCESS_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_EXCEPTION),

  UNCATEGORIZED_SQL_EXCEPTION(UNCATEGORIZED_DATA_ACCESS_EXCEPTION),

  INVALID_RESULTSET_ACCESS_EXCEPTION(INVALID_DATA_ACCESS_RESOURCE_USAGE_EXCEPTION),

  BAD_SQL_GRAMMAR_EXCEPTION(INVALID_DATA_ACCESS_RESOURCE_USAGE_EXCEPTION),

  DUPLICATE_KEY_EXCEPTION(DATA_INTEGRITY_VIOLATION_EXCEPTION),

  DATA_ACCESS_RESOURCE_FAILURE_EXCEPTION(NON_TRANSIENT_DATA_ACCESS_RESOURCE_EXCEPTION);

  private ErrorTypeDefinition<? extends Enum<?>> parent;

  DatabaseErrors(ErrorTypeDefinition<? extends Enum<?>> parent) {
    this.parent = parent;
  }

  DatabaseErrors() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(parent);
  }
}
