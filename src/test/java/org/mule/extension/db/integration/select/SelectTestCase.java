/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.rules.ExpectedException.none;
import static org.mule.db.commons.api.exception.connection.DbError.BAD_SQL_SYNTAX;
import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getCountRecord;
import static org.mule.extension.db.integration.model.Planet.MARS;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Planet;
import org.mule.extension.db.integration.model.Record;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SelectTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-config.xml"};
  }

  @Test
  public void select() throws Exception {
    Message response = flowRunner("select").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void selectWithNullParams() throws Exception {
    Message response = flowRunner("selectWithNullParams").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void fixedParam() throws Exception {
    Message response = flowRunner("fixedParam").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void expressionAndFixedParamMixed() throws Exception {
    Message response = flowRunner("expressionAndFixedParamMixed").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getEarthRecord());
  }

  @Test
  public void dynamicQuery() throws Exception {
    Message response = flowRunner("dynamicQuery").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void maxRows() throws Exception {
    Message response = flowRunner("selectMaxRows").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getVenusRecord(), getEarthRecord());
  }

  @Test
  public void emptyResult() throws Exception {
    executeWithDataSources(dataSource -> testDatabase.truncateSpaceshipTable(dataSource.getConnection()));
    Message response = flowRunner("emptyResult").keepStreamsOpen().run().getMessage();
    assertThat(response.getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
    assertThat((((CursorIteratorProvider) response.getPayload().getValue()).openCursor()).hasNext(), is(false));
  }

  @Test
  public void namedParameter() throws Exception {
    Message response = flowRunner("selectParameterizedQuery").withPayload(MARS.getName()).keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void returnsAliasInResultSet() throws Exception {
    final String nameFieldAlias = "PLANETNAME";

    Message response = flowRunner("usesAlias").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, new Record(new Field(nameFieldAlias, Planet.VENUS.getName())),
                          new Record(new Field(nameFieldAlias, Planet.EARTH.getName())),
                          new Record(new Field(nameFieldAlias, Planet.MARS.getName())));
  }

  @Test
  public void invalidQueryType() throws Exception {
    assertErrorType(() -> flowRunner("invalidQueryType").run(), "DB", BAD_SQL_SYNTAX.name());
  }

  @Test
  public void badSqlSyntax() throws Exception {
    assertErrorType(() -> flowRunner("badSqlSyntax").run(), "DB", BAD_SQL_SYNTAX.name());
  }

  @Test
  public void selectNestedQuery() throws Exception {
    Message response = flowRunner("selectNestedQuery").keepStreamsOpen().run().getMessage();

    assertThat(response.getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
    assertMessageContains(response, getCountRecord(3));
  }


  @Test
  public void selectDoubleNestedQuery() throws Exception {
    Message response = flowRunner("selectDoubleNestedQuery").keepStreamsOpen().run().getMessage();

    assertThat(response.getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
    assertMessageContains(response, getCountRecord(3));
  }

  @Test
  public void selectNestedParametrizedFirstQuery() throws Exception {
    Message response = flowRunner("selectNestedParametrizedFirstQuery").keepStreamsOpen().run().getMessage();

    assertThat(response.getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
    assertMessageContains(response, getCountRecord(1));
  }


  @Test
  public void selectNestedParametrizedSecondQuery() throws Exception {
    Message response = flowRunner("selectNestedParametrizedSecondQuery").keepStreamsOpen().run().getMessage();

    assertThat(response.getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
    assertMessageContains(response, getCountRecord(1));
  }

  private void assertErrorType(Callable task, String errorNamespace, String errorIdentifier) throws Exception {
    expectedError.expectErrorType(errorNamespace, errorIdentifier);
    task.call();
  }
}
