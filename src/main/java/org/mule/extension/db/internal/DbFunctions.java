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
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Database connector functions to create complex JDBC Structures.
 *
 * @since 1.5.1
 */
public class DbFunctions {

  private static final CoreEvent EVENT = CoreEvent.builder(create("DB", "dummy", fromSingleComponent("DB"),
                                                                  NullExceptionHandler.getInstance()))
      .message(Message.of("none"))
      .build();

  @Inject
  ExtensionManager extensionManager;

  /**
   * DataWeave function to create JDBC Array objects based on the Array Type to create and the values that conforms the type.
   *
   * @param typeName   The name of the Array type to create
   * @param values     An array of values that conforms the Array Type
   * @param configName The configuration in charge of creating the Array Type
   * @return
   */
  public Object createArray(String configName, String typeName, List values) {
    return execute((con, val, jdbcType) -> con.createArrayOf(jdbcType, values.toArray()), values, typeName,
                   configName);
  }

  /**
   * DataWeave function to create JDBC Struct objects based on the Type Name and their correspondent properties.
   *
   * @param typeName   The name of the Array type to create
   * @param properties An array of values that conforms the Struct properties
   * @param configName The configuration in charge of creating the Struct type
   * @return
   */
  public Object createStruct(String configName, String typeName, List properties) {
    return execute((con, val, jdbcType) -> con.getJdbcConnection()
        .createStruct(jdbcType, val.toArray()), properties, typeName, configName);
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
  private interface WithConnection {

    Object execute(DbConnection connection, List values, String typeName) throws ConnectionException, SQLException;
  }
}
