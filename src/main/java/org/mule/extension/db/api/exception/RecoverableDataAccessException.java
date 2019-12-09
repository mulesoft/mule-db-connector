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

import static org.mule.extension.db.api.exception.DatabaseErrors.RECOVERABLE_DATA_ACCESS_EXCEPTION;

/**
 * Data access exception thrown when a previously failed operation might be able
 * to succeed if the application performs some recovery steps and retries the entire
 * transaction or in the case of a distributed transaction, the transaction branch.
 * At a minimum, the recovery operation must include closing the current connection
 * and getting a new connection.
 *
 * @see java.sql.SQLRecoverableException
 */

public class RecoverableDataAccessException extends DataAccessException {

  public RecoverableDataAccessException(String message) {
    super(message, RECOVERABLE_DATA_ACCESS_EXCEPTION);
  }

  public RecoverableDataAccessException(String message, DatabaseErrors errorType) {
    super(message, errorType);
  }

  public RecoverableDataAccessException(String message, Throwable cause) {
    super(message, RECOVERABLE_DATA_ACCESS_EXCEPTION, cause);
  }

  public RecoverableDataAccessException(String message, DatabaseErrors errorType, Throwable cause) {
    super(message, errorType, cause);
  }
}
