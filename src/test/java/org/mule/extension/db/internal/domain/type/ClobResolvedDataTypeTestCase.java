/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.sql.Types.CLOB;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.db.internal.domain.type.ClobResolvedDataType.createUnsupportedTypeErrorMessage;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ClobResolvedDataTypeTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ClobResolvedDataType dataType;
  private PreparedStatement statement;
  private Connection connection;
  private DbConnection dbConnection;
  private Clob clob;

  @Before
  public void setUp() throws Exception {
    dataType = new ClobResolvedDataType(CLOB, null);
    statement = mock(PreparedStatement.class);
    connection = mock(Connection.class);
    dbConnection = mock(DbConnection.class);
    clob = mock(Clob.class);

    when(statement.getConnection()).thenReturn(connection);
    when(connection.createClob()).thenReturn(clob);
  }

  @Test
  public void convertsStringToClobWhenDriverSupportsSetClob() throws Exception {
    String value = "foo";

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(connection).createClob();
  }

  @Test
  public void convertsInputStreamToClobWhenDriverSupportsSetClob() throws Exception {
    String streamContent = "bar";
    InputStream value = new StringInputStream(streamContent);

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(connection).createClob();
  }

  @Test
  public void convertsStringToClobWhenDriverDoesNotSupportSetClob() throws Exception {
    when(connection.createClob()).thenThrow(new RuntimeException());

    String value = "foo";

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(statement).setCharacterStream(eq(PARAM_INDEX), any(StringReader.class), eq(value.length()));
  }

  @Test
  public void convertsInputStreamToClobWhenDriverDoesNotSupportSetClob() throws Exception {
    when(connection.createClob()).thenThrow(new RuntimeException());

    String streamContent = "bar";
    InputStream value = new StringInputStream(streamContent);

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);

    verify(statement).setCharacterStream(eq(PARAM_INDEX), any(StringReader.class), eq(streamContent.length()));
  }

  @Test
  public void setClobDirectly() throws Exception {
    Clob clob = mock(Clob.class);

    dataType.setParameterValue(statement, PARAM_INDEX, clob, dbConnection);

    verify(statement).setObject(PARAM_INDEX, clob, CLOB);
  }

  @Test
  public void failsToConvertUnsupportedType() throws Exception {

    Object value = new Object();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString(createUnsupportedTypeErrorMessage(value)));

    dataType.setParameterValue(statement, PARAM_INDEX, value, dbConnection);
  }

  private static class StringInputStream extends ReaderInputStream {

    public StringInputStream(String source) {
      super(new StringReader(source));
    }
  }
}
