/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.lang.String.format;

import org.mule.extension.db.internal.domain.connection.DbConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a data type for {@link Clob}
 */
public class ClobResolvedDataType extends ResolvedDbType {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClobResolvedDataType.class);

  public ClobResolvedDataType(int id, String name) {
    super(id, name);
  }

  @Override
  public void setParameterValue(PreparedStatement statement, int index, Object value, DbConnection connection)
      throws SQLException {
    if (value != null && !(value instanceof Clob)) {

      String valueString;
      if (value instanceof String) {
        valueString = (String) value;
      } else if (value instanceof InputStream) {
        try {
          valueString = IOUtils.toString((InputStream) value);
        } catch (IOException e) {
          throw new SQLException("Error converting Stream to String to set CLOB object", e);
        }
      } else {
        throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
      }

      try {
        LOGGER.debug("Creating CLOB object");
        Clob clob = statement.getConnection().createClob();
        clob.setString(1, valueString);
        super.setParameterValue(statement, index, clob, connection);
      } catch (Throwable t) {
        // createClob method has been add to JDBC API in version 3.0. Since we have to support any driver that works
        // with JDK 1.8 we try an alternative way to set CLOB objects.
        LOGGER.debug("Error creating CLOB object. Using alternative way to set CLOB object", t);
        statement.setCharacterStream(index, new StringReader(valueString), valueString.length());
      }
    } else {
      super.setParameterValue(statement, index, value, connection);
    }
  }

  static String createUnsupportedTypeErrorMessage(Object value) {
    return format("Cannot create a Clob from a value of type '%s'", value.getClass());
  }
}
