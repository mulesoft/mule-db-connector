/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation.tracing;

import static org.mule.extension.db.internal.operation.tracing.TracingUtils.DB_CONNECTION_STRING_KEY;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.DB_STATEMENT;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.DB_SYSTEM_KEY;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.DB_USER_KEY;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.NET_PEER_NAME_KEY;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.SPAN_KIND;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.SPAN_KIND_CLIENT_NAME;
import static org.mule.extension.db.internal.operation.tracing.TracingUtils.setAttributesForDbClientOperation;

import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TracingUtilsTestCase {

  public static final String TEST_SPAN_NAME = "test_span_name";
  public static final String TEST_QUERY = "test_query";
  public static final String TEST_CONNECTION_STRING = "connection_string";
  public static final String TEST_DB_SYSTEM = "db_system";
  public static final String TEST_PEER_NAME = "peer_name";
  public static final String TEST_USER = "user";

  @Test
  public void tracingSemanticConventionAttributes() {
    DbConnectionTracingMetadata dbConnectionTracingMetadata = mock(DbConnectionTracingMetadata.class);
    DistributedTraceContextManager distributedTraceContext = mock(DistributedTraceContextManager.class);
    when(dbConnectionTracingMetadata.getConnectionString()).thenReturn(TEST_CONNECTION_STRING);
    when(dbConnectionTracingMetadata.getDbSystem()).thenReturn(TEST_DB_SYSTEM);
    when(dbConnectionTracingMetadata.getPeerName()).thenReturn(of(TEST_PEER_NAME));
    when(dbConnectionTracingMetadata.getUser()).thenReturn(TEST_USER);
    setAttributesForDbClientOperation(dbConnectionTracingMetadata, TEST_SPAN_NAME, distributedTraceContext, TEST_QUERY);
    verify(distributedTraceContext).setCurrentSpanName(any());
    ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(distributedTraceContext).addCurrentSpanAttributes(argumentCaptor.capture());
    Map<String, String> attributes = argumentCaptor.getValue();
    assertThat(attributes, aMapWithSize(6));
    assertThat(attributes, hasEntry(DB_CONNECTION_STRING_KEY, TEST_CONNECTION_STRING));
    assertThat(attributes, hasEntry(SPAN_KIND, SPAN_KIND_CLIENT_NAME));
    assertThat(attributes, hasEntry(DB_SYSTEM_KEY, TEST_DB_SYSTEM));
    assertThat(attributes, hasEntry(DB_USER_KEY, TEST_USER));
    assertThat(attributes, hasEntry(NET_PEER_NAME_KEY, TEST_PEER_NAME));
    assertThat(attributes, hasEntry(DB_STATEMENT, TEST_QUERY));
  }

  @Test
  public void failsafeAttributesPropagation() {
    DbConnectionTracingMetadata dbConnectionTracingMetadata = mock(DbConnectionTracingMetadata.class);
    DistributedTraceContextManager distributedTraceContext = mock(DistributedTraceContextManager.class);
    setAttributesForDbClientOperation(dbConnectionTracingMetadata, TEST_SPAN_NAME, distributedTraceContext, TEST_QUERY);

  }
}
