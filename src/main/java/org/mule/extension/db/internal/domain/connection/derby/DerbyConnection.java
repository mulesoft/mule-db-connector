/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import org.mule.db.commons.internal.domain.connection.DefaultDbConnection;
import org.mule.db.commons.internal.domain.query.QueryTemplate;
import org.mule.db.commons.internal.domain.type.DbType;

import java.sql.Connection;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * {@link DefaultDbConnection} implementation for Derby databases
 *
 * @since 1.3.4
 */
public class DerbyConnection extends DefaultDbConnection {

  DerbyConnection(Connection connection, List<DbType> dbTypes, Cache<String, QueryTemplate> cachedTemplates) {
    super(connection, dbTypes, cachedTemplates);
  }

  // We are disabling content streaming for Derby because of a incompatibility between the connector logic and the
  // driver. When the connector is iterating a ResultSet and hits the end of this one, the Derby driver automatically
  // closes all the created streams.
  @Override
  public boolean supportsContentStreaming() {
    return false;
  }
}
