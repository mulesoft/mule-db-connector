/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import static java.sql.Types.ARRAY;
import static java.sql.Types.STRUCT;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StructDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbTypeException;

public class ParameterTypeResolverUtils {

  public static DbType resolveDbType(DbTypeManager dbTypeManager, DbConnection connection, int typeId, String typeName) {
    DbType dbType;
    try {
      dbType = dbTypeManager.lookup(connection, typeId, typeName);
      // TODO - MULE-15241 : Fix how DB Connector chooses ResolvedTypes
    } catch (UnknownDbTypeException e) {
      // Type was not found in the type manager, but the DB knows about it
      if (typeId == STRUCT) {
        //Maybe is not defined the type on the Config, but we can still use it.
        dbType = new StructDbType(typeId, typeName);
      } else if (typeId == ARRAY) {
        dbType = new ArrayResolvedDbType(typeId, typeName);
      } else {
        dbType = new ResolvedDbType(typeId, typeName);
      }
    }
    return dbType;
  }

}
