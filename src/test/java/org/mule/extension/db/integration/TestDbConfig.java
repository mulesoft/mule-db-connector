/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.extension.db.integration.model.MySqlTestDatabase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.model.SqlServerTestDataBase;

import java.util.ArrayList;
import java.util.List;

public class TestDbConfig {

  static {
    USE_MYSQL = false; //getValueFor("mysql"); X
    USE_MSSQL_SERVER = false;//getValueFor("mssql"); check
    USE_DERBY = false;//getValueFor("derby"); check
    USE_ORACLE = true; //getValueFor("oracle");
  }

  private static boolean USE_DERBY;

  /**
   *  <strong>Developer Note:</strong>
   *  To run the MySQL tests you need to follow the following steps:
   *  <ul>
   *    <li>Set this USE_MYSQL flag to true</li>
   *    <li>Download a docker MySQL image: <code>docker pull mysql</code></li>
   *    <li>Start the container <code>docker run --name some-mysql -e MYSQL_ROOT_PASSWORD=mysql -d mysql:latest</code></li>
   *  </ul>
   *
   *  Alternately, you can enable the system property <code>mysql</code>.
   */
  private static boolean USE_MYSQL;

  /**
   * <strong>Developer Note:</strong>
   * To run the oracle tests you need to follow the following steps:
   * <ul>
   *  <li>Set this USE_ORACLE flag to true</li>
   *  <li>Download a docker Oracle image: <code>docker pull store/oracle/database-enterprise:12.2.0.1</code></li>
   *  <li>Start the container:<code>docker run -d -it --name oracle-db -p 1521:1521 store/oracle/database-enterprise:12.2.0.1</code> </li>
   *  <li>Install in your maven repository an <strong>oracle-jdbc-driver</strong> using the mvn install:install-file command</li>
   *  <li>Add the installed driver dependency to this project so the test can find it in the classpath</li>
   *  <li>Add the driver as a shared dependency on {@link DbArtifactClassLoaderRunnerConfig}</li>
   * </ul>
   *
   * Alternately, you can enable the system property oracle, and follow just the steps five and six above.
   */
  private static boolean USE_ORACLE;

  /**
   * <strong>Developer Note:</strong>
   * To run the MsSQL tests you need to follow the following steps:
   * <ul>
   *  <li>Set this USE_MSSQL_SERVER flag to true</li>
   *   <li>Download a docker MsSQL image: <code>docker pull microsoft/mssql-server-linux</code></li>
   *  <li>Start the container: <code>docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=yourStrong(!)Password' -p 1433:1433 -d microsoft/mssql-server-linux</code>
   * </ul>
   *
   * Alternately, you can enable the system property <code>mssql</code>.
   */
  private static boolean USE_MSSQL_SERVER;

  public static List<Object[]> getResources() {

    if (!(USE_DERBY || USE_MYSQL || USE_MSSQL_SERVER || USE_ORACLE)) {
      USE_DERBY = true;
    }

    List<Object[]> result = new ArrayList<>();

    result.addAll(getDerbyResource());
    result.addAll(getMySqlResource());
    result.addAll(getOracleResource());
    result.addAll(getSqlServerResource());

    return result;
  }

  public static List<Object[]> getDerbyResource() {
    if (USE_DERBY) {
      final DerbyTestDatabase derbyTestDatabase = new DerbyTestDatabase();
      return singletonList(new Object[] {"integration/config/derby-datasource.xml", derbyTestDatabase,
          derbyTestDatabase.getDbType(), emptyList()});
    } else {
      return emptyList();
    }
  }

  public static List<Object[]> getMySqlResource() {
    if (USE_MYSQL) {
      final MySqlTestDatabase mySqlTestDatabase = new MySqlTestDatabase();
      return singletonList(new Object[] {"integration/config/mysql-db-config.xml", mySqlTestDatabase,
          mySqlTestDatabase.getDbType(), emptyList()});
    } else {
      return emptyList();
    }
  }

  public static List<Object[]> getOracleResource() {
    if (USE_ORACLE) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      return singletonList(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList()});
    } else {
      return emptyList();
    }
  }

  public static List<Object[]> getSqlServerResource() {
    if (USE_MSSQL_SERVER) {
      final SqlServerTestDataBase sqlServerTestDataBase = new SqlServerTestDataBase();
      return singletonList(new Object[] {"integration/config/mssql-db-config.xml", sqlServerTestDataBase,
          sqlServerTestDataBase.getDbType(), singletonList("merge")});
    } else {
      return emptyList();
    }
  }

  private static Boolean getValueFor(String vendor) {
    return Boolean.valueOf(System.getProperty(vendor));
  }
}
