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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registry for custom {@link SQLExceptionTranslator} instances associated with
 * specific databases allowing for overriding translation based on values contained in the configuration file
 * named "sql-error-codes.xml".
 *
 * @see SQLErrorCodesFactory
 */
public final class CustomSQLExceptionTranslatorRegistry {

  private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistry.class);

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();


  /**
   * Return the singleton instance.
   */
  public static CustomSQLExceptionTranslatorRegistry getInstance() {
    return instance;
  }


  /**
   * Map registry to hold custom translators specific databases.
   * Key is the database product name as defined in the
   * {@link SQLErrorCodesFactory}.
   */
  private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<>();


  /**
   * Create a new instance of the {@link CustomSQLExceptionTranslatorRegistry} class.
   * <p>Not public to enforce Singleton design pattern.
   */
  private CustomSQLExceptionTranslatorRegistry() {}


  /**
   * Register a new custom translator for the specified database name.
   * @param dbName the database name
   * @param translator the custom translator
   */
  public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
    SQLExceptionTranslator replaced = this.translatorMap.put(dbName, translator);
    if (logger.isDebugEnabled()) {
      if (replaced != null) {
        logger.debug("Replacing custom translator [" + replaced + "] for database '" + dbName +
            "' with [" + translator + "]");
      } else {
        logger.debug("Adding custom translator of type [" + translator.getClass().getName() +
            "] for database '" + dbName + "'");
      }
    }
  }

  /**
   * Find a custom translator for the specified database.
   * @param dbName the database name
   * @return the custom translator, or {@code null} if none found
   */
  @Nullable
  public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
    return this.translatorMap.get(dbName);
  }

}

