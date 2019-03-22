/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.List;

import javax.inject.Inject;

/**
 * Database connector functions to create complex JDBC Structures.
 *
 * @since 1.5.1
 */
public class DbFunctions implements Initialisable {

  @Inject
  ExtensionManager extensionManager;

  DbFunctionUtil util = null;

  /**
   * DataWeave function to create JDBC Array objects based on the Array Type to create and the values that conforms the type.
   *
   * @param typeName   The name of the Array type to create
   * @param values     An array of values that conforms the Array Type
   * @param configName The configuration in charge of creating the Array Type
   * @return
   */
  public Object createArray(String configName, String typeName, List values) {
    return util.execute((con, val, jdbcType) -> con.createArrayOf(jdbcType, values.toArray()), values, typeName, configName);
  }

  /**
   * DataWeave function to create JDBC Struct objects based on the Type Name and their correspondent properties.
   *
   * @param typeName   The name of the Struct type to create
   * @param properties An array of values that conforms the Struct properties
   * @param configName The configuration in charge of creating the Struct type
   * @return
   */
  public Object createStruct(String configName, String typeName, List properties) {
    return util.execute((con, val, jdbcType) -> con.createStruct(jdbcType, val.toArray()), properties, typeName, configName);
  }


  @Override
  public void initialise() throws InitialisationException {
    util = new DbFunctionUtil(extensionManager);
  }
}
