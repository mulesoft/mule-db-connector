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

import static org.mule.extension.db.api.exception.DatabaseErrors.CLEANUP_FAILURE_DATA_ACCESS_EXCEPTION;

/**
 * Exception thrown when we couldn't cleanup after a data access operation,
 * but the actual operation went OK.
 *
 * <p>For example, this exception or a subclass might be thrown if a JDBC
 * Connection couldn't be closed after it had been used successfully.
 *
 * <p>Note that data access code might perform resources cleanup in a
 * finally block and therefore log cleanup failure rather than rethrow it,
 * to keep the original data access exception, if any.
 */
public class CleanupFailureDataAccessException extends NonTransientDataAccessException {

  public CleanupFailureDataAccessException(String message) {
    super(message, CLEANUP_FAILURE_DATA_ACCESS_EXCEPTION);
  }

  public CleanupFailureDataAccessException(String message, DatabaseErrors errorType) {
    super(message, errorType);
  }

  public CleanupFailureDataAccessException(String message, Throwable cause) {
    super(message, CLEANUP_FAILURE_DATA_ACCESS_EXCEPTION, cause);
  }

  public CleanupFailureDataAccessException(String message, DatabaseErrors errorType, Throwable cause) {
    super(message, errorType, cause);
  }
}
