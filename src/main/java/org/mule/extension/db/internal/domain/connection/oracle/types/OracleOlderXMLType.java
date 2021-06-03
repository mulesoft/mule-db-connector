package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleOlderXMLType extends OracleXmlTypeHandler {

  private static final int OPAQUE_TYPE_ID = 2007;
  private static final String XML_TYPE_NAME = "XMLTYPE";

  public OracleOlderXMLType() {
    super(OPAQUE_TYPE_ID, XML_TYPE_NAME);
  }

}
