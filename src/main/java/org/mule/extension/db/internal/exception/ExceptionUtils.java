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
import org.mule.extension.db.api.exception.connection.ConnectionClosingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.sql.DataSource;

public class ExceptionUtils {

  /**
   * Sort the given {@code String} array if necessary.
   * @param array the original array (potentially empty)
   * @return the array in sorted form (never {@code null})
   */
  public static String[] sortStringArray(String[] array) {
    if (array == null || array.length == 0) {
      return array;
    }

    Arrays.sort(array);
    return array;
  }

  /**
   * Call the specified method on DatabaseMetaData for the given DataSource,
   * and extract the invocation result.
   * @param dataSource the DataSource to extract meta-data for
   * @param metaDataMethodName the name of the DatabaseMetaData method to call
   * @return the object returned by the specified DatabaseMetaData method
   * @throws MetaDataAccessException if we couldn't access the DatabaseMetaData
   * or failed to invoke the specified method
   * @see java.sql.DatabaseMetaData
   */
  @SuppressWarnings("unchecked")
  public static <T> T extractDatabaseMetaData(DataSource dataSource, final String metaDataMethodName)
      throws MetaDataAccessException {

    return (T) extractDatabaseMetaData(dataSource,
                                       dbmd -> {
                                         try {
                                           return DatabaseMetaData.class.getMethod(metaDataMethodName).invoke(dbmd);
                                         } catch (NoSuchMethodException ex) {
                                           throw new MetaDataAccessException("No method named '" + metaDataMethodName +
                                               "' found on DatabaseMetaData instance [" + dbmd + "]", ex);
                                         } catch (IllegalAccessException ex) {
                                           throw new MetaDataAccessException(
                                                                             "Could not access DatabaseMetaData method '"
                                                                                 + metaDataMethodName + "'",
                                                                             ex);
                                         } catch (InvocationTargetException ex) {
                                           if (ex.getTargetException() instanceof SQLException) {
                                             throw (SQLException) ex.getTargetException();
                                           }
                                           throw new MetaDataAccessException(
                                                                             "Invocation of DatabaseMetaData method '"
                                                                                 + metaDataMethodName + "' failed",
                                                                             ex);
                                         }
                                       });
  }

  /**
   * Extract database meta-data via the given DatabaseMetaDataCallback.
   * <p>This method will open a connection to the database and retrieve the database meta-data.
   * Since this method is called before the exception translation feature is configured for
   * a datasource, this method can not rely on the SQLException translation functionality.
   * <p>Any exceptions will be wrapped in a MetaDataAccessException. This is a checked exception
   * and any calling code should catch and handle this exception. You can just log the
   * error and hope for the best, but there is probably a more serious error that will
   * reappear when you try to access the database again.
   * @param dataSource the DataSource to extract meta-data for
   * @param action callback that will do the actual work
   * @return object containing the extracted information, as returned by
   * the DatabaseMetaDataCallback's {@code processMetaData} method
   * @throws MetaDataAccessException if meta-data access failed
   */
  public static Object extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback action)
      throws MetaDataAccessException {

    Connection jdbcConnection = null;
    try {
      jdbcConnection = dataSource.getConnection();
      DatabaseMetaData metaData = jdbcConnection.getMetaData();
      if (metaData == null) {
        // should only happen in test environments
        throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + jdbcConnection + "] was null");
      }
      return action.processMetaData(metaData);
    } catch (SQLException ex) {
      throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
    } catch (AbstractMethodError err) {
      throw new MetaDataAccessException("JDBC DatabaseMetaData method not implemented by the JDBC driver, try upgrading it", err);
    } finally {
      try {
        jdbcConnection.close();
      } catch (SQLException e) {
        throw new ConnectionClosingException(e);
      }
    }
  }

  /**
   * Match a String against the given pattern, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   * @param pattern the pattern to match against
   * @param str the String to match
   * @return whether the String matches the given pattern
   */
  public static boolean simpleMatch(@Nullable String pattern, @Nullable String str) {
    if (pattern == null || str == null) {
      return false;
    }

    int firstIndex = pattern.indexOf('*');
    if (firstIndex == -1) {
      return pattern.equals(str);
    }

    if (firstIndex == 0) {
      if (pattern.length() == 1) {
        return true;
      }
      int nextIndex = pattern.indexOf('*', 1);
      if (nextIndex == -1) {
        return str.endsWith(pattern.substring(1));
      }
      String part = pattern.substring(1, nextIndex);
      if (part.isEmpty()) {
        return simpleMatch(pattern.substring(nextIndex), str);
      }
      int partIndex = str.indexOf(part);
      while (partIndex != -1) {
        if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
          return true;
        }
        partIndex = str.indexOf(part, partIndex + 1);
      }
      return false;
    }

    return (str.length() >= firstIndex &&
        pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) &&
        simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
  }

  /**
   * Match a String against the given patterns, supporting the following simple
   * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   * @param patterns the patterns to match against
   * @param str the String to match
   * @return whether the String matches any of the given patterns
   */
  public static boolean simpleMatch(@Nullable String[] patterns, String str) {
    if (patterns != null) {
      for (String pattern : patterns) {
        if (simpleMatch(pattern, str)) {
          return true;
        }
      }
    }
    return false;
  }

}
