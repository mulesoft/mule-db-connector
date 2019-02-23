/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.db.internal.domain.connection.DefaultDbConnection.ATTR_TYPE_NAME_INDEX;
import static org.mule.extension.db.internal.domain.connection.DefaultDbConnection.DATA_TYPE_INDEX;
import org.mule.extension.db.internal.domain.connection.DefaultDbConnection;
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

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DefaultDbConnection testThroughMetadata(Connection delegate, int dataType, String dataTypeName) throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(delegate.getMetaData()).thenReturn(metadata);
    when(delegate.getCatalog()).thenReturn("catalog");
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getInt(DATA_TYPE_INDEX)).thenReturn(dataType);
    when(resultSet.getString(ATTR_TYPE_NAME_INDEX)).thenReturn(dataTypeName);
    when(metadata.getAttributes("catalog", null, TYPE_NAME, null)).thenReturn(resultSet);

    return new DefaultDbConnection(delegate, new ArrayList<>());
  }

}
