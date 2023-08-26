/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.model.derbyutil.DerbyTestStoredProcedure.NULL_PLANET_NAME;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class StoredProcedureParameterizedUpdateTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-parameterized-update-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureParameterizedUpdateTestType1(getDefaultDataSource());
  }

  @Test
  public void update() throws Exception {
    Map<String, Object> payload = runProcedure("update", "foo");
    assertPlanetUpdated(payload, "foo");
  }

  @Test
  public void storedProcedureWithNullArgument() throws Exception {
    Map<String, Object> payload = runProcedure("update", null);
    assertPlanetUpdated(payload, NULL_PLANET_NAME);
  }

  private void assertPlanetUpdated(Map<String, Object> payload, String expectedPlanetName) throws java.sql.SQLException {
    assertThat(payload.size(), is(1));
    int expectedUpdateCount = testDatabase instanceof DerbyTestDatabase ? 0 : 1;
    assertThat(payload.get("updateCount1"), equalTo(expectedUpdateCount));

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", expectedPlanetName), new Field("POSITION", 4)));
  }
}
