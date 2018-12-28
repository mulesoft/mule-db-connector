/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.transaction;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.core.api.event.CoreEvent;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;

public abstract class AbstractTxDbIntegrationTestCase extends AbstractDbIntegrationTestCase {

  protected static final String MARS = "Mars";
  protected static final String MERCURY = "Mercury";

  protected void executeTransaction(String flowName) throws Exception {
    FlowRunner flowRunner = flowRunner(flowName);
    additionalVariables().entrySet().forEach(entry -> flowRunner.withVariable(entry.getKey(), entry.getValue()));
    Exception exception = flowRunner.runExpectingException();
    assertThat(exception.getCause(), is(instanceOf(IllegalStateException.class)));
  }

  protected CoreEvent executeSucessfullTransaction(String flowName) throws Exception {
    FlowRunner flowRunner = flowRunner(flowName);
    additionalVariables().entrySet().forEach(entry -> flowRunner.withVariable(entry.getKey(), entry.getValue()));
    return flowRunner.run();
  }

  protected void validateDbState(String planet) throws java.sql.SQLException {
    DataSource dataSource = getDefaultDataSource();
    checkState(planet, dataSource);
  }

  protected void validateDbState(String planet, String configName) throws java.sql.SQLException {
    DataSource dataSource = getDataSource(configName);
    checkState(planet, dataSource);
  }

  protected void validateDbState(String planet, String configName, Map<String, Object> variables) throws java.sql.SQLException {
    DataSource dataSource = getDataSource(configName, variables);
    checkState(planet, dataSource);
  }

  private void checkState(String planet, DataSource dataSource) throws java.sql.SQLException {
    List<Map<String, Object>> result = selectData("select * from PLANET where POSITION=4", dataSource);
    Matcher<Map<? extends String, ?>> mapMatcher = hasEntry("NAME", planet);
    //Oracle returns BigDecimals
    Matcher<Map<? extends String, ?>> numberMatcher = hasEntry(is("POSITION"), is(anyOf(is(4), is(new BigDecimal(4)))));
    Matcher<Iterable<Map<String, Object>>> matcher = hasItems(mapMatcher, numberMatcher);
    assertThat(result, matcher);
  }


}
