/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.row;

import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Map;

/**
 * Maps a row using returning a case insensitive map
 */
public class InsensitiveMapRowHandler implements RowHandler {

  @Override
  public Map<String, Object> process(ResultSet resultSet) throws SQLException {
    Map result = new CaseInsensitiveHashMap();
    ResultSetMetaData metaData = resultSet.getMetaData();
    int cols = metaData.getColumnCount();

    for (int i = 1; i <= cols; i++) {
      String column = metaData.getColumnLabel(i);
      Object value = resultSet.getObject(i);

      if (value instanceof SQLXML) {
        result.put(column, handleSqlXmlType((SQLXML) value));
      } else if (value instanceof Clob) {
        result.put(column, handleClobType((Clob) value));
      } else if (value instanceof Blob) {
        result.put(column, handleBlobType((Blob) value));
      } else {
        result.put(column, value);
      }
    }

    if (cols != result.size()) {
      throw new IllegalArgumentException("Record cannot be mapped as it contains multiple columns with the same label. Define column aliases to solve this problem");
    }

    return result;
  }

  //TODO MULE-14614 - All these handlers should return TypedValues indicating their MimeType.
  private InputStream handleSqlXmlType(SQLXML value) throws SQLException {
    return value.getBinaryStream();
  }

  private InputStream handleBlobType(Blob value) throws SQLException {
    return value.getBinaryStream();
  }

  private InputStream handleClobType(Clob value) throws SQLException {
    return value.getAsciiStream();
  }
}
