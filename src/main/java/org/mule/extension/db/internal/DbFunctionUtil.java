/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static java.lang.String.format;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for DB Functions
 *
 * @since 1.5.1, 1.6.0
 */
public class DbFunctionUtil implements Disposable {

  private final Logger logger = LoggerFactory.getLogger(DbFunctionUtil.class);
  private final ExtensionManager extensionManager;
  private final LoadingCache<String, DbConnection> cachedConnections;

  private static final CoreEvent EVENT = CoreEvent.builder(create("DB", "dummy", fromSingleComponent("DB"),
                                                                  NullExceptionHandler.getInstance()))
      .message(Message.of("none"))
      .build();

  public DbFunctionUtil(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
    this.cachedConnections = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(1)))
        .removalListener((RemovalListener<String, DbConnection>) (configName, connection, removalCause) -> {
          logger.debug("Disconnecting connection {} for config: {}.", connection, configName);
          try {
            ConnectionProvider connectionProvider = getConnectionProvider(configName);
            connectionProvider.disconnect(connection);
          } catch (Exception ex) {
            logger.warn("Cannot disconnect the current connection {}. Exception: {}.", configName, ex);
          }
        })
        .build(configName -> createNewConnection(configName));
  }

  public Object execute(WithConnection withConnection, List values, String typeName, String configName) {
    try {
      DbConnection dbConnection = this.cachedConnections.get(configName);
      return withConnection.execute(dbConnection, values, typeName);
    } catch (Throwable t) {
      this.cachedConnections.invalidate(configName);
      throw new RuntimeException("An error occurred when trying to create JDBC Structure. " + t.getMessage(), t);
    }
  }

  private DbConnection createNewConnection(String configName) throws ConnectionException {
    ConnectionProvider connectionProvider = getConnectionProvider(configName);
    Object newConnection = connectionProvider.connect();
    if (!(newConnection instanceof DbConnection)) {
      throw new RuntimeException("Connection is not a DB Connection");
    } else {
      return (DbConnection) newConnection;
    }
  }

  private ConnectionProvider getConnectionProvider(String configName) {
    ConfigurationInstance configuration = extensionManager.getConfiguration(configName, EVENT);
    Optional<ConnectionProvider> connectionProvider = configuration.getConnectionProvider();
    return connectionProvider
        .orElseThrow(() -> new RuntimeException(format("Unable to obtain a connection for configuration: [%s]", configName)));
  }

  @Override
  public void dispose() {
    this.cachedConnections.invalidateAll();
  }

  @FunctionalInterface
  public interface WithConnection {

    Object execute(DbConnection connection, List values, String typeName) throws ConnectionException, SQLException;
  }
}
