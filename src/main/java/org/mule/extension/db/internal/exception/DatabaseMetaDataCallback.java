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

import org.mule.extension.db.api.exception.MetaDataAccessException;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * A callback interface used by the ExceptionUtils class. Implementations of this
 * interface perform the actual work of extracting database meta-data, but
 * don't need to worry about exception handling. SQLExceptions will be caught
 * and handled correctly by the ExceptionUtils class.
 */
@FunctionalInterface
public interface DatabaseMetaDataCallback {

  /**
   * Implementations must implement this method to process the meta-data
   * passed in. Exactly what the implementation chooses to do is up to it.
   * @param dbmd the DatabaseMetaData to process
   * @return a result object extracted from the meta-data
   * (can be an arbitrary object, as needed by the implementation)
   * @throws SQLException if an SQLException is encountered getting
   * column values (that is, there's no need to catch SQLException)
   * @throws MetaDataAccessException in case of other failures while
   * extracting meta-data (for example, reflection failure)
   */
  Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
