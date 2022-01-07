package org.mule.extension.db.internal.domain.connection;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionUtils {

  /** Tells whether a connection is an Oracle connection.
   *
   * @param connection    the connection to check
   * @return true if it's an Oracle connection
   */
  public static boolean isOracle(Connection connection) throws SQLException {
    return connection.getMetaData().getDatabaseProductName().equals("Oracle");
  }

  private ConnectionUtils() {}
}
