/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.driver.OracleConnection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleOpaqueXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleSQLXMLType;
import org.mule.extension.db.internal.domain.connection.oracle.types.OracleXmlTypeHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLXML;

import static org.mockito.Mockito.*;

@SmallTest
public class OracleXmlTypeHandlerTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;
  private static final short version = 11999;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OracleSQLXMLType oracleSQLXMLType;
  private PreparedStatement statement;
  private OracleConnection connection;
  private oracle.jdbc.internal.OracleConnection internalConnection;
  private DatabaseMetaData databaseMetaData;
  private OracleCallableStatement oracleCallableStatement;
  private SQLXML sqlxml;

  @Before
  public void setUp() throws Exception {
    oracleSQLXMLType = new OracleSQLXMLType();
    statement = mock(PreparedStatement.class);
    connection = mock(OracleConnection.class);
    internalConnection = mock(oracle.jdbc.internal.OracleConnection.class);
    databaseMetaData = mock(DatabaseMetaData.class);
    oracleCallableStatement = mock(OracleCallableStatement.class);
    sqlxml = mock(SQLXML.class);

    when(statement.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(databaseMetaData);
    when(connection.prepareCall(anyString())).thenReturn(oracleCallableStatement);
    when(connection.physicalConnectionWithin()).thenReturn(internalConnection);
    when(connection.physicalConnectionWithin().getVersionNumber()).thenReturn(version);

    Class.forName(OracleXmlTypeHandler.ORACLE_XMLTYPE_CLASS);
  }

  @Test
  public void createXMLTypeFromString() throws Exception {
    String xmlText = "<note>\n" +
        "<to>Tove</to>\n" +
        "<from>Jani</from>\n" +
        "<heading>Reminder</heading>\n" +
        "<body>Don't forget me this weekend!</body>\n" +
        "</note>";

    when(connection.createSQLXML()).thenReturn(sqlxml);

    oracleSQLXMLType.setParameterValue(statement, PARAM_INDEX, xmlText, null);

    verify(connection).createSQLXML();
  }

}
