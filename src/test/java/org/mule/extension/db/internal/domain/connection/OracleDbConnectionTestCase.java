/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.junit.Test;
import org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleDbConnection.ATTR_TYPE_NAME_PARAM;
import static org.mule.extension.db.internal.domain.connection.type.resolver.ArrayTypeResolver.QUERY_ALL_COLL_TYPES;

public class OracleDbConnectionTestCase extends AbstractMuleTestCase
{
    private final String TYPE_NAME = "TYPE_NAME";
    private final String OTHER_TYPE_NAME = "OTHER_TYPE";


    @Test
    public void lobResolutionPerformance() throws Exception
    {


        /*
        The main goal of this test is to assert that lob resolution is performed using
        database query only once for every type.

        Assertions required:
                * Assert that the query is executed only once for
                  every Type.
                * Assert that the right value is present in the cache.
                * Assert that on the following calls for lob resolution
                  the value comes from the cache.
                * The mock must be reset for every call.

         Important Note. Fixing DBCON-179 will change the statements executed
         on every call.
         */

        Map<String, Map<Integer, ResolvedDbType>> dbTypeCache = new ConcurrentHashMap<>();
        Object[] structValues = {"clob", "foo"};
        Object[] structValues1 = {"clob1", "foo1"};
        Object[] params = {structValues, structValues1};
        Object[] params2 = {structValues, structValues1};
        final String USER_TYPE_NAME = "FOO";
        final String USER_TYPE_DBNAME = "BAR";
        final String USER_TYPE_NAME_B = "ICECREAM";
        final String USER_TYPE_DBNAME_B = "SANDWICH";

        //First call.
        Connection delegate = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(delegate.prepareStatement(OracleDbConnection.QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);
        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(USER_TYPE_DBNAME);

        OracleDbConnection cnx = new OracleDbConnection(delegate, Collections.emptyList(), dbTypeCache);
        cnx.createArrayOf(USER_TYPE_NAME, params);
        assertThat(dbTypeCache.containsKey(USER_TYPE_NAME), is(true));
        assertThat(dbTypeCache.get(USER_TYPE_NAME).get(0).getName(), is(USER_TYPE_DBNAME));
        verify(preparedStatement, times(2)).executeQuery();

        //Second Call
        delegate = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(delegate.prepareStatement(OracleDbConnection.QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);
        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(USER_TYPE_DBNAME);

        cnx = new OracleDbConnection(delegate, Collections.emptyList(), dbTypeCache);
        cnx.createArrayOf(USER_TYPE_NAME, params);

        assertThat(dbTypeCache.containsKey(USER_TYPE_NAME), is(true));
        assertThat(dbTypeCache.get(USER_TYPE_NAME).get(0).getName(), is(USER_TYPE_DBNAME));
        assertThat(dbTypeCache.keySet().size(), is(1));
        //Fixing DBCON-179 will result in 0 times.
        verify(preparedStatement, times(1)).executeQuery();

        //Third Call
        delegate = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(delegate.prepareStatement(OracleDbConnection.QUERY_TYPE_ATTRS)).thenReturn(preparedStatement);
        when(delegate.prepareStatement(QUERY_ALL_COLL_TYPES)).thenReturn(preparedStatement);

        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(resultSet.getString(ATTR_TYPE_NAME_PARAM)).thenReturn(USER_TYPE_DBNAME_B);

        cnx = new OracleDbConnection(delegate, Collections.emptyList(), dbTypeCache);
        cnx.createArrayOf(USER_TYPE_NAME_B, params);

        assertThat(dbTypeCache.containsKey(USER_TYPE_NAME_B), is(true));
        assertThat(dbTypeCache.get(USER_TYPE_NAME_B).get(0).getName(), is(USER_TYPE_DBNAME_B));
        assertThat(dbTypeCache.keySet().size(), is(2));
        verify(preparedStatement, times(2)).executeQuery();


    }
}
