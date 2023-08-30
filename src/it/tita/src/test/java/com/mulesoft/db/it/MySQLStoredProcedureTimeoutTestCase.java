/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.db.it;

import com.mulesoft.anypoint.tests.http.HttpResponse;
import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.environment.api.artifact.Identifier;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(Ambar.class)
public class MySQLStoredProcedureTimeoutTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(MySQLStoredProcedureTimeoutTestCase.class);

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  @Standalone(log4j = "log4j2-test.xml")
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    if (Boolean.parseBoolean(System.getProperty("mysql"))) {
      LOGGER.info("Setting MySQL configuration.");
      String mysqlPort = System.getProperty("mysql.db.port");
      LOGGER.trace("Port for MySQL database is: " + mysqlPort);
      return runtimeBuilder
            .custom("stored-procedure-mysql-timeout-app", "stored-procedure-mysql-timeout-app.xml")
            .withTemplatePomFile("stored-procedure-mysql-timeout-app-pom.xml")
            .withProperty("db.port", mysqlPort == null ? "3306" :mysqlPort)
            .withApi(api, port);
    } else {
      LOGGER.info("Setting default configuration.");
      return runtimeBuilder
              .custom("default-app", "default-app.xml")
              .withApi(api, port);
    }
  }

  @Test
  public void mySqlSPTimeoutTestCase() throws Exception {
    if (Boolean.parseBoolean(System.getProperty("mysql"))) {
      LOGGER.info("MySQL testing begins.");
      runtime.api(api).request("/test-sp-timeout").post();

      HttpResponse responseApi = runtime.api(api).request("/test-sp-timeout").get();
      assertThat(responseApi.statusCode(), is(SC_OK));

      assertThat(responseApi.asString(), containsString("ERROR"));
      assertThat(responseApi.asString(), containsString("Statement cancelled due to timeout"));
    } else {
      LOGGER.warn("The mysql system property is set to false, no tests are performed.");
    }
  }
}
