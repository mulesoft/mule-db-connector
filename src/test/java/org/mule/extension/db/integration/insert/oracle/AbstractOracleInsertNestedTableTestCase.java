/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.insert.oracle;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.sql.SQLException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractOracleInsertNestedTableTestCase extends AbstractDbIntegrationTestCase {

  @Before
  public void init() throws SQLException {
    ((OracleTestDatabase) this.testDatabase).initUdts(getDefaultDataSource().getConnection());
  }

  @Parameterized.Parameter(4)
  public String flowSuffix;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/oracle/insert-nested-tables.xml"};
  }

  @Test
  public void insertAndSelect() throws Exception {
    flowRunner("insertNestedTables")
        .withVariable("id", 123)
        .withVariable("name", "Apple")
        .withVariable("quantity", 456)
        .run();

    Object selectNestedTables = flowRunner("selectNestedTables")
        .keepStreamsOpen()
        .run().getMessage().getPayload().getValue();

    CursorIteratorProvider result = (CursorIteratorProvider) selectNestedTables;
    CursorIterator cursorIterator = result.openCursor();

    while (cursorIterator.hasNext()) {
      Map<String, Object> row = (Map<String, Object>) cursorIterator.next();
      assertRow(row);
    }
  }

  protected abstract void assertRow(Map<String, Object> row);
}
