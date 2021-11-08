/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.tita;

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

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(Ambar.class)
public class OracleInsertSYSXMLTypeTestCase {

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  @Standalone(log4j = "tita/log4j2-tita-test.xml")
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    return runtimeBuilder
        .custom("insert-oracle-sys-xmltype-app", "tita/insert-oracle-sys-xmltype-app.xml")
        .withTemplatePomFile("tita/insert-oracle-sys-xmltype-app-pom.xml")
        .withProperty("db.port", System.getProperty("oracle.db.port"))
        .withApi(api, port);
  }

  @Test
  public void oracleXMTypeInsertTestCase() throws Exception {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      runtime.api(api).request("/test-insert").post();

      HttpResponse responseApi = runtime.api(api).request("/test-insert").get();
      assertThat(responseApi.statusCode(), is(SC_OK));
      assertThat(responseApi.asString(), containsString("SUCCESS"));
    }
  }
}
