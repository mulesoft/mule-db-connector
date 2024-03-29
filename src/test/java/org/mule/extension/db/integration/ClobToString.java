/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.Clob;

/**
 * Converts the value of a CLOB field to a String.
 * <p>
 * This class is required as some databases, in particular MySql, do not return Clob values for fields of "Clob" type. MySql has a
 * set of Clob like types, that accept a Clob value as input, but when selected they just return as a String value.
 * <p>
 * <b>IMPORTANT:</b> Clobs must be read from a open connection, so to use this class be sure to maintain active the same
 * transaction that created the Clob value.
 */
public class ClobToString extends AbstractComponent implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    final Message message = event.getMessage();
    return CoreEvent.builder(event).message(Message.builder(message).value(convert(message.getPayload().getValue())).build())
        .build();
  }

  /**
   * Converts a {@link Clob} to {@link String}
   *
   * @param value to convert. Can be null.
   * @return a the content of the Clob as a {@link String}, the same value if it is already a {@link String} or null
   * @throws IllegalArgumentException if value is not a {@link Clob} or {@link String}
   */
  public String convert(Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Clob) {
      Clob clob = (Clob) value;
      StringBuilder sb = new StringBuilder();
      try {
        Reader reader = clob.getCharacterStream();
        BufferedReader br = new BufferedReader(reader);

        String line;
        while (null != (line = br.readLine())) {
          sb.append(line);
        }
        br.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return sb.toString();
    } else if (value instanceof String) {
      return (String) value;
    } else {
      throw new IllegalArgumentException("Cannot convert object to type: " + (value == null ? "null" : value.getClass()));
    }
  }
}
