/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.model;

import org.mule.extension.db.integration.DbTestUtil;
import org.mule.metadata.api.model.MetadataType;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlServerTestDataBase extends AbstractTestDatabase {

  private static final String MSSQL_ERROR_OBJECT_ALREADY_EXISTS = "S0001";

  @Override
  public DbTestUtil.DbType getDbType() {
    return DbTestUtil.DbType.SQLSERVER;
  }

  public static String SQL_CREATE_SP_UPDATE_TEST_TYPE_1 =
      "CREATE PROCEDURE updateTestType1\n" +
          "AS\n" +
          "BEGIN\n" +
          "    UPDATE PLANET SET NAME='Mercury' WHERE POSITION=4;\n" +
          "END";

  public static String SQL_CREATE_SP_GET_RECORDS =
      "CREATE PROCEDURE getTestRecords\n" +
          "AS\n" +
          "BEGIN\n" +
          "    SELECT * FROM PLANET;\n" +
          "END";

  public static String SQL_CREATE_SP_GET_SPLIT_RECORDS =
      "CREATE PROCEDURE getSplitTestRecords\n" +
          "AS\n" +
          "BEGIN\n" +
          "    SELECT * FROM PLANET WHERE POSITION <= 2; \n" +
          "    SELECT * FROM PLANET WHERE POSITION > 2;\n" +
          "END";

  @Override
  public void createPlanetTable(Connection connection) throws SQLException {
    executeDdl(connection,
               "CREATE TABLE PLANET(\n" +
                   "ID INTEGER NOT NULL IDENTITY (1,1),\n" +
                   "POSITION INTEGER,\n" +
                   "NAME VARCHAR(255), \n" +
                   "PICTURE varbinary(8000), \n" +
                   "DESCRIPTION NTEXT, \n" +
                   "PRIMARY KEY (ID))");
  }

  @Override
  public void createSpaceshipTable(Connection connection) throws SQLException {
    executeDdl(connection,
               "CREATE TABLE SPACESHIP(\n" +
                   "ID INTEGER NOT NULL IDENTITY (1,1),\n" +
                   "MODEL VARCHAR(255), \n" +
                   "MANUFACTURER VARCHAR(255), \n" +
                   "PRIMARY KEY (ID))");
  }

  @Override
  public void createLanguagesTable(Connection connection) throws SQLException {
    executeDdl(connection, "CREATE TABLE LANGUAGES (NAME VARCHAR(128), SAMPLE_TEXT VARCHAR(max))");
  }

  @Override
  public void createMathFunctionSchema(Connection connection) throws SQLException {
    String sql = "CREATE SCHEMA mathFunction";
    createSchema(connection, sql);
  }

  @Override
  protected String getInsertPlanetSql(String name, int position) {
    return "INSERT INTO PLANET(POSITION, NAME) VALUES (" + position + ", '" + name + "')";
  }

  @Override
  public void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS getTestRecords;\n");
    createStoredProcedure(dataSource, SQL_CREATE_SP_GET_RECORDS);
  }

  @Override
  public void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS updateTestType1;\n");
    createStoredProcedure(dataSource, SQL_CREATE_SP_UPDATE_TEST_TYPE_1);
  }

  @Override
  public void createStoredProcedureGetSpanishLanguageSampleText(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS getSpanishLanguageSample;\n");

    final String sql =
        "CREATE PROCEDURE getSpanishLanguageSample(@language VARCHAR(max) OUTPUT) AS\n" +
            "BEGIN\n" +
            "    SET @language = (SELECT SAMPLE_TEXT FROM LANGUAGES WHERE NAME='Spanish')\n" +
            "END;";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureParameterizedUpdatePlanetDescription(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS updatePlanetDescription;\n");

    final String sql =
        "CREATE PROCEDURE updatePlanetDescription(@pName VARCHAR(50),@pDescription NTEXT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    update Planet SET DESCRIPTION=@pDescription where NAME = @pName;\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS updateParamTestType1;\n");

    final String sql = "CREATE PROCEDURE updateParamTestType1(@pName VARCHAR(50))\n" +
        "    AS\n" +
        "    BEGIN\n" +
        "        DECLARE @name varchar(50);\n" +
        "        SET @name = ISNULL(@pName,'NullLand') \n" +
        "\n" +
        "    UPDATE PLANET SET NAME=@name WHERE POSITION=4;\n" +
        "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS countTestRecords;\n");

    final String sql = "CREATE PROCEDURE countTestRecords(@pResult INT OUTPUT)\n" +
        "AS\n" +
        "BEGIN\n" +
        "    set @pResult = (select count(*) from PLANET)\n" +
        "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS getSplitTestRecords;\n");
    createStoredProcedure(dataSource, SQL_CREATE_SP_GET_SPLIT_RECORDS);

  }

  @Override
  public void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS doubleMyInt;\n");

    final String sql =
        "CREATE PROCEDURE doubleMyInt(@pInt INT OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pInt = @pInt * 2\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStringDate(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS concatStringDate;\n");

    final String sql =
        "CREATE PROCEDURE concatStringDate(@pDate DATE, @pText VARCHAR(50), @pResult VARCHAR(100) OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pResult = CONCAT(@pText, @pDate)\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStringTimestamp(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS concatStringTimestamp;\n");

    final String sql =
        "CREATE PROCEDURE concatStringTimestamp(@pText VARCHAR(50), @pTimestamp DATETIME, @pResult VARCHAR(100) OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pResult = CONCAT(@pText, CONVERT(varchar, @pTimestamp, 20))\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureExtractReducedBio(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS getReducedBiography;\n");

    final String sql =
        "CREATE PROCEDURE getReducedBiography(@pName VARCHAR(50), @pBirthDate DATE, @pPlaceBirth VARCHAR(50), @pDied DATETIME, @pPlaceDeath VARCHAR(50), @pProfession VARCHAR(50), @pAlmaMater VARCHAR(50), @pNationality VARCHAR(50), @pChildren INT, @pSpouse VARCHAR(50),  @pMother VARCHAR(50), @pFather VARCHAR(50), @pBio VARCHAR(500), @pResult VARCHAR(100) OUTPUT)\n"
            +
            "AS\n" +
            "BEGIN\n" +
            "  SET @pResult = CONCAT(@pName, ' was born ', @pBirthDate, ', in ', @pPlaceBirth, ' and died in ', @pPlaceDeath)\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }


  @Override
  public void createStoredProcedureAddOne(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS mathFunction.addOne;\n");

    final String sql = "CREATE PROCEDURE mathFunction.addOne(@num INT OUTPUT) AS\n" +
        "BEGIN\n" +
        "    SET @num = @num + 1;\n" +
        "END";
    createStoredProcedure(dataSource, sql);
  }


  @Override
  public void createStoredProcedureAddOneDefaultSchema(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS addOne;\n");

    String sql = "CREATE PROCEDURE addOne(@num INT OUTPUT) AS\n" +
        "BEGIN\n" +
        "    SET @num = @num - 1;\n" +
        "END";
    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS multiplyInts;\n");

    final String sql =
        "CREATE PROCEDURE multiplyInts(@pInt1 INT, @pInt2 INT, @pResult1 INT OUTPUT, @pInt3 INT, @pResult2 INT OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pResult1 = @pInt1 * @pInt2;\n" +
            "    SET @pResult2 = @pInt1 * @pInt2 * @pInt3;\n" +
            "END";
    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void returnNullValue(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS returnNullValue;\n");

    final String sql =
        "CREATE PROCEDURE returnNullValue(@pString1 VARCHAR(50), @pString2 VARCHAR(50), @pResult VARCHAR(100) OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pResult = null\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException {
    executeDdl(dataSource, "DROP PROCEDURE IF EXISTS concatenateStrings;\n");

    final String sql =
        "CREATE PROCEDURE concatenateStrings(@pString1 VARCHAR(50), @pString2 VARCHAR(50), @pResult VARCHAR(100) OUTPUT)\n" +
            "AS\n" +
            "BEGIN\n" +
            "    SET @pResult = CONCAT(@pString1, @pString2)\n" +
            "END";

    createStoredProcedure(dataSource, sql);
  }

  @Override
  public void createDelayFunction(DataSource dataSource) throws SQLException {
    //SQL Server doesn't support delays inside functions
  }

  @Override
  public MetadataType getDescriptionFieldMetaDataType() {
    return typeBuilder.stringType().build();
  }

  @Override
  public String getInsertLanguageSql(String name, String sampleText) {
    return "INSERT INTO LANGUAGES VALUES ('" + name + "', '" + sampleText + "')";
  }

  public void createStoredProcedure(DataSource dataSource, String sql) throws SQLException {
    try {
      super.createStoredProcedure(dataSource, sql);
    } catch (SQLException e) {
      // Ignore exception when stored procedure already exists
      if (!MSSQL_ERROR_OBJECT_ALREADY_EXISTS.equals(e.getSQLState())) {
        throw e;
      }
    }
  }

  public void createSchema(Connection connection, String sql) throws SQLException {
    try {
      executeDdl(connection, sql);
    } catch (SQLException e) {
      // Ignore exception when user already exists
      if (!MSSQL_ERROR_OBJECT_ALREADY_EXISTS.equals(e.getSQLState())) {
        throw e;
      }
    }
  }
}
