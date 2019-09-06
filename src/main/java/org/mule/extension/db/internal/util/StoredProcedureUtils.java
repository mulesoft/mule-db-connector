/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.util;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for stored procedure operations.
 *
 * @since 1.4.1
 */
public class StoredProcedureUtils {

  private static final String STORED_PROCEDURE_REGEX = "(?msi)(\\{\\s*)?call\\s+(\\w+\\.)?(\\w+)\\s*\\(.*";

  private final static Pattern storedProcedurePattern = Pattern.compile(STORED_PROCEDURE_REGEX);


  /**
   * Gets the name of the stored procedure of the given SQL Query.
   *
   * @param sqlText the SQL Query text
   * @return the name of the stored procedure
   * @throws SQLException if it is no possible to get name of the stored procedure from the given SQL Query or the
   *         SQL Query syntax is not valid
   */
  public static String getStoredProcedureName(String sqlText) throws SQLException {
    Matcher matcher = storedProcedurePattern.matcher(sqlText);

    if (!matcher.matches()) {
      throw new SQLException(format("Unable to detect stored procedure name from '%s'", sqlText));
    }

    return matcher.group(3);
  }

  /**
   * Gets the Schema of the stored procedure of the given SQL Query.
   *
   * @param sqlText the SQL Query text
   * @return an {@link Optional} with the Schema of the stored procedure
   * @throws SQLException if the SQL Query syntax is not valid
   */
  public static Optional<String> getStoreProcedureSchema(String sqlText) throws SQLException {
    Matcher matcher = storedProcedurePattern.matcher(sqlText);

    if (!matcher.matches()) {
      throw new SQLException(format("Unable to detect stored procedure schema from '%s'", sqlText));
    }

    String schemaText = matcher.group(2);

    if (isBlank(schemaText)) {
      return Optional.empty();
    } else {
      // Remove the dot at the end of the text
      String schemaName = schemaText.substring(0, schemaText.length() - 1);
      return Optional.of(schemaName);
    }
  }

  public static String getPackageName(String sqlText) throws SQLException {
    Matcher matcher = storedProcedurePattern.matcher(sqlText);

    if (!matcher.matches()) {
      return "";
    }

    return matcher.group(2);
  }

}
