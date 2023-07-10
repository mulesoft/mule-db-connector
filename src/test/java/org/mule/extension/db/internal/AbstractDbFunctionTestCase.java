/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.db.commons.internal.domain.connection.DefaultDbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class AbstractDbFunctionTestCase extends AbstractMuleTestCase {

  static final String TYPE_NAME = "TEST_ARRAY";
  static final String TYPE_NAME_WITH_OWNER = "OWNER.TEST_ARRAY";

  static final String ATTR_TYPE_NAME_PARAM = "ATTR_TYPE_NAME";

  static final String ATTR_NO_PARAM = "ATTR_NO";

  static final String QUERY_TYPE_ATTRS =
      "SELECT ATTR_NO, ATTR_TYPE_NAME FROM ALL_TYPE_ATTRS WHERE TYPE_NAME = ? AND ATTR_TYPE_NAME IN ('CLOB', 'BLOB')";

  static final String QUERY_OWNER_CONDITION = " AND OWNER = ?";

  private static final int DATA_TYPE_INDEX = 5;
  private static final int ATTR_TYPE_NAME_INDEX = 6;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DefaultDbConnection mockDefaultDbConnectionMetadata(Connection delegate, int dataType, String dataTypeName) throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(delegate.getMetaData()).thenReturn(metadata);
    when(delegate.getCatalog()).thenReturn("catalog");
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(DATA_TYPE_INDEX)).thenReturn(dataType);
    when(resultSet.getString(ATTR_TYPE_NAME_INDEX)).thenReturn(dataTypeName);
    when(metadata.getAttributes("catalog", null, TYPE_NAME, null)).thenReturn(resultSet);

    return new DefaultDbConnection(delegate, new ArrayList<>(), 1000);
  }

}
