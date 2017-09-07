/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.type;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a Blob data type that was resolved for a database instance.
 *
 * @since 1.0
 */
public class BlobDbType extends ResolvedDbType {

  public BlobDbType(int id, String name) {
    super(id, name);
  }

  /**
   * Sets the parameter accounting for the case in which the {@code value} is
   * an {@link InputStream} or a {@link String}, in which case it is consumed into a {@code byte[]} and
   * set.
   * {@inheritDoc}
   */
  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException {
    value = canBeCoercedToBlob(value) ? coerceToBlob(statement, index, value) : value;
    super.setParameterValue(statement, index, value);
  }

  private boolean canBeCoercedToBlob(Object value) {
    return value instanceof byte[] || value instanceof InputStream || value instanceof String;
  }

  private Object coerceToBlob(PreparedStatement statement, int index, Object value) throws SQLException {
    Blob blob = statement.getConnection().createBlob();
    blob.setBytes(1, getBlobBytes(value, index));
    return blob;
  }

  private byte[] getBlobBytes(Object value, int index) {
    if (value instanceof byte[]) {
      return (byte[]) value;
    } else if (value instanceof InputStream) {
      try {
        return toByteArray((InputStream) value);
      } catch (IOException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not consume inputStream in parameter of index " + index), e);
      }
    } else if (value instanceof String) {
      return ((String) value).getBytes();
    }

    throw new IllegalArgumentException(format("Object of class '%s' cannot be coerced into a BLOB", value.getClass().getName()));
  }
}
