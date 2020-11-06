/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.result.resultset;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.result.row.RowHandler;
import org.mule.extension.db.internal.util.ResultSetCharsetEncodedHandler;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;

import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class SingleResultSetHandler implements ResultSetCharsetEncodedHandler {
    private final RowHandler rowHandler;
    private final Charset charset;

    public SingleResultSetHandler(RowHandler rowHandler) {
        this.rowHandler = rowHandler;
        this.charset = Charset.defaultCharset();
    }

    public SingleResultSetHandler(RowHandler rowHandler, Charset charset) {
        this.rowHandler = rowHandler;
        this.charset = charset;
    }

    @Override
    public Map<String, Object> processResultSet(DbConnection connection, ResultSet resultSet) throws SQLException {
        try {
            if (resultSet.next()) {
                return rowHandler.process(resultSet);
            }
        } finally {
            resultSet.close();
        }

        return new CaseInsensitiveHashMap<>();
    }

    @Override
    public boolean requiresMultipleOpenedResults() {
        return false;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }
}
