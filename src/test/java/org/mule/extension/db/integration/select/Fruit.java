/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.Objects;

public class Fruit implements SQLData {


  public Fruit() {}

  public Fruit(long fruitID, String fruitName, int fruitQuantity) {
    this.fruitID = fruitID;
    this.fruitName = fruitName;
    this.fruitQuantity = fruitQuantity;
  }

  long fruitID;
  String fruitName;
  int fruitQuantity;

  public long getFruitID() {
    return fruitID;
  }

  public void setFruitID(int fruitID) {
    this.fruitID = fruitID;
  }

  public String getFruitName() {
    return fruitName;
  }

  public void setFruitName(String fruitName) {
    this.fruitName = fruitName;
  }

  public int getFruitQuantity() {
    return fruitQuantity;
  }

  public void setFruitQuantity(int fruitQuantity) {
    this.fruitQuantity = fruitQuantity;
  }

  @Override
  public String getSQLTypeName() throws SQLException {
    return "FRUIT_RECORD_TYPE";
  }

  @Override
  public void readSQL(SQLInput stream, String typeName) throws SQLException {
    this.fruitID = stream.readLong();
    this.fruitName = stream.readString();
    this.fruitQuantity = stream.readInt();
  }

  @Override
  public void writeSQL(SQLOutput stream) throws SQLException {
    stream.writeLong(fruitID);
    stream.writeString(fruitName);
    stream.writeLong(fruitQuantity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Fruit fruit = (Fruit) o;
    return fruitID == fruit.fruitID &&
        fruitQuantity == fruit.fruitQuantity &&
        Objects.equals(fruitName, fruit.fruitName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fruitID, fruitName, fruitQuantity);
  }
}

