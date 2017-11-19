/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.extension.db.api.exception.connection.QueryExecutionException;
import org.mule.extension.db.internal.exception.DbExceptionHandler;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.SQLException;

import org.junit.Test;

@SmallTest
public class DbExceptionHandlerTestCase extends AbstractMuleTestCase {

  private DbExceptionHandler handler = new DbExceptionHandler();

  @Test
  public void handleModuleException() {
    ModuleException e = mock(ModuleException.class);
    assertThat(handler.enrichException(e), is(sameInstance(e)));
  }

  @Test
  public void handle08S01SqlState() {
    assertConnectivitySqlState("08S01");
  }

  @Test
  public void handle08001SqlState() {
    assertConnectivitySqlState("08001");
  }

  @Test
  public void genericSqlException() {
    SQLException sqlException = mock(SQLException.class);
    Exception handledException = handler.enrichException(sqlException);

    assertThat(handledException, is(instanceOf(QueryExecutionException.class)));
    assertThat(handledException.getCause(), is(sameInstance(sqlException)));
  }

  @Test
  public void nonSqlException() {
    Exception e = new RuntimeException();
    assertThat(handler.enrichException(e), is(sameInstance(e)));
  }

  private void assertConnectivitySqlState(String sqlState) {
    SQLException sqlException = mock(SQLException.class);
    when(sqlException.getSQLState()).thenReturn(sqlState);

    Exception handledException = handler.enrichException(sqlException);
    assertThat(handledException, is(instanceOf(ConnectionException.class)));
    assertThat(handledException.getCause(), is(sameInstance(sqlException)));
  }
}
