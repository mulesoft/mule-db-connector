/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.db.it;

import com.mulesoft.anypoint.tests.http.HttpResponse;
import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.environment.api.artifact.Identifier;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(Ambar.class)
public class OracleLobSelectInsideForeachTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleLobSelectInsideForeachTestCase.class);

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  @Standalone(log4j = "log4j2-test.xml")
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      LOGGER.info("Setting Oracle configuration.");
      String oraclePort = System.getProperty("oracle.db.port");
      LOGGER.trace("Port for Oracle database is: " + oraclePort);
      return runtimeBuilder
              .custom("stored-procedure-oracle-lob-foreach-app", "stored-procedure-oracle-lob-foreach-app.xml")
              .withTemplatePomFile("stored-procedure-oracle-lob-foreach-app-pom.xml")
              .withProperty("db.port", oraclePort == null ? "1521" : oraclePort)
              .withApi(api, port);
    } else {
      LOGGER.info("Setting default configuration.");
      return runtimeBuilder
              .custom("default-app", "default-app.xml")
              .withApi(api, port);
    }
  }

  @Test
  public void oracleLobSelectInsideForeachTestCase() throws Exception {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      LOGGER.info("Oracle testing begins.");
      runtime.api(api).request("/test-lob-foreach").post();

      HttpResponse responseApi = runtime.api(api).request("/test-lob-foreach").get();
      assertThat(responseApi.statusCode(), is(SC_OK));
      assertThat(responseApi.asString(), containsString("SENT"));
    }
  }
}
