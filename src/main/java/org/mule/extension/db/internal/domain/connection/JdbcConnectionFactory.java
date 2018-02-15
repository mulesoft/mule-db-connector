/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.runtime.api.connection.ConnectionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * A factor for JDBC {@link Connection}s
 *
 * @since 1.0
 */
public class JdbcConnectionFactory {

  /**
   * Ensures DriverManager classloading takes place before any connection creation. It prevents a JDK deadlock that only occurs
   * when two JDBC Connections of different DB vendors are created concurrently and the {@link DriverManager} hasn't been loaded
   * yet. For more information, see MULE-14605.
   */
  static {
    DriverManager.getLoginTimeout();
  }

  /**
   * Creates a new JDBC {@link Connection}
   *
   * @param dataSource the {@link DataSource} from which the connection comes from
   * @param customDataTypes user defined data types
   * @return a {@link Connection}
   */
  public Connection createConnection(DataSource dataSource, List<DbType> customDataTypes)
      throws SQLException, ConnectionCreationException {
    Connection connection;
    connection = dataSource.getConnection();

    if (connection == null) {
      throw new ConnectionCreationException("Unable to create connection to the provided dataSource: " + dataSource);
    }

    Map<String, Class<?>> typeMapping = createTypeMapping(customDataTypes);

    if (typeMapping != null && !typeMapping.isEmpty()) {
      connection.setTypeMap(typeMapping);
    }

    return connection;
  }

  private Map<String, Class<?>> createTypeMapping(List<DbType> customDataTypes) {
    final Map<String, Class<?>> typeMapping = new HashMap<>();

    customDataTypes.stream()
        .filter(dbType -> dbType instanceof MappedStructResolvedDbType)
        .forEach(dbType -> {
          final MappedStructResolvedDbType structDbType = (MappedStructResolvedDbType) dbType;
          if (structDbType.getMappedClass() != null) {
            typeMapping.put(structDbType.getName(), structDbType.getMappedClass());
          }
        });

    return typeMapping;
  }
}
