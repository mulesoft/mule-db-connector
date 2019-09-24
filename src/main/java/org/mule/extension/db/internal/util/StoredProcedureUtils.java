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

  private static final String STORED_PROCEDURE_REGEX = "(?msi)(\\{\\s*)?call\\s+(\\w+\\.)?(\\w+\\.)?(\\w+)\\s*\\(.*";

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

    return matcher.group(4);
  }

  /**
   * Gets the owner of the stored procedure of the given SQL Query. Normally the owner will be a schema, although in
   * case of Oracle it can be a package.
   *
   * @param sqlText the SQL Query text
   * @return an {@link Optional} with the Schema of the stored procedure
   * @throws SQLException if the SQL Query syntax is not valid
   */
  public static Optional<String> getStoreProcedureOwner(String sqlText) throws SQLException {
    Matcher matcher = storedProcedurePattern.matcher(sqlText);

    if (!matcher.matches()) {
      throw new SQLException(format("Unable to detect stored procedure schema from '%s'", sqlText));
    }

    String firstPart = matcher.group(2);
    String secondPart = matcher.group(3);

    if (!isBlank(firstPart) && !isBlank(secondPart)) {
      String packageName = firstPart.substring(0, firstPart.length() - 1);
      return Optional.of(packageName);
    } else if (!isBlank(firstPart)) {
      String packageName = firstPart.substring(0, firstPart.length() - 1);
      return Optional.of(packageName);
    } else if (!isBlank(secondPart)) {
      String packageName = secondPart.substring(0, secondPart.length() - 1);
      return Optional.of(packageName);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Gets the owner of the owner of the stored procedure of the given SQL Query. This has sense only for Oracle, where
   * Stored procedures can be defined within packages which in turn will be defined under a schema.
   *
   * @param sqlText the SQL Query text
   * @return an {@link Optional} with the package of the stored procedure
   * @throws SQLException if the SQL Query syntax is not valid
   */
  public static Optional<String> getStoredProcedureParentOwner(String sqlText) throws SQLException {
    Matcher matcher = storedProcedurePattern.matcher(sqlText);

    if (!matcher.matches()) {
      throw new SQLException(format("Unable to detect stored procedure package from '%s'", sqlText));
    }

    String firstPart = matcher.group(2);
    String secondPart = matcher.group(3);

    if (!isBlank(firstPart) && !isBlank(secondPart)) {
      String packageName = secondPart.substring(0, secondPart.length() - 1);
      return Optional.of(packageName);
    } else {
      return Optional.empty();
    }
  }

}
