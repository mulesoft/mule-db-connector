/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db;

import java.util.Properties;

public class DbMunitUtils {

  private static final String DEFAULT_PORT = "100";

  public static boolean isTestIgnored(String dbName) {
    Properties availableProperties = System.getProperties();

    return !availableProperties.containsKey(dbName);
  }

  public static String getDbPort(String dbName) {
    Properties availableProperties = System.getProperties();
    String port = String.format("%s.db.port", dbName);

    return availableProperties.containsKey(port) ? availableProperties.getProperty(port) : DEFAULT_PORT;
  }

  public static String getPostgresqlConnectionString(String host, String database) {
    String port = getDbPort("postgresql");
    return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
  }

  public static String getSecureDbPort(String dbName) {
    Properties availableProperties = System.getProperties();
    String port = String.format("%s.db.mtls.port", dbName);

    return availableProperties.containsKey(port) ? availableProperties.getProperty(port) : DEFAULT_PORT;
  }
}
