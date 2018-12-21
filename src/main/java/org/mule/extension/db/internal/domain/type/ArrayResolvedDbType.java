/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.mule.extension.db.internal.domain.connection.DbConnection;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a structured data type for {@link Array}
 */
public class ArrayResolvedDbType extends AbstractStructuredDbType {

  /**
   * Creates a new instance
   *
   * @param id identifier for the type
   * @param name type name. Non Empty.
   */
  public ArrayResolvedDbType(int id, String name) {
    super(id, name);
  }

  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value, DbConnection dbConnection)
      throws SQLException {
    if (!(value instanceof Array)) {
      if (value instanceof Object[]) {
        value = dbConnection.createArrayOf(name, (Object[]) value);
      } else if (value instanceof List) {
        value = dbConnection.createArrayOf(name, ((List) value).toArray());
      } else {
        throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
      }
    }

    statement.setArray(index, (Array) value);
  }

  @Override
  public Object getParameterValue(CallableStatement statement, int index) throws SQLException {
    Object array = statement.getArray(index).getArray();
    if (array instanceof Collection) {
      return ((Collection<?>) array).stream().map(this::processArray).collect(toList());
    } else if (array instanceof Object[]) {
      return Arrays.stream((Object[]) array).map(this::processArray).collect(toList());
    }
    return array;
  }

  private Object processArray(Object object) {
    if (object instanceof Struct) {
      try {
        return ((Struct) object).getAttributes();
      } catch (SQLException e1) {
        throw new RuntimeException(e1);
      }
    } else {
      return object;
    }
  }

  /**
   * Creates error message for the case when a given class is not supported
   *
   * @param value value that was attempted to be converted
   * @return the error message for the provided value's class
   */
  protected static String createUnsupportedTypeErrorMessage(Object value) {
    return format("Cannot create a %s from a value of type %s", Struct.class.getName(), value.getClass());
  }
}
