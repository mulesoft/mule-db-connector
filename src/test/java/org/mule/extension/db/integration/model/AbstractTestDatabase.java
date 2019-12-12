/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.model;

import static org.mule.extension.db.integration.model.RegionManager.NORTHWEST_MANAGER;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.extension.db.integration.DbTestUtil;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTestDatabase {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestDatabase.class);

  public static final Planet[] PLANET_TEST_VALUES = {Planet.VENUS, Planet.EARTH, Planet.MARS};
  public static final Planet[] ADDITIONAL_PLANET_VALUES = {Planet.TATOOINE, Planet.JAKU};

  public static final Language[] LANGUAGES_TEST_VALUES = {Language.SPANISH, Language.GERMAN};

  public static final Alien[] ALIEN_TEST_VALUES = {Alien.MONGUITO, Alien.ET};
  public static final Contact[] CONTACT_TEST_VALUES = {Contact.CONTACT1, Contact.CONTACT2};
  public static final Region[] REGION_TEST_VALUES = {Region.NORTHWEST, Region.SOUTHWEST};
  public static final RegionManager[] REGION_MANAGER_TEST_VALUES = {SOUTHWEST_MANAGER, NORTHWEST_MANAGER};

  public static final String NO_SQLXML_SUPPORT_ERROR = "Database does not support SQLXML type";
  public static final String NO_RESULSET_FROM_FUNCTION_SUPPORT_ERROR =
      "Database does not support returning a resultset from a function";
  public static final String NO_UDT_SUPPORT_ERROR = "Database does not support User Defined Data Types";

  public final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(MetadataFormat.JAVA);

  public void deletePlanetTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM PLANET");
  }

  public void deleteSpaceshipTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM SPACESHIP");
  }

  public void deleteLanguagesTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM LANGUAGES");
  }

  public void truncateSpaceshipTable(Connection connection) throws SQLException {
    executeUpdate(connection, "TRUNCATE TABLE SPACESHIP");
  }

  public abstract void createPlanetTable(Connection connection) throws SQLException;

  public abstract void createSpaceshipTable(Connection connection) throws SQLException;

  public abstract void createLanguagesTable(Connection connection) throws SQLException;

  public abstract void createMathFunctionSchema(Connection connection) throws SQLException;

  public abstract DbTestUtil.DbType getDbType();

  public static void executeDdl(DataSource dataSource, String ddl) throws SQLException {
    Connection connection = dataSource.getConnection();

    try {
      executeDdl(connection, ddl);

    } finally {
      connection.close();
    }
  }

  public static void executeDdl(Connection connection, String ddl) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(ddl);
    }
  }

  public void executeUpdate(Connection connection, String updateSql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      int updated = statement.executeUpdate(updateSql);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated. Query: " + updateSql);
      }
    }
  }

  public final void populatePlanetTable(Connection connection, Planet[] testValues) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Planet planet : testValues) {
      int updated = qr.update(connection, getInsertPlanetSql(planet.getName(), planet.getPosition()));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated. Inserted: " + planet.getName());
      }
    }
  }

  public void addAdditionalPlanets(Connection connection) throws SQLException {
    populatePlanetTable(connection, ADDITIONAL_PLANET_VALUES);
  }

  public void removePlanets(Connection connection, Planet... planets) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Planet planet : planets) {
      int updated = qr.update(connection, getDeletePlanetSql(planet.getName(), planet.getPosition()));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated. Deleted: " + planet.getName());
      }
    }
  }

  protected abstract String getInsertPlanetSql(String name, int position);

  protected String getDeletePlanetSql(String name, int position) {
    return "DELETE FROM PLANET WHERE NAME='" + name + "' AND POSITION=" + position;
  }

  public void createDefaultDatabaseConfig(DataSource dataSource) throws SQLException {
    Connection connection = dataSource.getConnection();
    try {
      connection.setAutoCommit(false);

      createPlanetTestTable(connection);
      createSpaceshipTestTable(connection);
      createLanguagesTestTable(connection);
      createMathFunctionSchema(connection);

      if (supportsXmlType()) {
        createAlienTestTable(connection);
      }

      if (supportsSimpleUdt()) {
        createContactDetailsType(connection);
        createRegionManagersTestTable(connection);

        if (supportsArraysUdt()) {
          createContactsTestTable(connection);
          createRegionsTestTable(connection);
        }
      }

      connection.commit();
    } catch (SQLException e) {
      LOGGER.info("Error creating test database", e);
      connection.rollback();
      throw e;
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  protected boolean supportsArraysUdt() {
    return false;
  }

  protected boolean supportsSimpleUdt() {
    return false;
  }

  protected void createZipArrayType(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createContactDetailsType(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createContactDetailsArrayType(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  private void createContactsTestTable(Connection connection) throws SQLException {
    try {
      deleteContactsTable(connection);
    } catch (Exception e) {
      createContactDetailsArrayType(connection);
      createContactsTable(connection);
    }

    populateContactsTable(connection, CONTACT_TEST_VALUES);
  }

  private void populateContactsTable(Connection connection, Contact[] contacts) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Contact contact : contacts) {
      int updated = qr.update(connection, getInsertContactSql(contact));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated");
      }
    }
  }

  protected String getInsertContactSql(Contact contact) {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createContactsTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void deleteContactsTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createRegionsTestTable(Connection connection) throws SQLException {
    try {
      deleteRegionsTable(connection);
    } catch (Exception e) {
      createZipArrayType(connection);
      createRegionsTable(connection);
    }

    populateRegionsTable(connection, REGION_TEST_VALUES);
  }

  private void createRegionManagersTestTable(Connection connection) throws SQLException {
    try {
      deleteRegionManagersTable(connection);
    } catch (Exception e) {
      createRegionManagersTable(connection);
    }
    populateRegionManagersTable(connection, REGION_MANAGER_TEST_VALUES);
  }

  private void populateRegionsTable(Connection connection, Region[] regions) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Region region : regions) {
      int updated = qr.update(connection, getInsertRegionSql(region));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated");
      }
    }
  }

  protected String getInsertRegionSql(Region region) {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createRegionsTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void deleteRegionsTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void deleteRegionManagersTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  protected void createRegionManagersTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  private void populateRegionManagersTable(Connection connection, RegionManager[] managers) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (RegionManager regionManager : managers) {
      int updated = qr.update(connection, getInsertRegionManagerSql(regionManager));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated");
      }
    }
  }

  protected String getInsertRegionManagerSql(RegionManager regionManager) {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  private void createAlienTestTable(Connection connection) throws SQLException {
    try {
      deleteAlienTable(connection);
    } catch (Exception e) {
      createAlienTable(connection);
    }

    populateAlienTable(connection, ALIEN_TEST_VALUES);
  }

  private void populateAlienTable(Connection connection, Alien[] testValues) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Alien alien : testValues) {
      int updated = qr.update(connection, getInsertAlienSql(alien));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated");
      }
    }
  }

  protected void createAlienTable(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
  }

  protected void deleteAlienTable(Connection connection) throws SQLException {
    executeUpdate(connection, "DELETE FROM ALIEN");
  }

  protected String getInsertAlienSql(Alien alien) {
    throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
  }

  protected boolean supportsXmlType() {
    return false;
  }

  protected void createPlanetTestTable(Connection connection) throws SQLException {
    try {
      deletePlanetTable(connection);
    } catch (Exception e) {
      createPlanetTable(connection);
    }

    populatePlanetTable(connection, PLANET_TEST_VALUES);
  }

  protected void createSpaceshipTestTable(Connection connection) throws SQLException {
    try {
      deleteSpaceshipTable(connection);
    } catch (Exception e) {
      createSpaceshipTable(connection);
    }
  }

  protected void createLanguagesTestTable(Connection connection) throws SQLException {
    try {
      deleteLanguagesTable(connection);
    } catch (Exception e) {
      createLanguagesTable(connection);
    }

    populateLanguagesTable(connection, LANGUAGES_TEST_VALUES);
  }

  public void populateLanguagesTable(Connection connection, Language[] languages) throws SQLException {
    QueryRunner qr = new QueryRunner();

    for (Language language : languages) {
      int updated = qr.update(connection, getInsertLanguageSql(language.getName(), language.getSampleText()));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(updated + " rows updated on languages table");
      }
    }
  }

  public abstract String getInsertLanguageSql(String name, String sampleText);

  public void createStoredProcedure(DataSource dataSource, String sql) throws SQLException {
    Connection connection = dataSource.getConnection();

    try {
      Statement statement = connection.createStatement();

      statement.execute(sql);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  public abstract void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException;

  public void createFunctionGetRecords(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_RESULSET_FROM_FUNCTION_SUPPORT_ERROR);
  }

  public abstract void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureParameterizedUpdatePlanetDescription(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureGetSpanishLanguageSampleText(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureAddOne(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException;

  public abstract void returnNullValue(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException;

  public abstract void createDelayFunction(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureConcatenateStringDate(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureConcatenateStringTimestamp(DataSource dataSource) throws SQLException;

  public abstract void createStoredProcedureExtractReducedBio(DataSource dataSource) throws SQLException;

  public void createStoredProcedureGetAlienDescription(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
  }

  public void createStoredProcedureUpdateAlienDescription(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
  }

  public void createStoredProcedureGetZipCodes(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  public void createStoredProcedureUpdateZipCodes(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  public void createStoredProcedureUpdateContactDetails(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  public void createStoredProcedureGetContactDetails(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  public abstract void createStoredProcedureAddOneDefaultSchema(DataSource dataSource) throws SQLException;

  public void createStoredProcedureGetManagerDetails(DataSource dataSource) throws SQLException {
    throw new UnsupportedOperationException(NO_UDT_SUPPORT_ERROR);
  }

  public MetadataType getIdFieldMetaDataType() {
    return typeBuilder.numberType().build();
  }

  public MetadataType getPositionFieldMetaDataType() {
    return typeBuilder.numberType().build();
  }

  public MetadataType getNameFieldMetaDataType() {
    return typeBuilder.stringType().build();
  }

  public MetadataType getDescriptionFieldMetaDataType() {
    return typeBuilder.stringType().build();
  }

  public Class getIdFieldJavaClass() {
    return Number.class;
  }

  public Class getDefaultAutoGeneratedKeyClass() {
    return Number.class;
  }
}
