/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.util;

import static java.lang.String.format;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for stored procedure operations.
 *
 * @since 1.4.1
 */
public class StoredProcedureUtils {

  private final static Pattern storedProcedureMatcher = Pattern.compile("(?msi)(\\{\\s*)?call\\s+([\\w#]+\\.)?(\\w+)\\s*\\(.*");

  public static String getStoredProcedureName(String sqlText) throws SQLException {
    Matcher matcher = storedProcedureMatcher.matcher(sqlText);

    if (!matcher.matches()) {
      throw new SQLException(format("Unable to detect stored procedure name from '%s'", sqlText));
    }

    return matcher.group(3);
  }

}
