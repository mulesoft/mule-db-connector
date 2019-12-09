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

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.db.api.exception.MetaDataAccessException;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.slf4j.Logger;

/**
 * Factory for creating {@link SQLErrorCodes} based on the
 * "databaseProductName" taken from the {@link java.sql.DatabaseMetaData}.
 *
 * <p>Returns {@code SQLErrorCodes} populated with vendor codes
 * defined in a resource file named "sql-error-codes.xml".
 *
 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
 */
@SuppressWarnings("unchecked")
public class SQLErrorCodesFactory {

  private static final Logger LOGGER = getLogger(DbConnectionProvider.class);

  /**
   * The name of default SQL error code files, loading from the class path.
   */
  public static final String SQL_ERROR_CODE_DEFAULT_PATH = "errorCodes.xml";

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();

  /**
   * Return the singleton instance.
   */
  public static SQLErrorCodesFactory getInstance() {
    return instance;
  }

  /**
   * Map to hold error codes for all databases defined in the config file.
   * Key is the database product name, value is the SQLErrorCodes instance.
   */
  private final Map<String, SQLErrorCodes> errorCodesMap;

  /**
   * Map to cache the SQLErrorCodes instance per DataSource.
   */
  private final Map<DataSource, SQLErrorCodes> dataSourceCache = new WeakHashMap<>(16);

  /**
   * Create a new instance of the {@link SQLErrorCodesFactory} class.
   * <p>Not public to enforce Singleton design pattern. Would be private
   * except to allow testing via overriding the
   */
  protected SQLErrorCodesFactory() {
    Map<String, SQLErrorCodes> errorCodes;

    try {
      XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(SQL_ERROR_CODE_DEFAULT_PATH)));
      Object object = decoder.readObject();
      errorCodes = (Map<String, SQLErrorCodes>) object;
      decoder.close();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("SQLErrorCodes loaded: " + errorCodes.keySet());
      }
    } catch (FileNotFoundException e) {
      LOGGER.debug("Default sql-error-codes.xml file was not found.");
      errorCodes = Collections.emptyMap();
    } catch (ClassCastException e) {
      LOGGER.warn("The object that was deserialized from sql-error-codes.xml could not be cast to a Map");
      errorCodes = Collections.emptyMap();
    }
    this.errorCodesMap = errorCodes;
  }

  /**
   * Return the {@link SQLErrorCodes} instance for the given database.
   * <p>No need for a database meta-data lookup.
   * @param databaseName the database name (must not be {@code null})
   * @return the {@code SQLErrorCodes} instance for the given database
   * @throws IllegalArgumentException if the supplied database name is {@code null}
   */
  public SQLErrorCodes getErrorCodes(String databaseName) {
    if (databaseName == null) {
      throw new IllegalArgumentException("Database product name must not be null");
    }

    SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
    if (sec == null) {
      for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
        if (ExceptionUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
          sec = candidate;
          break;
        }
      }
    }
    if (sec != null) {
      checkCustomTranslatorRegistry(databaseName, sec);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("SQL error codes for '" + databaseName + "' found");
      }
      return sec;
    }

    // Could not find the database among the defined ones.
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("SQL error codes for '" + databaseName + "' not found");
    }
    return new SQLErrorCodes();
  }

  /**
   * Return {@link SQLErrorCodes} for the given {@link DataSource},
   * evaluating "databaseProductName" from the
   * {@link java.sql.DatabaseMetaData}, or an empty error codes
   * instance if no {@code SQLErrorCodes} were found.
   * @param dataSource the {@code DataSource} identifying the database
   * @return the corresponding {@code SQLErrorCodes} object
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public SQLErrorCodes getErrorCodes(DataSource dataSource) {
    if (dataSource == null) {
      throw new IllegalArgumentException("DataSource must not be null");
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
    }

    // Try efficient lock-free access for existing cache entry
    SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
    if (sec == null) {
      synchronized (this.dataSourceCache) {
        // Double-check within full dataSourceCache lock
        sec = this.dataSourceCache.get(dataSource);
        if (sec == null) {
          // We could not find it - got to look it up.
          try {
            String name = ExceptionUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName");
            if (name != null && !name.isEmpty()) {
              return registerDatabase(dataSource, name);
            }
          } catch (MetaDataAccessException ex) {
            LOGGER.warn("Error while extracting database name - falling back to empty error codes", ex);
          }
          // Fallback is to return an empty SQLErrorCodes instance.
          return new SQLErrorCodes();
        }
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
    }

    return sec;
  }

  /**
   * Associate the specified database name with the given {@link DataSource}.
   * @param dataSource the {@code DataSource} identifying the database
   * @param databaseName the corresponding database name as stated in the error codes
   * definition file (must not be {@code null})
   * @return the corresponding {@code SQLErrorCodes} object (never {@code null})
   * @see #unregisterDatabase(DataSource)
   */
  public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
    SQLErrorCodes sec = getErrorCodes(databaseName);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Caching SQL error codes for DataSource [" + identify(dataSource) +
          "]: database product name is '" + databaseName + "'");
    }
    this.dataSourceCache.put(dataSource, sec);
    return sec;
  }

  /**
   * Clear the cache for the specified {@link DataSource}, if registered.
   * @param dataSource the {@code DataSource} identifying the database
   * @return the corresponding {@code SQLErrorCodes} object that got removed,
   * or {@code null} if not registered
   * @since 4.3.5
   * @see #registerDatabase(DataSource, String)
   */
  @Nullable
  public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
    return this.dataSourceCache.remove(dataSource);
  }

  /**
   * Build an identification String for the given {@link DataSource},
   * primarily for logging purposes.
   * @param dataSource the {@code DataSource} to introspect
   * @return the identification String
   */
  private String identify(DataSource dataSource) {
    return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
  }

  /**
   * Check the {@link CustomSQLExceptionTranslatorRegistry} for any entries.
   */
  private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
    SQLExceptionTranslator customTranslator =
        CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
    if (customTranslator != null) {
      if (errorCodes.getCustomSqlExceptionTranslator() != null && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Overriding already defined custom translator '" +
            errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName() +
            " with '" + customTranslator.getClass().getSimpleName() +
            "' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
      } else if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Using custom translator '" + customTranslator.getClass().getSimpleName() +
            "' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
      }
      errorCodes.setCustomSqlExceptionTranslator(customTranslator);
    }
  }

}

