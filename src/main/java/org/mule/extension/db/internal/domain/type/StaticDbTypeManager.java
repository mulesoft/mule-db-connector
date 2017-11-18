/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import org.mule.extension.db.internal.domain.connection.DbConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a way to statically resolve {@link DbType} using a predefined set of types.
 */
public class StaticDbTypeManager implements DbTypeManager {

  private Map<String, DbType> nameVendorTypesMap = new HashMap<>();
  private Map<String, DbType> nameAndIdVendorTypesMap = new HashMap<>();

  public StaticDbTypeManager(List<DbType> vendorTypes) {
    for (DbType vendorType : vendorTypes) {
      this.nameAndIdVendorTypesMap.put(vendorType.getName() + vendorType.getId(), vendorType);
      this.nameVendorTypesMap.put(vendorType.getName(), vendorType);
    }
  }

  @Override
  public DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException {
    if (nameAndIdVendorTypesMap.containsKey(name + id)) {
      return nameAndIdVendorTypesMap.get(name + id);
    } else {
      throw new UnknownDbTypeException(id, name);
    }
  }

  @Override
  public DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException {
    if (nameVendorTypesMap.containsKey(name)) {
      return nameVendorTypesMap.get(name);
    } else {
      throw new UnknownDbTypeException(name);
    }
  }
}
