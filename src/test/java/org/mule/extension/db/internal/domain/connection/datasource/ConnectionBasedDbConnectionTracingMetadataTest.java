/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.junit.Test;
import scala.NotImplementedError;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class ConnectionBasedDbConnectionTracingMetadataTest {

  public static final String EXPECTED_URL = "tcp://localhost:8081/db";
  public static final String EXPECTED_DB_SYSTEM = "DB_SYSTEM";
  public static final String EXPECTED_PEER_NAME = "localhost";
  public static final String EXPECTED_PEER_PROTOCOL = "tcp";
  public static final String EXPECTED_USER = "expectedUser";
  public static final String UNSET = "unset";

  @Test
  public void testDbConnectionTracingMetadata() throws Exception {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metadata);
    when(metadata.getURL()).thenReturn(EXPECTED_URL);
    when(metadata.getDatabaseProductName()).thenReturn(EXPECTED_DB_SYSTEM);
    when(metadata.getUserName()).thenReturn(EXPECTED_USER);

    ConnectionBasedDbConnectionTracingMetadata connectionBasedDbConnectionTracingMetadata =
        new ConnectionBasedDbConnectionTracingMetadata(connection);

    assertEquals(EXPECTED_URL, connectionBasedDbConnectionTracingMetadata.getConnectionString());
    assertEquals(EXPECTED_DB_SYSTEM, connectionBasedDbConnectionTracingMetadata.getDbSystem());
    assertEquals(of(EXPECTED_PEER_NAME), connectionBasedDbConnectionTracingMetadata.getPeerName());
    assertEquals(of(EXPECTED_PEER_PROTOCOL), connectionBasedDbConnectionTracingMetadata.getPeerTransport());
    assertEquals(EXPECTED_USER, connectionBasedDbConnectionTracingMetadata.getUser());
  }

  @Test
  public void testDoesNotFailWhenTryingToRetrieveMetadata() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getMetaData()).thenThrow(NotImplementedError.class);

    ConnectionBasedDbConnectionTracingMetadata connectionBasedDbConnectionTracingMetadata =
        new ConnectionBasedDbConnectionTracingMetadata(connection);
    assertEquals(UNSET, connectionBasedDbConnectionTracingMetadata.getConnectionString());
    assertEquals(UNSET, connectionBasedDbConnectionTracingMetadata.getDbSystem());
    assertEquals(empty(), connectionBasedDbConnectionTracingMetadata.getPeerName());
    assertEquals(empty(), connectionBasedDbConnectionTracingMetadata.getPeerTransport());
    assertEquals(UNSET, connectionBasedDbConnectionTracingMetadata.getUser());
  }
}
