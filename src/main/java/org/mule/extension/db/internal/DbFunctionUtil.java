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

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for DB Functions
 *
 * @since 1.5.1, 1.6.0
 */
public class DbFunctionUtil {

  private ExtensionManager extensionManager;

  private static final CoreEvent EVENT = CoreEvent.builder(create("DB", "dummy", fromSingleComponent("DB"),
                                                                  NullExceptionHandler.getInstance()))
      .message(Message.of("none"))
      .build();

  public DbFunctionUtil(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  public Object execute(WithConnection withConnection, List values, String typeName, String connectionName) {
    ConnectionProvider connectionProvider = getConnectionProvider(connectionName);
    DbConnection dbConnection = null;
    try {
      Object connection = connectionProvider.connect();
      if (!(connection instanceof DbConnection)) {
        throw new RuntimeException("Connection is not a DB Connection");
      } else {
        dbConnection = (DbConnection) connection;
      }

      return withConnection.execute(dbConnection, values, typeName);
    } catch (Throwable t) {
      throw new RuntimeException("An error occurred when trying to create JDBC Structure. " + t.getMessage(), t);
    } finally {
      connectionProvider.disconnect(dbConnection);
    }
  }

  private ConnectionProvider getConnectionProvider(String configName) {
    ConfigurationInstance configuration = extensionManager.getConfiguration(configName, EVENT);
    Optional<ConnectionProvider> connectionProvider = configuration.getConnectionProvider();
    return connectionProvider
        .orElseThrow(() -> new RuntimeException(format("Unable to obtain a connection for configuration: [%s]", configName)));
  }

  @FunctionalInterface
  public interface WithConnection {

    Object execute(DbConnection connection, List values, String typeName) throws ConnectionException, SQLException;
  }
}
