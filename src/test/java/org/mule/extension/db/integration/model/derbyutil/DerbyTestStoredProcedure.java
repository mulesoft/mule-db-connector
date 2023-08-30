/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.model.derbyutil;

import org.mule.extension.db.integration.model.ContactDetails;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;

/**
 * Defines stored procedures for testing purposes. Used by reflection from {@link DerbyTestDatabase}
 */
@SuppressWarnings("UnusedDeclaration")
public class DerbyTestStoredProcedure {

  public static final String NULL_PLANET_NAME = "NullLand";

  public static void selectRows(ResultSet[] data1) throws SQLException {

    Connection conn = DriverManager.getConnection("jdbc:default:connection");
    try {
      PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET");
      data1[0] = ps1.executeQuery();
    } finally {
      conn.close();
    }
  }

  public static void updateTestType1() throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      Statement ps1 = conn.createStatement();
      ps1.executeUpdate("UPDATE PLANET SET NAME='Mercury' WHERE POSITION=4");
    } finally {
      conn.close();
    }
  }

  public static void updateParameterizedTestType1(String name) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    if (name == null) {
      name = NULL_PLANET_NAME;
    }

    try {
      Statement ps1 = conn.createStatement();
      ps1.executeUpdate("UPDATE PLANET SET NAME='" + name + "' WHERE POSITION=4");
    } finally {
      conn.close();
    }
  }

  public static void updatePlanetDescription(String name, Clob description) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      PreparedStatement ps1 = conn.prepareStatement("UPDATE PLANET SET DESCRIPTION=? WHERE NAME=?");
      ps1.setClob(1, description);
      ps1.setString(2, name);
      ps1.execute();
    } finally {
      conn.close();
    }
  }

  public static void countTestRecords(int[] count) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      Statement ps1 = conn.createStatement();
      ResultSet resultSet = ps1.executeQuery("SELECT COUNT(*) FROM PLANET");
      resultSet.next();
      count[0] = resultSet.getInt(1);
    } finally {
      conn.close();
    }
  }

  public static void getSpanishLanguageSample(Clob[] language) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      Statement ps1 = conn.createStatement();
      ResultSet resultSet = ps1.executeQuery("SELECT SAMPLE_TEXT FROM LANGUAGES WHERE NAME='Spanish'");
      resultSet.next();
      String value = resultSet.getString(1);
      Clob clobResult = new ClobTest();
      clobResult.setString(1, value);
      language[0] = clobResult;
    } finally {
      conn.close();
    }
  }

  public static void getTestRecords(ResultSet[] data1) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET");
      data1[0] = ps1.executeQuery();
    } finally {
      conn.close();
    }
  }

  public static void getSplitTestRecords(ResultSet[] data1, ResultSet[] data2) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET WHERE POSITION <= 2");
      data1[0] = ps1.executeQuery();

      PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM PLANET WHERE POSITION > 2");
      data2[0] = ps2.executeQuery();
    } finally {
      conn.close();
    }
  }

  public static void doubleMyInt(int[] i) {
    i[0] *= 2;
  }

  public static void multiplyInts(int int1, int int2, int[] result1, int int3, int[] result2) {
    result1[0] = int1 * int2;
    result2[0] = int1 * int2 * int3;
  }

  public static void returnNullValue(String string1, String string2, String[] result) {
    result[0] = null;
  }

  public static void concatenateStrings(String string1, String string2, String[] result) {
    result[0] = string1 + string2;
  }

  public static int timeDelay(int secondsDelay) {
    if (secondsDelay > 0) {
      try {
        Thread.sleep(secondsDelay * 1000);
      } catch (InterruptedException e) {
        return 0;
      }
    }

    return 1;
  }

  public static ContactDetails createContactDetails(String description, String phone, String email) {
    return new ContactDetails(description, phone, email);
  }

  public static void getManagerDetails(String name, ContactDetails[] result) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:default:connection");

    try {
      Statement ps1 = conn.createStatement();
      ResultSet resultSet = ps1.executeQuery("SELECT DETAILS FROM REGION_MANAGERS WHERE REGION_NAME ='" + name + "'");
      resultSet.next();
      result[0] = (ContactDetails) resultSet.getObject(1);
    } finally {
      conn.close();
    }
  }

  public static void concatStringDate(Date date, String text, String[] result) {
    result[0] = text + date.toString();
  }

  public static void concatStringTimestamp(String text, Timestamp timestamp, String[] result) {
    result[0] = text + timestamp.toString();
  }

  public static void getReducedBiography(String name, Date birth_date, String place_birth, Timestamp died, String place_death,
                                         String profession, String alma_mater, String nationality, int children, String spouse,
                                         String mother, String father, Clob bio, String[] result)
      throws SQLException {
    result[0] = name + " was born " + birth_date.toString() + ", in " + place_birth + " and died in " + place_death;
  }

  public static void addOne(int[] num) {
    num[0] += 1;
  }


  public static class ClobTest implements Clob {

    private String str;

    @Override
    public long length() throws SQLException {
      return 0;
    }

    @Override
    public String getSubString(long pos, int length) throws SQLException {
      return null;
    }

    @Override
    public Reader getCharacterStream() throws SQLException {
      return new StringReader(str);
    }

    @Override
    public InputStream getAsciiStream() throws SQLException {
      return null;
    }

    @Override
    public long position(String searchstr, long start) throws SQLException {
      return 0;
    }

    @Override
    public long position(Clob searchstr, long start) throws SQLException {
      return 0;
    }

    @Override
    public int setString(long pos, String str) throws SQLException {
      this.str = str;
      return 0;
    }

    @Override
    public int setString(long pos, String str, int offset, int len) throws SQLException {
      this.str = str;
      return 0;
    }

    @Override
    public OutputStream setAsciiStream(long pos) throws SQLException {
      return null;
    }

    @Override
    public Writer setCharacterStream(long pos) throws SQLException {
      return null;
    }

    @Override
    public void truncate(long len) throws SQLException {

    }

    @Override
    public void free() throws SQLException {

    }

    @Override
    public Reader getCharacterStream(long pos, long length) throws SQLException {
      return null;
    }
  }

}
