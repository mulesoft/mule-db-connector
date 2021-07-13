/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.tita;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
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
import org.apache.maven.model.Dependency;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Alien;

@RunWith(Ambar.class)
public class OracleStoredProcedureXMLTypeTestCase {

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");
  private static final String CONTENT_TYPE_HEADER_VALUE = "application/xml";

  @Standalone
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    return runtimeBuilder
        .custom("stored-procedure-oracle-xmltype-app", "tita/stored-procedure-oracle-xmltype-app.xml")
        .withTemplatePomFile("tita/stored-procedure-oracle-xmltype-app-pom.xml")
        .withProperty("db.port", System.getProperty("oracle.db.port"))
        .withApi(api, port);
  }

  @Test
  public void oracleXMTypeTestCase() throws Exception {
    Alien firstAlien = AbstractTestDatabase.ALIEN_TEST_VALUES[1];

    runtime.api(api).request("/test").withPayload(firstAlien.getXml())
        .withHeader(CONTENT_TYPE, CONTENT_TYPE_HEADER_VALUE)
        .post();

    HttpResponse responseApi = runtime.api(api).request("/test").get();
    assertThat(responseApi.statusCode(), is(SC_OK));
    assertThat(responseApi.asString(), containsString("SUCCESS"));
  }

}
