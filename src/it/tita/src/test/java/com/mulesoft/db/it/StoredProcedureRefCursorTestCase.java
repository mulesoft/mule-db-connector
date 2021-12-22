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
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(Ambar.class)
public class StoredProcedureRefCursorTestCase {

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  @Standalone(log4j = "log4j2-test.xml")
  private Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    return runtimeBuilder
            .custom("stored-procedure-oracle-reftype-app", "stored-procedure-oracle-reftype-app.xml")
            .withTemplatePomFile("stored-procedure-oracle-reftype-app-pom.xml")
            .withProperty("db.port", System.getProperty("oracle.db.port"))
            .withApi(api, port);
  }

  @Test
  public void oracleREFTypeTestCase() throws Exception {
    if (Boolean.parseBoolean(System.getProperty("oracle"))) {
      runtime.api(api).request("/test-cursor-ref").post();

      HttpResponse responseApi = runtime.api(api).request("/test-cursor-ref").get();
      assertThat(responseApi.statusCode(), is(SC_OK));
      assertThat(responseApi.asString(), containsString("SARASA"));
    }
  }
}
