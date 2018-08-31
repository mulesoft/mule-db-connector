/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import static java.lang.String.format;
import static org.mule.extension.db.api.param.JdbcType.BLOB;
import static org.mule.extension.db.api.param.JdbcType.CLOB;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;

import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.runtime.core.api.util.IOUtils;

public class OracleJdbcConnectionWrapper extends AbstractJdbcConnectionWrapper {

  public static final String ATTR_TYPE_NAME_PARAM = "ATTR_TYPE_NAME";

  public static final String ATTR_NO_PARAM = "ATTR_NO";

  public static final String QUERY_TYPE_ATTRS =
      "SELECT ATTR_NO, ATTR_TYPE_NAME FROM ALL_TYPE_ATTRS WHERE TYPE_NAME = ? AND ATTR_TYPE_NAME IN ('CLOB', 'BLOB')";



  private Method createArrayMethod;
  private boolean initialized;

  public OracleJdbcConnectionWrapper(Connection delegate) {
    super(delegate);
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    if (getCreateArrayOfMethod(delegate) == null) {
      return super.createArrayOf(typeName, elements);
    } else {
      try {
        return (Array) getCreateArrayOfMethod(delegate).invoke(delegate, typeName, elements);
      } catch (Exception e) {
        throw new SQLException("Error creating ARRAY", e);
      }
    }
  }

  private Method getCreateArrayOfMethod(Connection delegate) {
    if (createArrayMethod == null && !initialized) {
      synchronized (this) {
        if (createArrayMethod == null && !initialized) {
          try {
            createArrayMethod = delegate.getClass().getMethod("createARRAY", String.class, Object.class);
            createArrayMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            // Ignore, will use the standard method
          }

          initialized = true;
        }
      }
    }

    return createArrayMethod;
  }

  @Override
  protected void resolveLobs(String typeName, Object[] attributes) throws SQLException {
    Map<Integer, String> dataTypes = getDataTypes(typeName);

    for (int index : dataTypes.keySet()) {
      String dataTypeName = dataTypes.get(index);
      // In Oracle we do not have the data type for structs, as the
      // the driver does not provide the getAttributes functionality
      // in their DatabaseMetaData.
      // It has to be taken into account that the data type depends on JDBC, so the
      // driver is the unit responsible for the mapping and we do not have that information
      // in the DB catalog. We resolve the lobs depending on the name only.
      doResolveLobIn(attributes, index - 1, dataTypeName);
    }
  }

  private Map<Integer, String> getDataTypes(String typeName) throws SQLException {
    Map<Integer, String> dataTypes = new HashMap<Integer, String>();

    try (PreparedStatement ps = this.prepareStatement(QUERY_TYPE_ATTRS)) {
      ps.setString(1, typeName);

      ResultSet resultSet = ps.executeQuery();

      while (resultSet.next()) {
        dataTypes.put(resultSet.getInt(ATTR_NO_PARAM), resultSet.getString(ATTR_TYPE_NAME_PARAM));
      }

      return dataTypes;
    }
  }

  protected Blob createBlob(Object attribute) throws SQLException {
    Blob blob = this.createBlob();
    if (attribute instanceof byte[]) {
      blob.setBytes(1, (byte[]) attribute);
    } else if (attribute instanceof InputStream) {
      blob.setBytes(1, toByteArray((InputStream) attribute));
    } else if (attribute instanceof String) {
      blob.setBytes(1, ((String) attribute).getBytes());
    } else {
      throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(),
                                                attribute.getClass()));
    }

    return blob;
  }

  protected Clob createClob(Object attribute) throws SQLException {
    Clob clob = this.createClob();
    if (attribute instanceof String) {
      clob.setString(1, (String) attribute);
    } else if (attribute instanceof InputStream) {
      clob.setString(1, IOUtils.toString((InputStream) attribute));
    } else {
      throw new IllegalArgumentException(format("Cannot create a %s from a value of type '%s'", Struct.class.getName(),
                                                attribute.getClass()));
    }

    return clob;
  }
}
