/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.result.row;

import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.metadata.MediaType.XML;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;

/**
 * Maps a row using returning a case insensitive map
 */
public class InsensitiveMapRowHandler implements RowHandler {

  private DbConnection dbConnection;
  protected Charset charset;

  public InsensitiveMapRowHandler(DbConnection dbConnection) {
    this.dbConnection = dbConnection;
    this.charset = Charset.defaultCharset();
  }

  public InsensitiveMapRowHandler(DbConnection dbConnection, Charset charset) {
    this.dbConnection = dbConnection;
    this.charset = charset;
  }

  @Override
  public Map<String, Object> process(ResultSet resultSet) throws SQLException {
    Map result = new CaseInsensitiveHashMap();
    ResultSetMetaData metaData = resultSet.getMetaData();
    int cols = metaData.getColumnCount();

    for (int i = 1; i <= cols; i++) {
      String column = metaData.getColumnLabel(i);
      Object value = resultSet.getObject(i);

      Object cellValue = getMuleConsumableValue(value);

      result.put(column, cellValue);
    }

    if (cols != result.size()) {
      throw new IllegalArgumentException("Record cannot be mapped as it contains multiple columns with the same label. Define column aliases to solve this problem");
    }

    return result;
  }

  private Object getMuleConsumableValue(Object value) throws SQLException {
    Object returnValue;

    if (value instanceof SQLXML) {
      returnValue = handleSqlXmlType((SQLXML) value);
    } else if (value instanceof Clob) {
      returnValue = handleClobType((Clob) value);
    } else if (value instanceof Blob) {
      returnValue = handleBlobType((Blob) value);
    } else if (value instanceof Struct) {
      returnValue = handleStructType((Struct) value);
    } else if (value instanceof Array) {
      returnValue = handleArrayType((Array) value);
    } else {
      returnValue = value;
    }
    return returnValue;
  }

  private Object handleArrayType(Array value) throws SQLException {
    Object array = value.getArray();
    if (array.getClass().isArray()) {
      Object[] arrayValue = (Object[]) array;
      Object[] newArrayValue = new Object[arrayValue.length];
      for (int i = 0; i < newArrayValue.length; i++) {
        newArrayValue[i] = getMuleConsumableValue(arrayValue[i]);
      }

      return newArrayValue;
    } else {
      return array;
    }
  }

  private Object[] handleStructType(Struct value) throws SQLException {
    return value.getAttributes();
  }

  protected TypedValue<Object> handleSqlXmlType(SQLXML value) throws SQLException {
    return new TypedValue<>(value.getBinaryStream(), DataType.builder().type(InputStream.class).mediaType(XML).build());
  }

  protected TypedValue<Object> handleBlobType(Blob value) throws SQLException {
    if (dbConnection != null && dbConnection.supportsContentStreaming()) {
      return new TypedValue<>(value.getBinaryStream(), DataType.builder().type(InputStream.class).mediaType(BINARY).build());
    } else {
      return new TypedValue<>(new ByteArrayInputStream(IOUtils.toByteArray(value.getBinaryStream())),
              DataType.builder().type(byte[].class).mediaType(BINARY).build());
    }
  }

  protected TypedValue<Object> handleClobType(Clob value) throws SQLException {
    ReaderInputStream inputStream = new ReaderInputStream(value.getCharacterStream(), charset);
    if (dbConnection != null && dbConnection.supportsContentStreaming()) {
      return new TypedValue<>(inputStream, DataType.builder().type(InputStream.class)
              .mediaType(TEXT)
              .charset(charset)
              .build());
    } else {
      return new TypedValue<>(new ByteArrayInputStream(IOUtils.toByteArray(inputStream)), DataType.builder()
              .type(byte[].class)
              .mediaType(TEXT)
              .charset(charset)
              .build());
    }
  }
}