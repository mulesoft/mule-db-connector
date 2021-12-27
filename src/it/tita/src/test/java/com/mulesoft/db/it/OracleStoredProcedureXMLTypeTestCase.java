/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.db.it;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.apache.http.HttpStatus.SC_OK;

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

@RunWith(Ambar.class)
public class OracleStoredProcedureXMLTypeTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleStoredProcedureXMLTypeTestCase.class);

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  @Standalone(log4j = "log4j2-test.xml")
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      LOGGER.info("Setting Oracle configuration.");
      return runtimeBuilder
          .custom("stored-procedure-oracle-xmltype-app", "stored-procedure-oracle-xmltype-app.xml")
          .withTemplatePomFile("stored-procedure-oracle-xmltype-app-pom.xml")
          .withProperty("db.port", System.getenv("oracle.db.port") == null ? "1521" : System.getenv("oracle.db.port"))
          .withApi(api, port);
    } else {
      LOGGER.info("Setting default configuration.");
      return runtimeBuilder
          .custom("default-app", "default-app.xml")
          .withApi(api, port);
    }
  }

  @Test
  public void oracleXMTypeTestCase() throws Exception {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      LOGGER.info("Oracle testing begins.");
      runtime.api(api).request("/test").post();

      HttpResponse responseApi = runtime.api(api).request("/test").get();
      assertThat(responseApi.statusCode(), is(SC_OK));
      assertThat(responseApi.asString(), containsString("SUCCESS"));
    } else {
      runtime.api(api).request("/hello").post();

      HttpResponse responseApi = runtime.api(api).request("/hello").get();
      assertThat(responseApi.statusCode(), is(SC_OK));
      assertThat(responseApi.asString(), containsString("Uh, Yeah Hi"));
    }
  }

}
