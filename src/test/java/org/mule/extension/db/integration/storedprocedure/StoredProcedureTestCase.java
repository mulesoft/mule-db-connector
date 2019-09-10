/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.functional.api.exception.ExpectedError;

import java.util.Map;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StoredProcedureTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureDoubleMyInt(getDefaultDataSource());
    testDatabase.createStoredProcedureConcatenateStrings(getDefaultDataSource());
    testDatabase.createStoredProcedureCountRecords(getDefaultDataSource());
    testDatabase.createStoredProcedureMultiplyInts(getDefaultDataSource());
    testDatabase.returnNullValue(getDefaultDataSource());
    testDatabase.createStoredProcedureAddOne(getDefaultDataSource());
    testDatabase.createStoredProcedureAddOneDefaultSchema(getDefaultDataSource());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-config.xml"};
  }

  @Test
  public void inOutParameterWithoutSpaceBeforeCall() throws Exception {
    Map<String, Object> payload = runProcedure("inOutParameterWithoutSpaceBeforeCall");

    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)

    assertThat("6", equalTo(payload.get("myInt").toString()));
  }

  @Test
  public void inOutParameter() throws Exception {
    Map<String, Object> payload = runProcedure("inOutParameter");

    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)

    assertThat("6", equalTo(payload.get("myInt").toString()));
  }

  @Test
  public void multipliesIntegers() throws Exception {
    Map<String, Object> payload = runProcedure("multiplyInts");

    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)
    assertThat(payload.get("result1").toString(), IsEqual.equalTo("12"));
    assertThat(payload.get("result2").toString(), IsEqual.equalTo("60"));
  }

  @Test
  public void returnNullValue() throws Exception {
    Map<String, Object> payload = runProcedure("returnNullValue");
    assertThat(payload.get("result"), is(nullValue()));
  }

  @Test
  public void concatenatesStrings() throws Exception {
    Map<String, Object> payload = runProcedure("concatenateStrings");

    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    assertThat(payload.get("result"), equalTo("foobar"));
  }

  @Test
  public void outParam() throws Exception {
    Map<String, Object> payload = runProcedure("outParam");

    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // assertThat((Integer) payload.get("updateCount1"), equalTo(0));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)

    assertThat("3", equalTo(payload.get("count").toString()));
  }

  @Test
  public void runStoredProcedureWithArgumentThatDoesNotExists() throws Exception {
    expectedError.expectErrorType("DB", "QUERY_EXECUTION");
    runProcedure("callNotExistingStoredProcedureWithAnArgument");
  }

  @Test
  public void runStoredProcedureSpecifyingSchema() throws Exception {
    Map<String, Object> payload = runProcedure("addOne");
    assertThat("7", equalTo(payload.get("num").toString()));
  }

}
