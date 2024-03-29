/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.connectivity;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.mule.extension.db.integration.TestDbConfig.getMySqlResource;

import java.util.List;

import org.junit.Before;
import org.junit.runners.Parameterized;

public class DataSourcePoolingMySqlTestCase extends DataSourcePoolingTestCase {

  private static final String DB_HOST_KEY = "db.host";
  private static final String DB_HOST_VALUE = "0.0.0.0";
  private static final String DB_PORT_KEY = "db.port";
  private static final String DB_PORT_VALUE = getProperty("mysql.db.port");

  @Before
  public void setUp() {
    setProperty(DB_HOST_KEY, DB_HOST_VALUE);
    setProperty(DB_PORT_KEY, DB_PORT_VALUE);
    setConcurrentRequests(2);
  }

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    return getMySqlResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/connectivity/mysql-db-pooling-config.xml",
        "integration/connectivity/connection-pooling-config.xml"};
  }

}
