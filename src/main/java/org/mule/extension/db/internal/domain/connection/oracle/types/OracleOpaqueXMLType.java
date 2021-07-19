/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleOpaqueXMLType extends OracleXmlTypeHandler {

  private static final int OPAQUEXML_TYPE_ID = 2009;
  private static final String OPAQUEXML_TYPE_NAME = "OPAQUE/XMLTYPE";

  public OracleOpaqueXMLType() {
    super(OPAQUEXML_TYPE_ID, OPAQUEXML_TYPE_NAME);
  }

}
