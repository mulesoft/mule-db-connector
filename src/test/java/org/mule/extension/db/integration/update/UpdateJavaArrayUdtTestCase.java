/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static java.util.Collections.emptyList;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.AllureConstants.DbFeature.DB_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.ContactDetails;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runners.Parameterized;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(DB_EXTENSION)
@Story("Update Statement")
public class UpdateJavaArrayUdtTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList()});
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-udt-array-config.xml"};
  }

  @Test
  public void updatesStringArray() throws Exception {
    Message response = flowRunner("updatesStringArray").run().getMessage();
    assertThat(response.getPayload().getValue(), Matchers.<Object>equalTo(new Object[] {"93101", "97201", "99210"}));
  }

  @Test
  public void updatesMappedObjectArray() throws Exception {
    Message response = flowRunner("updatesStructArray").run().getMessage();

    assertThat(response.getPayload().getValue(), instanceOf(Object[].class));
    final Object[] arrayPayload = (Object[]) response.getPayload().getValue();

    assertThat(arrayPayload.length, is(1));
    assertThat(arrayPayload[0], equalTo(new ContactDetails("work", "2-222-222", "2@2222.com")));
  }

}
