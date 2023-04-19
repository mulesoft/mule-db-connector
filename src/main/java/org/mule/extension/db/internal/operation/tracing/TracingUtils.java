/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation.tracing;

import org.mule.db.commons.internal.domain.connection.DbConnectionTracingMetadata;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utils for runtime tracing.
 */
public class TracingUtils {

  public static final String SPAN_KIND_CLIENT_NAME = "CLIENT";

  private static final Logger LOGGER = getLogger(TracingUtils.class);
  static final String SPAN_KIND = "span.kind.override";
  static final String DB_CONNECTION_STRING_KEY = "db.connection_string";
  static final String DB_SYSTEM_KEY = "db.system";
  static final String DB_USER_KEY = "db.user";
  static final String NET_PEER_NAME_KEY = "net.peer.name";
  static final String DB_STATEMENT = "db.statement";

  /**
   * It sets the attributes for the db client operation corresponding
   * to the semantic conventions for Optel.
   *
   * @link <a href="https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/database/">Semantic conventions for database client calls</a>
   *
   * @param dbConnectionTracingMetadata dbConnectionTracingMetadata the metadata from the tracing connection to get information for the span.
   * @param spanName the updated name of the current span
   * @param distributedTraceContextManager the {@link DistributedTraceContextManager} to communicate with the runtime.
   * @param query the query string for the operation.
   */
  public static void setAttributesForDbClientOperation(DbConnectionTracingMetadata dbConnectionTracingMetadata,
                                                       String spanName,
                                                       DistributedTraceContextManager distributedTraceContextManager,
                                                       String query) {
    try {
      // We set the name because it is the most generic operation.
      // We follow the semantic conventions in this part:
      // "It is not recommended to attempt any client-side parsing of db.statement just to get these properties,
      // they should only be used if the library being instrumented already provides them. When it?s otherwise
      // impossible to get any meaningful span name, db.name or the tech-specific database name MAY be used."
      traceDbClientOperation(spanName,
                             distributedTraceContextManager,
                             dbConnectionTracingMetadata,
                             query);
    } catch (Throwable e) {
      LOGGER.warn("Exception on adding span attributes", e);
    }
  }

  /**
   * It sets the attributes for the db client operation corresponding
   * to the semantic conventions for Optel.
   * As the span name the db system will be used.
   *
   * @link <a href="https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/database/">Semantic conventions for database client calls</a>
   *
   * @param dbConnectionTracingMetadata dbConnectionTracingMetadata the metadata from the tracing connection to get information for the span.
   * @param distributedTraceContextManager the {@link DistributedTraceContextManager} to communicate with the runtime.
   * @param query the query string for the operation.
   */
  public static void setAttributesForDbClientOperation(DbConnectionTracingMetadata dbConnectionTracingMetadata,
                                                       DistributedTraceContextManager distributedTraceContextManager,
                                                       String query) {

    try {
      // We set the name because it is the most generic operation.
      // We follow the semantic conventions in this part:
      // "It is not recommended to attempt any client-side parsing of db.statement just to get these properties,
      // they should only be used if the library being instrumented already provides them. When it?s otherwise
      // impossible to get any meaningful span name, db.name or the tech-specific database name MAY be used."
      traceDbClientOperation(dbConnectionTracingMetadata.getDbSystem(),
                             distributedTraceContextManager,
                             dbConnectionTracingMetadata,
                             query);
    } catch (Throwable e) {
      LOGGER.warn("Exception on adding span attributes", e);
    }

  }

  private static void traceDbClientOperation(String spanName, DistributedTraceContextManager distributedTraceContextManager,
                                             DbConnectionTracingMetadata dbConnectionTracingMetadata, String sqlQuery) {
    distributedTraceContextManager.setCurrentSpanName(spanName);
    distributedTraceContextManager.addCurrentSpanAttributes(new LazySpanAttributesMap(sqlQuery, dbConnectionTracingMetadata));
  }

  /**
   * A lazy span attributes map to not affect the performance of the operations.
   */

  private final static class LazySpanAttributesMap implements Map<String, String> {

    private final DbConnectionTracingMetadata dbConnectionTracingMetadata;

    private final String sqlQuery;

    private Map<String, String> delegate;

    public LazySpanAttributesMap(String sqlQuery, DbConnectionTracingMetadata dbConnectionTracingMetadata) {
      this.sqlQuery = sqlQuery;
      this.dbConnectionTracingMetadata = dbConnectionTracingMetadata;
    }

    @Override
    public int size() {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.size();
    }

    private Map<String, String> createDelegateMap() {
      Map<String, String> delegate = new HashMap<>();
      delegate.put(DB_STATEMENT, sqlQuery);
      delegate.put(DB_CONNECTION_STRING_KEY, dbConnectionTracingMetadata.getConnectionString());
      delegate.put(DB_SYSTEM_KEY, dbConnectionTracingMetadata.getDbSystem());
      delegate.put(DB_USER_KEY, dbConnectionTracingMetadata.getUser());
      dbConnectionTracingMetadata.getPeerName()
          .ifPresent(peerName -> delegate.put(NET_PEER_NAME_KEY, peerName));
      delegate.put(SPAN_KIND, SPAN_KIND_CLIENT_NAME);
      return delegate;
    }

    @Override
    public boolean isEmpty() {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.containsValue(value);
    }

    @Override
    public String get(Object key) {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.get(key);
    }

    @Override
    public String put(String key, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.keySet();
    }

    @Override
    public Collection<String> values() {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      if (delegate == null) {
        delegate = createDelegateMap();
      }
      return delegate.entrySet();
    }
  }
}
