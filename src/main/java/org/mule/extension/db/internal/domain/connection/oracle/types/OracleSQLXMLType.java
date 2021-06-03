package org.mule.extension.db.internal.domain.connection.oracle.types;

public class OracleSQLXMLType extends OracleXmlTypeHandler {

    private static final int SQLXML_TYPE_ID = 2009;
    private static final String SQLXML_TYPE_NAME = "SQLXML";

    public OracleSQLXMLType() {
        super(SQLXML_TYPE_ID, SQLXML_TYPE_NAME);
    }

}
