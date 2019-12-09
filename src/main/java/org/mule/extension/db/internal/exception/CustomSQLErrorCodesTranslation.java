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

import static org.mule.extension.db.internal.exception.ExceptionUtils.sortStringArray;

import org.mule.extension.db.api.exception.DataAccessException;

import javax.annotation.Nullable;

/**
 * JavaBean for holding custom JDBC error codes translation for a particular
 * database. The "exceptionClass" property defines which exception will be
 * thrown for the list of error codes specified in the errorCodes property.
 *
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class CustomSQLErrorCodesTranslation {

  private String[] errorCodes = new String[0];

  @Nullable
  private Class<?> exceptionClass;

  /**
   * Set the SQL error codes to match.
   */
  public void setErrorCodes(String... errorCodes) {
    this.errorCodes = sortStringArray(errorCodes);
  }

  /**
   * Return the SQL error codes to match.
   */
  public String[] getErrorCodes() {
    return this.errorCodes;
  }

  /**
   * Set the exception class for the specified error codes.
   */
  public void setExceptionClass(@Nullable Class<?> exceptionClass) {
    if (exceptionClass != null && !DataAccessException.class.isAssignableFrom(exceptionClass)) {
      throw new IllegalArgumentException("Invalid exception class [" + exceptionClass +
          "]: needs to be a subclass of [DataAccessException]");
    }
    this.exceptionClass = exceptionClass;
  }

  /**
   * Return the exception class for the specified error codes.
   */
  @Nullable
  public Class<?> getExceptionClass() {
    return this.exceptionClass;
  }

}

