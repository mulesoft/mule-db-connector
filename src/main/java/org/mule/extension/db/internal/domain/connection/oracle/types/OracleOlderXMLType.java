/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleOlderXMLType extends OracleXmlTypeHandler {

  private static final int OPAQUE_TYPE_ID = 2007;
  private static final String XML_TYPE_NAME = "XMLTYPE";

  public OracleOlderXMLType() {
    super(OPAQUE_TYPE_ID, XML_TYPE_NAME);
  }

}