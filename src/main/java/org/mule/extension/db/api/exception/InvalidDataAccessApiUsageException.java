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

import static org.mule.extension.db.api.exception.DatabaseErrors.INVALID_DATA_ACCESS_API_USAGE_EXCEPTION;

/**
 * Exception thrown on incorrect usage of the API, such as failing to
 * "compile" a query object that needed compilation before execution.
 *
 * <p>This represents a problem in our Java data access framework,
 * not the underlying data access infrastructure.
 */
public class InvalidDataAccessApiUsageException extends NonTransientDataAccessException {

  public InvalidDataAccessApiUsageException(String message) {
    super(message, INVALID_DATA_ACCESS_API_USAGE_EXCEPTION);
  }

  public InvalidDataAccessApiUsageException(String message, DatabaseErrors errorType) {
    super(message, errorType);
  }

  public InvalidDataAccessApiUsageException(String message, Throwable cause) {
    super(message, INVALID_DATA_ACCESS_API_USAGE_EXCEPTION, cause);
  }

  public InvalidDataAccessApiUsageException(String message, DatabaseErrors errorType, Throwable cause) {
    super(message, errorType, cause);
  }
}
