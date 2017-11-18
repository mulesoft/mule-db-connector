/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.DbTestUtil.DbType.SQLSERVER;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractQueryTimeoutTestCase extends AbstractDbIntegrationTestCase {

  public static final String QUERY_TIMEOUT_FLOW = "queryTimeout";

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  /**
   * Verifies that queryTimeout is used and query execution is aborted with an error. As different DB drivers thrown different
   * type of exceptions instead of throwing SQLTimeoutException, the test firsts executes the flow using no timeout, which must
   * pass, and then using a timeout which must fail. Because the first execution was successful is assumed that the error is
   * because of an aborted execution.
   *
   * @throws Exception
   */
  @Test
  public void timeoutsQuery() throws Exception {
    //TODO: SQL Server doesn't support delay inside functions.
    assumeThat(dbType, is(not(SQLSERVER)));
    CoreEvent responseEvent = flowRunner(QUERY_TIMEOUT_FLOW).withPayload(0).run();

    Message response = responseEvent.getMessage();
    assertThat(response.getPayload().getValue(), is(notNullValue()));

    expectedError.expectErrorType("DB", "QUERY_EXECUTION");
    flowRunner(QUERY_TIMEOUT_FLOW).withPayload(5).run();
  }

  @Before
  public void setupDelayFunction() throws Exception {
    testDatabase.createDelayFunction(getDefaultDataSource());
  }
}
