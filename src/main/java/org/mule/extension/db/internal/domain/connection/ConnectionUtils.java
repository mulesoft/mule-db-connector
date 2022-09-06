/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionUtils {

  /**
   * Oracle.
   */
  public static final String ORACLE = "Oracle";

  /**
   * Tells whether a connection is an Oracle connection.
   *
   * @param connection the connection to check
   * @return true if it's an Oracle connection
   */
  public static boolean isOracle(Connection connection) throws SQLException {
    return connection.getMetaData().getDatabaseProductName().equals(ORACLE);
  }

  private ConnectionUtils() {}
}
