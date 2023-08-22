/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle.types;

import oracle.jdbc.OracleTypes;

public class OracleXMLType extends OracleXmlTypeHandler {

  private static final int XMLTYPE_TYPE_ID = OracleTypes.SQLXML;
  private static final String XMLTYPE_TYPE_NAME = "XMLTYPE";

  public OracleXMLType() {
    super(XMLTYPE_TYPE_ID, XMLTYPE_TYPE_NAME);
  }

}
