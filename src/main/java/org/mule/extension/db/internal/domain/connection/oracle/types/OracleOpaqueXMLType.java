package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleOpaqueXMLType extends OracleXmlTypeHandler {

  private static final int OPAQUEXML_TYPE_ID = 2009;
  private static final String OPAQUEXML_TYPE_NAME = "OPAQUE/XMLTYPE";

  public OracleOpaqueXMLType() {
    super(OPAQUEXML_TYPE_ID, OPAQUEXML_TYPE_NAME);
  }

}
