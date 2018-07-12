/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.source;

import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * {@link TypeKeysResolver} for {@link RowListener} polling source which retrieves the available tables to poll.
 *
 * @since 1.3.4
 */
public class TableKeyResolver implements TypeKeysResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    Optional<DbConnection> connection = context.getConnection();

    DbConnection dbConnection = connection
            .orElseThrow(() -> new MetadataResolvingException("No connection available to retrieve existing tables", CONNECTION_FAILURE));

    LinkedHashSet<MetadataKey> metadataKeys = new LinkedHashSet<>();
    ResultSet tables;
    try {
      tables = dbConnection.getJdbcConnection().getMetaData().getTables(null, null, "%", null);
      while (tables.next()) {
        metadataKeys.add(MetadataKeyBuilder.newKey(tables.getString(3)).build());
      }
    } catch (SQLException e) {
      throw new MetadataResolvingException("Unexpected error when retrieving existing tables", UNKNOWN, e);
    }

    return metadataKeys;
  }

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

}
