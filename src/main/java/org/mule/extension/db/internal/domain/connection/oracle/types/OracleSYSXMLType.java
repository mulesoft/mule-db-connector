/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleSYSXMLType extends OracleXmlTypeHandler {

  private static final int SYS_XML_TYPE_ID = 2009;
  private static final String SYS_XML_TYPE_NAME = "SYS.XMLTYPE";

  public OracleSYSXMLType() {
    super(SYS_XML_TYPE_ID, SYS_XML_TYPE_NAME);
  }

}
