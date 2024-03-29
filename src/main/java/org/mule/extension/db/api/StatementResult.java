/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about the execution of a DML statement
 *
 */
@ExcludeFromGeneratedCoverage
public class StatementResult {

  /**
   * The amount of rows affected by the statement
   */
  private final int affectedRows;

  /**
   * A {@link Map} containing the ids of the keys which were auto generated by the statement. The map's keys are strings which
   * identify the column and the values are {@link BigInteger}s which represent the ids.
   */
  private final Map<String, BigInteger> generatedKeys;

  /**
   * Creates a new instance
   * 
   * @param affectedRows the amount of affected rows
   * @param generatedKeys the auto generated keys
   */
  public StatementResult(int affectedRows, Map<String, BigInteger> generatedKeys) {
    this.affectedRows = affectedRows;
    this.generatedKeys = generatedKeys != null ? unmodifiableMap(new HashMap<>(generatedKeys)) : emptyMap();
  }


  public StatementResult() {
    affectedRows = 0;
    generatedKeys = null;
  }


  /**
   * Creates a new Instance from DB Client Statement Result.
   * @param result
   */
  public StatementResult(org.mule.db.commons.api.StatementResult result) {
    this(result.getAffectedRows(), result.getGeneratedKeys());
  }


  /**
   * @return the amount of affected rows
   */
  public int getAffectedRows() {
    return affectedRows;
  }

  /**
   * @return an immutable {@link Map} with the generated keys
   */
  public Map<String, BigInteger> getGeneratedKeys() {
    return generatedKeys;
  }
}
