/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import static com.mchange.v2.c3p0.C3P0ProxyConnection.RAW_CONNECTION;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import com.mchange.v2.c3p0.impl.NewProxyConnection;

public class OracleJdbcConnectionWrapper extends AbstractJdbcConnectionWrapper {

  private boolean initialized;
  private ArrayFactory arrayFactory;

  public OracleJdbcConnectionWrapper(Connection delegate) {
    super(delegate);
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    try {
      Object[] objects = Arrays.stream(elements).map(e -> {
        if (e instanceof Collection) {
          return ((Collection) e).toArray();
        } else {
          return e;
        }
      }).toArray();

      return createArray(delegate, typeName, objects);
    } catch (Exception e) {
      throw new SQLException("Error creating ARRAY", e);
    }
  }

  private Array createArray(Connection delegate, String typeName, Object[] objects)
      throws IllegalAccessException, SQLException, InvocationTargetException {
    init(delegate);
    return arrayFactory.createArray(typeName, objects);
  }

  private void init(Connection delegate) {
    if (arrayFactory == null || !initialized) {
      synchronized (this) {
        if (arrayFactory == null || !initialized) {
          try {
            if (delegate instanceof NewProxyConnection) {
              arrayFactory = createArrayFactoryFromProxy((NewProxyConnection) delegate);
            } else {
              arrayFactory = createArrayFactory(delegate);
            }
          } catch (Exception e) {
            arrayFactory = super::createArrayOf;
          }
        }
        initialized = true;
      }
    }
  }

  /**
   * Creates a {@link ArrayFactory} based on the given connection
   *
   * @param connection Connection Proxy
   * @return a {@link ArrayFactory}
   */
  private ArrayFactory createArrayFactory(Connection connection) throws NoSuchMethodException {
    Method createArrayOfMethod = getCreateArrayOfMethod(connection.getClass());
    return (type, objects) -> (Array) createArrayOfMethod.invoke(connection, type, objects);
  }

  /**
   * Creates a {@link ArrayFactory} based on a Proxied Oracle Connection
   *
   * @param connection Connection Proxy
   * @return a {@link ArrayFactory}
   */
  private ArrayFactory createArrayFactoryFromProxy(NewProxyConnection connection)
      throws NoSuchMethodException, IllegalAccessException, SQLException, InvocationTargetException {
    Method method = getCreateArrayOfMethod(getProxiedConnectionClass(connection));
    return (type, objects) -> (Array) connection.rawConnectionOperation(method, RAW_CONNECTION, new Object[] {type, objects});
  }

  /**
   * Obtains the class of the proxied connection
   *
   * @param connection Proxied class
   * @return The {@link Class} of the proxied connection
   */
  private Class<?> getProxiedConnectionClass(NewProxyConnection connection)
      throws IllegalAccessException, InvocationTargetException, SQLException, NoSuchMethodException {
    return (Class) connection.rawConnectionOperation(Object.class.getMethod("getClass"), RAW_CONNECTION, new Object[] {});
  }

  private Method getCreateArrayOfMethod(Class<?> connectionClass) throws NoSuchMethodException {
    Method createArrayMethod = connectionClass.getMethod("createARRAY", String.class, Object.class);
    createArrayMethod.setAccessible(true);

    return createArrayMethod;
  }

  @FunctionalInterface
  private interface ArrayFactory {

    Array createArray(String type, Object[] objects) throws SQLException, InvocationTargetException, IllegalAccessException;
  }

}
