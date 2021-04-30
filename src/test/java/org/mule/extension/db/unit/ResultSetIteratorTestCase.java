/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import org.junit.Test;
import org.mockito.Mock;
import org.mule.db.commons.internal.result.resultset.ResultSetIterator;
import org.mule.db.commons.internal.result.row.RowHandler;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultSetIteratorTestCase {

  @Mock
  private final ResultSet resultSetMock = mock(ResultSet.class);

  @Mock
  private final RowHandler rowHandlerMock = mock(RowHandler.class);

  private final ResultSetIterator resultSetIterator = new ResultSetIterator(this.resultSetMock, this.rowHandlerMock);

  @Test(expected = ModuleException.class)
  public void next_WhenProcessingNextRowThrowsSQLException_ThenModuleExceptionIsCreated() throws SQLException {
    when(this.rowHandlerMock.process(any(ResultSet.class))).thenThrow(new SQLException("Some SQL Exception"));

    this.resultSetIterator.next();
  }
}
