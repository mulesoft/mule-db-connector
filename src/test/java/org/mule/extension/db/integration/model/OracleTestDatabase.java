/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.model;

import org.mule.extension.db.integration.DbTestUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Defines an Oracle test database to use with ojdbc7 driver.
 */
public class OracleTestDatabase extends AbstractTestDatabase {

  private static final String ORACLE_ERROR_OBJECT_ALREADY_EXISTS = "42000";

  @Override
  public DbTestUtil.DbType getDbType() {
    return DbTestUtil.DbType.ORACLE;
  }

  @Override
  public void createPlanetTable(Connection connection) throws SQLException {
    executeDdl(connection,
               "CREATE TABLE PLANET(ID INTEGER NOT NULL PRIMARY KEY,POSITION SMALLINT, NAME VARCHAR(255), PICTURE BLOB, DESCRIPTION CLOB)");

    executeDdl(connection, "CREATE SEQUENCE PLANET_SEQ INCREMENT BY 1 START WITH 1");

    executeDdl(connection,
               "CREATE TRIGGER PLANET_TRIGGER\n" + "BEFORE INSERT ON PLANET\n" + "FOR EACH ROW WHEN (new.ID is null)\n"
                   + "begin\n" + "    select PLANET_SEQ.nextval into :new.ID from dual;\n" + "end;");
  }

  @Override
  public void createSpaceshipTable(Connection connection) throws SQLException {
    executeDdl(connection,
               "CREATE TABLE SPACESHIP(ID INTEGER NOT NULL PRIMARY KEY,MODEL VARCHAR(255), MANUFACTURER VARCHAR(255))");
  }

  @Override
  public void createLanguagesTable(Connection connection) throws SQLException {
    executeDdl(connection, "CREATE TABLE LANGUAGES(NAME VARCHAR(128), SAMPLE_TEXT CLOB)");
  }

  @Override
  public void createMathFunctionSchema(Connection connection) throws SQLException {
    //TODO: Remove the following line, which is only done to be able to create a user/schema without using c## prefix
    executeDdl(connection, "alter session set \"_ORACLE_SCRIPT\"=true");
    String sql = "CREATE USER mathFunction IDENTIFIED BY pass";
    createSchema(connection, sql);
  }

  @Override
  protected String getInsertPlanetSql(String name, int position) {
    return "INSERT INTO PLANET(ID, POSITION, NAME) VALUES (PLANET_SEQ.NEXTVAL, " + position + ", '" + name + "')";
  }

  @Override
  public void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException {

    final String sql = "CREATE OR REPLACE PROCEDURE getTestRecords ( st_cursor OUT SYS_REFCURSOR  )\n" + "     is\n" + " BEGIN\n"
        + "  OPEN st_cursor FOR\n" + "  SELECT * FROM PLANET;\n" + " end;\n";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createFunctionGetRecords(DataSource dataSource) throws SQLException {
    String query =
        "CREATE OR REPLACE FUNCTION getTestRecordsFunction\n" + "RETURN SYS_REFCURSOR\n" + "IS planet_cursor SYS_REFCURSOR;\n"
            + "BEGIN\n" + "  OPEN planet_cursor FOR\n" + "  SELECT * FROM planet;\n" + "  RETURN planet_cursor;\n" + "END;";

    executeDdl(dataSource, query);
  }

  @Override
  public void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updateTestType1 (p_retVal OUT INTEGER)\n" + "AS\n" + "BEGIN\n"
        + "  UPDATE PLANET SET NAME='Mercury' WHERE POSITION=4;\n" + "   p_retVal := SQL%ROWCOUNT;\n" + "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureGetSpanishLanguageSampleText(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE getSpanishLanguageSample(language IN OUT CLOB) IS\n" +
            "BEGIN\n" +
            "   SELECT SAMPLE_TEXT\n" +
            "   INTO language\n" +
            "   FROM LANGUAGES\n" +
            "   WHERE NAME='Spanish';\n" +
            "END getSpanishLanguageSample;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureParameterizedUpdatePlanetDescription(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updatePlanetDescription (p_name IN VARCHAR2, p_description CLOB)\n" +
        "AS\n" +
        "BEGIN\n" +
        "  UPDATE PLANET SET DESCRIPTION=p_description WHERE name=p_name;\n" +
        "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updateParamTestType1 (p_name IN VARCHAR2, p_retVal OUT INTEGER)\n" + "AS\n"
        + "BEGIN\n" + "  UPDATE PLANET SET NAME=p_Name WHERE POSITION=4;\n" + "\n" + "   p_retVal := SQL%ROWCOUNT;\n" + "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE countTestRecords(count OUT NUMBER) IS\n" + "BEGIN\n" + "   SELECT COUNT(*)\n"
        + "   INTO   count\n" + "   FROM   PLANET;\n" + "END countTestRecords;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE getSplitTestRecords ( st_cursor1 OUT SYS_REFCURSOR, st_cursor2 OUT SYS_REFCURSOR  )\n"
            + "is\n" + "BEGIN\n" + "   OPEN st_cursor1 FOR SELECT * FROM PLANET WHERE POSITION <= 2;\n"
            + "   OPEN st_cursor2 FOR SELECT * FROM PLANET WHERE POSITION > 2;\n" + "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE doubleMyInt(MYINT IN OUT NUMBER) IS\n" + "BEGIN\n" + "    SELECT MYINT * 2 \n"
        + "    INTO   MYINT\n" + "    FROM   DUAL;\n" + "END doubleMyInt;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureAddOne(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE mathFunction.addOne(num IN OUT INTEGER) AS\n" +
        "BEGIN\n" +
        "   num := num + 1;\n" +
        "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureAddOneDefaultSchema(DataSource dataSource) throws SQLException {
    String sql = "CREATE OR REPLACE PROCEDURE addOne(num IN OUT INTEGER) AS\n" +
        "BEGIN\n" +
        "   num := num - 1;\n" +
        "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE multiplyInts(INT1 IN NUMBER, INT2 IN NUMBER, RESULT1 OUT NUMBER, INT3 IN NUMBER, RESULT2 OUT NUMBER) IS\n"
            + "BEGIN\n" + "    SELECT INT1 * INT2 \n" + "    INTO   RESULT1\n" + "    FROM   DUAL;\n"
            + "    SELECT INT1 * INT2 * INT3 \n" + "    INTO   RESULT2\n" + "    FROM   DUAL;\n" + "END multiplyInts;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void returnNullValue(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE returnNullValue(STRING1 IN VARCHAR2, STRING2 IN VARCHAR2, RESULT OUT VARCHAR2) IS\n"
            + "BEGIN\n" + "    SELECT null\n" + "    INTO   RESULT\n" + "    FROM   DUAL;\n"
            + "END returnNullValue;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE concatenateStrings(STRING1 IN VARCHAR2, STRING2 IN VARCHAR2, RESULT OUT VARCHAR2) IS\n"
            + "BEGIN\n" + "    SELECT STRING1 || STRING2\n" + "    INTO   RESULT\n" + "    FROM   DUAL;\n"
            + "END concatenateStrings;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createDelayFunction(DataSource dataSource) throws SQLException {

    final String sql = "CREATE OR REPLACE FUNCTION DELAY(seconds number) " + "RETURN number IS " + "targetDate DATE; "
        + "BEGIN SELECT sysdate + seconds * 10/864 INTO targetDate FROM DUAL; " + "LOOP EXIT WHEN SYSDATE >= targetDate; "
        + "END LOOP; " + "RETURN 1; " + "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public Class getIdFieldJavaClass() {
    return BigDecimal.class;
  }

  @Override
  public Class getDefaultAutoGeneratedKeyClass() {
    try {
      return org.apache.commons.lang3.ClassUtils.getClass("oracle.sql.ROWID");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to load ROWDID class");
    }
  }

  @Override
  protected void createAlienTable(Connection connection) throws SQLException {
    String ddl = "CREATE TABLE ALIEN(\n" + "  NAME varchar2(255),\n" + "  DESCRIPTION XMLTYPE)";
    executeDdl(connection, ddl);
  }

  @Override
  protected String getInsertAlienSql(Alien alien) {
    String sql = "INSERT INTO Alien VALUES ('" + alien.getName() + "' , XMLType('" + alien.getXml() + "'))";

    return sql;
  }

  @Override
  public void createStoredProcedureGetAlienDescription(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE getAlienDescription(pName IN VARCHAR2, pDescription OUT XMLType)\n" + "IS\n"
        + "BEGIN\n" + "    select description into pDescription from Alien where name= pName; \n" + "END;\n";

    executeDdl(dataSource, sql);
  }

  @Override
  protected boolean supportsXmlType() {
    return true;
  }

  @Override
  public void createStoredProcedureUpdateAlienDescription(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updateAlienDescription(pName IN VARCHAR2, pDescription in XMLType)\n" + "IS\n"
        + "BEGIN\n" + "    update Alien set description = pDescription where name= pName; \n" + "END;\n";

    executeDdl(dataSource, sql);
  }

  @Override
  protected boolean supportsSimpleUdt() {
    return true;
  }

  @Override
  protected boolean supportsArraysUdt() {
    return true;
  }

  @Override
  protected void createZipArrayType(Connection connection) throws SQLException {
    final String ddl = "CREATE OR REPLACE TYPE ZIPARRAY AS VARRAY(10) OF VARCHAR2(12)";
    executeDdl(connection, ddl);
  }

  @Override
  protected void createContactDetailsType(Connection connection) throws SQLException {
    final String ddl = "CREATE OR REPLACE TYPE CONTACT_DETAILS AS object(" + "DESCRIPTION VARCHAR2(12),"
        + "PHONE_NUMBER VARCHAR2(12)," + "EMAIL_ADDRESS VARCHAR2(100))";

    try {
      executeDdl(connection, ddl);
    } catch (SQLException e) {
      // If the type already exists, ignore the error
      if (!e.getMessage().contains("ORA-02303")) {
        throw e;
      }
    }
  }

  @Override
  protected void createContactDetailsArrayType(Connection connection) throws SQLException {
    final String ddl = "CREATE OR REPLACE TYPE CONTACT_DETAILS_ARRAY AS VARRAY(100) OF CONTACT_DETAILS";
    executeDdl(connection, ddl);
  }

  @Override
  public void createStoredProcedureGetZipCodes(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE getZipCodes(pName IN VARCHAR2, pZipCodes OUT ZIPARRAY) " + "IS " + "BEGIN "
        + "select ZIPS into pZipCodes from REGIONS where REGION_NAME = pName; " + "END;";

    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureUpdateZipCodes(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updateZipCodes(pName IN VARCHAR2, pZipCodes IN ZIPARRAY) "
        + "IS "
        + "BEGIN "
        + "UPDATE REGIONS SET ZIPS = pZipCodes where REGION_NAME = pName; "
        + "END;";

    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureUpdateContactDetails(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updateContactDetails(pName IN VARCHAR2, pDetails IN CONTACT_DETAILS_ARRAY) "
        + "IS "
        + "BEGIN "
        + "UPDATE CONTACTS SET DETAILS = pDetails where CONTACT_NAME= pName;"
        + "END;";

    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStringDate(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE concatStringDate(PDATE IN DATE, PTEXT IN VARCHAR2, RESULT OUT VARCHAR2) " + "IS "
            + "BEGIN\n"
            + "    SELECT PTEXT || TO_CHAR(PDATE, 'YYYY-MM-DD') INTO RESULT" + " FROM   DUAL;\n"
            + "END;";
    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStringTimestamp(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE concatStringTimestamp(PTEXT IN VARCHAR2, PDATE IN TIMESTAMP, RESULT OUT VARCHAR2) " + "IS "
            + "BEGIN\n"
            + "    SELECT PTEXT || TO_CHAR(PDATE, 'YYYY-MM-DD HH24:Mi:SS') INTO RESULT" + " FROM   DUAL;\n"
            + "END;";
    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureExtractReducedBio(DataSource dataSource) throws SQLException {
    // In Oracle DATE and TIMESTAMP are associated to the same type id, that of TIMESTAMP
    // therefore we cannot pass a date formatted as 'YYYY-MM-DD' to TIMESTAMP or DATE for this test.
    final String sql =
        "CREATE OR REPLACE PROCEDURE getReducedBiography(pName IN VARCHAR2, pBirthDate IN DATE, pPlaceBirth IN VARCHAR2, pDied IN TIMESTAMP, pPlaceDeath IN VARCHAR2, pProfession IN VARCHAR2, pAlmaMater IN VARCHAR2, pNationality IN VARCHAR2, pChildren IN INTEGER, pSpouse IN VARCHAR2, pMother IN VARCHAR2, pFather IN VARCHAR2, pBio IN VARCHAR2, pResult OUT VARCHAR2) "
            + "AS "
            + "BEGIN\n"
            + "    SELECT pName || ' was born ' || TO_CHAR(pBirthDate, 'YYYY-MM-DD') || ', in ' || pPlaceBirth || ' and died in ' || pPlaceDeath  INTO   pResult"
            + " FROM   DUAL;\n"
            + "END;";
    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureGetContactDetails(DataSource dataSource) throws SQLException {
    final String sql =
        "CREATE OR REPLACE PROCEDURE getContactDetails(pName IN VARCHAR2, pContactDetails OUT CONTACT_DETAILS_ARRAY) " + "IS "
            + "BEGIN " + "select DETAILS into pContactDetails from CONTACTS where CONTACT_NAME= pName; " + "END;";

    executeDdl(dataSource, sql);
  }

  @Override
  public void createStoredProcedureGetManagerDetails(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE getManagerDetails(pName IN VARCHAR2, pDetails OUT CONTACT_DETAILS) " + "IS "
        + "BEGIN " + "select DETAILS into pDetails from REGION_MANAGERS where REGION_NAME= pName; " + "END;";

    executeDdl(dataSource, sql);
  }

  @Override
  protected String getInsertContactSql(Contact contact) {
    StringBuilder builder =
        new StringBuilder("INSERT INTO CONTACTS VALUES ('").append(contact.getName()).append("', CONTACT_DETAILS_ARRAY(");

    boolean first = true;
    for (ContactDetails contactDetails : contact.getDetails()) {
      if (first) {
        first = false;
      } else {
        builder.append(",");
      }
      builder.append("CONTACT_DETAILS('").append(contactDetails.getDescription()).append("', '")
          .append(contactDetails.getPhoneNumber()).append("', '").append(contactDetails.getEmail()).append("')");
    }
    builder.append("))");

    return builder.toString();
  }

  @Override
  protected void createContactsTable(Connection connection) throws SQLException {
    String ddl = "create table CONTACTS " + "(CONTACT_NAME varchar(32) NOT NULL," + "DETAILS CONTACT_DETAILS_ARRAY NOT NULL,"
        + "PRIMARY KEY (CONTACT_NAME))";

    executeDdl(connection, ddl);
  }

  @Override
  protected void deleteContactsTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM CONTACTS");
  }

  @Override
  protected String getInsertRegionSql(Region region) {
    StringBuilder builder =
        new StringBuilder("INSERT INTO REGIONS VALUES ('").append(region.getName()).append("', ").append(" ZIPARRAY(");

    boolean first = true;
    for (String zipCode : region.getZips()) {
      if (first) {
        first = false;
      } else {
        builder.append(",");
      }
      builder.append(zipCode);
    }
    builder.append("))");

    return builder.toString();
  }

  @Override
  protected void createRegionsTable(Connection connection) throws SQLException {
    String ddl =
        "create table REGIONS " + "(REGION_NAME varchar(32) NOT NULL," + "ZIPS ZIPARRAY NOT NULL," + "PRIMARY KEY (REGION_NAME))";

    executeDdl(connection, ddl);
  }

  @Override
  protected void deleteRegionsTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM REGIONS");
  }

  @Override
  protected void createRegionManagersTable(Connection connection) throws SQLException {
    String ddl = "create table REGION_MANAGERS(" + "REGION_NAME varchar(32) NOT NULL," + "MANAGER_NAME varchar(32) NOT NULL,"
        + "DETAILS CONTACT_DETAILS NOT NULL," + "PRIMARY KEY (REGION_NAME))";

    executeDdl(connection, ddl);
  }

  public void dropTablesTypesProcedures(Connection connection) throws SQLException {
    executeDdlSilently(connection, "DROP PROCEDURE INSERT_FRUIT_AS_TYPE");
    executeDdlSilently(connection, "DROP PROCEDURE INSERT_FRUIT_AS_TABLE");
    executeDdlSilently(connection, "DROP PROCEDURE CREATE_FRUIT_TABLE");
    executeDdlSilently(connection, "DROP TABLE FRUITS_NESTED_TABLE");
    executeDdlSilently(connection, "DROP TABLE FRUITS_AS_TYPE");
    executeDdlSilently(connection, "DROP TABLE FRUITS_AS_TABLE");
    executeDdlSilently(connection, "DROP TYPE FRUIT_ORDER_CONTENTS_TABLE");
    executeDdlSilently(connection, "DROP TYPE FRUIT_RECORD_TYPE");
    executeDdlSilently(connection, "DROP TYPE CREATE_LIST_INPUT_OBJ");
    executeDdlSilently(connection, "DROP TYPE CREATE_LIST_INPUT");
    executeDdlSilently(connection, "DROP TYPE CREATE_LIST_OUTPUT_OBJ");
    executeDdlSilently(connection, "DROP TYPE CREATE_LIST_OUTPUT");
    executeDdlSilently(connection, "DROP TYPE OUTPUT_RESPONSE_OBJ");
    executeDdlSilently(connection, "DROP TYPE OUTPUT_RESPONSE");
    executeDdlSilently(connection, "DROP TYPE OBJECT_WITH_INNER_OBJECT_TYPE");
    executeDdlSilently(connection, "DROP TYPE OBJECTS_TABLE");
    executeDdlSilently(connection, "DROP TYPE OBJECT_TYPE");
    executeDdlSilently(connection, "DROP PROCEDURE STORE_PROCEDURE_NESTED_TYPES");
    executeDdlSilently(connection, "DROP PROCEDURE STORED_PROCEDURE_NESTED_OBJECT_TYPE");
  }

  public void initUdts(Connection connection) throws SQLException {
    dropTablesTypesProcedures(connection);

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE FRUIT_RECORD_TYPE AS OBJECT (\n" +
        "    fruitID integer,\n" +
        "    fruitName varchar2(250),\n" +
        "    fruitQuantity NUMBER(10,3)\n" +
        ");");
    executeDdlSilently(connection, "CREATE OR REPLACE TYPE FRUIT_ORDER_CONTENTS_TABLE AS TABLE OF FRUIT_RECORD_TYPE;");

    executeDdlSilently(connection, "CREATE TABLE \"SYSTEM\".\"FRUITS_AS_TYPE\" \n" +
        "   (\t\"FRUIT\" \"SYSTEM\".\"FRUIT_RECORD_TYPE\" \n" +
        "   )");

    executeDdlSilently(connection, "CREATE TABLE \"SYSTEM\".\"FRUITS_AS_TABLE\" \n" +
        "   (\t\"FRUITID\" NUMBER(*,0), \n" +
        "\t\"FRUITNAME\" VARCHAR2(250 BYTE), \n" +
        "\t\"FRUITQUANTITY\" NUMBER(10,3)\n" +
        "   )");

    executeDdlSilently(connection, "CREATE TABLE SYSTEM.FRUITS_NESTED_TABLE (\n" +
        "   fruitId  NUMBER,\n" +
        "   fruits FRUIT_ORDER_CONTENTS_TABLE )\n" +
        "NESTED TABLE fruits STORE AS FRUITS_AS_NESTED");

    executeDdlSilently(connection, "CREATE OR REPLACE PROCEDURE INSERT_FRUIT_AS_TYPE(fruits in FRUIT_ORDER_CONTENTS_TABLE)\n" +
        "as\n" +
        "BEGIN\n" +
        "FOR i IN fruits.FIRST .. fruits.LAST\n" +
        "  LOOP\n" +
        "\t    INSERT INTO FRUITS_AS_TYPE VALUES (fruits(i));\n" +
        "  END LOOP;\n" +
        "end;");

    executeDdlSilently(connection, "create or replace procedure INSERT_FRUIT_AS_TABLE(param1 in FRUIT_ORDER_CONTENTS_TABLE)\n" +
        "as\n" +
        "BEGIN\n" +
        "INSERT INTO FRUITS_AS_TABLE SELECT * FROM Table(param1);\n" +
        "end;");

    executeDdlSilently(connection, "create or replace procedure CREATE_FRUIT_TABLE(param1 out FRUIT_ORDER_CONTENTS_TABLE)\n" +
        "as\n" +
        "begin\n" +
        "    param1 := FRUIT_ORDER_CONTENTS_TABLE(FRUIT_RECORD_TYPE(123, 'sad', 321));\n" +
        "end;");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE CREATE_LIST_INPUT_OBJ FORCE AS OBJECT\n" +
        "(\n" +
        "  objNumber NUMBER,\n" +
        "  objName VARCHAR(10)\n" +
        ");");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE CREATE_LIST_INPUT AS TABLE OF CREATE_LIST_INPUT_OBJ;");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE CREATE_LIST_OUTPUT_OBJ FORCE AS OBJECT\n" +
        "(\n" +
        "  objOne VARCHAR2(10),\n" +
        "  objTwo VARCHAR2(10)\n" +
        ");");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE CREATE_LIST_OUTPUT AS TABLE OF CREATE_LIST_OUTPUT_OBJ;");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE OUTPUT_RESPONSE_OBJ AS OBJECT \n" +
        "( \n" +
        "  CREATE_LIST_OUTPUT_V CREATE_LIST_OUTPUT,\n" +
        "  CREATE_LIST_INPUT_V CREATE_LIST_INPUT\n" +
        ");");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE OUTPUT_RESPONSE AS TABLE OF OUTPUT_RESPONSE_OBJ;");

    executeDdlSilently(connection, "CREATE OR REPLACE PROCEDURE STORE_PROCEDURE_NESTED_TYPES\n" +
        "(\n" +
        "  IN_VALUE IN CREATE_LIST_INPUT,\n" +
        "  OUT_VALUE OUT NOCOPY CREATE_LIST_OUTPUT,\n" +
        "  RSL OUT NOCOPY OUTPUT_RESPONSE\n" +
        ")\n" +
        "  IS\n" +
        "  BEGIN\n" +
        "    OUT_VALUE := CREATE_LIST_OUTPUT(CREATE_LIST_OUTPUT_OBJ('a','b'), CREATE_LIST_OUTPUT_OBJ('c','d'));\n" +
        "    RSL := OUTPUT_RESPONSE(OUTPUT_RESPONSE_OBJ(CREATE_LIST_OUTPUT(CREATE_LIST_OUTPUT_OBJ('a','b')), CREATE_LIST_INPUT(CREATE_LIST_INPUT_OBJ(1, 'a'))));\n"
        +
        "  END;");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE OBJECT_TYPE AS OBJECT\n" +
        "(\n" +
        "  aVarchar VARCHAR2(20)\n" +
        ");");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE OBJECTS_TABLE AS TABLE OF OBJECT_TYPE;");

    executeDdlSilently(connection, "CREATE OR REPLACE TYPE OBJECT_WITH_INNER_OBJECT_TYPE AS OBJECT\n" +
        "(\n" +
        "  objectsTable OBJECTS_TABLE\n" +
        ");");

    executeDdlSilently(connection, "CREATE OR REPLACE PROCEDURE STORED_PROCEDURE_NESTED_OBJECT_TYPE\n" +
        "(\n" +
        "  RESPONSE OUT OBJECT_WITH_INNER_OBJECT_TYPE\n" +
        ")\n" +
        "IS\n" +
        "BEGIN\n" +
        "  RESPONSE := OBJECT_WITH_INNER_OBJECT_TYPE(OBJECTS_TABLE(OBJECT_TYPE('aSimpleVarchar')));\n" +
        "END;");
  }

  private void executeDdlSilently(Connection connection, String sql) {
    try {
      executeUpdate(connection, sql);
    } catch (SQLException e) {
      //ignore
    }
  }

  @Override
  protected void deleteRegionManagersTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM REGION_MANAGERS");
  }

  @Override
  protected String getInsertRegionManagerSql(RegionManager regionManager) {
    StringBuilder builder = new StringBuilder("INSERT INTO REGION_MANAGERS VALUES ('").append(regionManager.getRegionName())
        .append("', '").append(regionManager.getName()).append("', CONTACT_DETAILS('")
        .append(regionManager.getContactDetails().getDescription()).append("', '")
        .append(regionManager.getContactDetails().getPhoneNumber()).append("', '")
        .append(regionManager.getContactDetails().getEmail()).append("'))");

    return builder.toString();
  }

  @Override
  public String getInsertLanguageSql(String name, String sampleText) {
    return "INSERT INTO LANGUAGES VALUES ('" + name + "', '" + sampleText + "')";
  }

  public void createPersonTable(Connection connection) throws SQLException {
    createPersonType(connection);
    executeDdl(connection, "create or replace type PERSON_TABLE as table of PERSON_TYPE;");
  }

  public void createPersonType(Connection connection) throws SQLException {
    executeDdl(connection, "create or replace type PERSON_TYPE as object ( " +
        "personId varchar2(36), " +
        "name clob, " +
        "age number(3) " +
        ");");
  }

  public void dropPersonTable(Connection connection) throws SQLException {
    executeDdl(connection, "DROP TYPE PERSON_TABLE");
    dropPersonType(connection);
  }

  public void dropPersonType(Connection connection) throws SQLException {
    executeDdl(connection, "DROP TYPE PERSON_TYPE");
  }

  public void createSchema(Connection connection, String sql) throws SQLException {
    try {
      executeDdl(connection, sql);
    } catch (SQLException e) {
      // Ignore exception when user already exists
      if (!ORACLE_ERROR_OBJECT_ALREADY_EXISTS.equals(e.getSQLState())) {
        throw e;
      }
    }
  }

  public void createPackages(Connection connection) throws SQLException {
    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE magicPackageVersionOne AS\n" +
                   "  PROCEDURE addMagicalNumber(num IN OUT NUMBER);\n" +
                   "END magicPackageVersionOne;");

    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE BODY magicPackageVersionOne AS\n" +
                   "  PROCEDURE addMagicalNumber(num IN OUT NUMBER) AS\n" +
                   "    BEGIN\n" +
                   "      num := num + 7;\n" +
                   "    END addMagicalNumber;\n" +
                   "END magicPackageVersionOne;");

    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE magicPackageVersionTwo AS\n" +
                   "  PROCEDURE addMagicalNumber(num IN OUT NUMBER, name OUT VARCHAR);\n" +
                   "  PROCEDURE addMagicalNumberAndACard(num IN OUT NUMBER, name OUT VARCHAR);" +
                   "END magicPackageVersionTwo;");

    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE BODY magicPackageVersionTwo AS\n" +
                   "  PROCEDURE addMagicalNumber(num IN OUT NUMBER, name OUT VARCHAR) AS\n" +
                   "    BEGIN\n" +
                   "     num := num + 9;\n" +
                   "    END addMagicalNumber;\n" +
                   "\n" +
                   "  PROCEDURE addMagicalNumberAndACard(num IN OUT NUMBER, name OUT VARCHAR) AS\n" +
                   "     BEGIN\n" +
                   "        num := num + 11;\n" +
                   "     END addMagicalNumberAndACard;\n" +
                   "END magicPackageVersionTwo;");

    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE magicPackageVersionThree AS\n" +
                   "  PROCEDURE getClob ( result OUT sys_refcursor );" +
                   "END magicPackageVersionThree;");

    executeDdl(connection,
               "CREATE OR REPLACE PACKAGE BODY magicPackageVersionThree AS\n" +
                   "  PROCEDURE getClob ( result OUT sys_refcursor ) AS\n" +
                   "    BEGIN\n" +
                   "        Open result for\n" +
                   "            select to_clob(1) from dual;\n" +
                   "    END getClob;" +
                   "END magicPackageVersionThree;");
  }
}
