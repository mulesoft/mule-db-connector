/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.result.row;

import org.apache.commons.io.input.ReaderInputStream;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;

import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

/**
 * Maps a row using returning a case insensitive map to its values.
 * No streams will be returned as values. This means that all of
 * the mapped values will be effectively read into memory
 * and returned in the result.
 */
public class NonStreamingInsensitiveMapRowHandler extends InsensitiveMapRowHandler {

  public NonStreamingInsensitiveMapRowHandler(DbConnection dbConnection) {
    super(dbConnection);
  }

  public NonStreamingInsensitiveMapRowHandler(DbConnection dbConnection, Charset charset) {
    super(dbConnection, charset);
  }

  @Override
  protected TypedValue<Object> handleSqlXmlType(SQLXML value) throws SQLException {
    return new TypedValue<>(value.getString(), DataType.builder().type(SQLXML.class).mediaType(XML).build());
  }

  @Override
  protected TypedValue<Object> handleBlobType(Blob value) throws SQLException {
    ByteArrayInputStream is = new ByteArrayInputStream(IOUtils.toByteArray(value.getBinaryStream()));
    return new TypedValue<>(IOUtils.toByteArray(is),
                            DataType.builder().type(byte[].class).mediaType(BINARY).build());
  }

  @Override
  protected TypedValue<Object> handleClobType(Clob value) throws SQLException {
    ReaderInputStream inputStream = new ReaderInputStream(value.getCharacterStream(), charset);
    return new TypedValue<>(IOUtils.toString(inputStream), DataType.builder()
        .type(String.class)
        .mediaType(TEXT)
        .charset(charset)
        .build());
  }
}
