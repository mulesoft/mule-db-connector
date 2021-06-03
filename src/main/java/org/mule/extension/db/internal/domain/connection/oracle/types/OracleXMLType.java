package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleXMLType extends OracleXmlTypeHandler {

  private static final int XMLTYPE_TYPE_ID = 2009;
  private static final String XMLTYPE_TYPE_NAME = "XMLTYPE";

  public OracleXMLType() {
    super(XMLTYPE_TYPE_ID, XMLTYPE_TYPE_NAME);
  }

}
