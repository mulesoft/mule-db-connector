package org.mule.extension.db.internal.operation;

import org.mule.db.commons.AbstractDbConnector;

import org.mule.db.commons.api.param.QuerySettings;
import org.mule.db.commons.internal.domain.connection.DbConnection;

import org.mule.db.commons.internal.operation.DdlOperations;
import org.mule.db.commons.internal.operation.OperationErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.sql.SQLException;

import static org.mule.db.commons.api.param.DbNameConstants.SQL_QUERY_TEXT;


@Throws(OperationErrorTypeProvider.class)
public class DbDdlOperations extends DdlOperations
{

    /**
     * Enables execution of DDL queries against a database.
     *
     * @param sql        The text of the SQL query to be executed
     * @param settings   Parameters to configure the query
     * @param connector  the acting connector
     * @param connection the acting connection
     * @return the number of affected rows
     */
    @DisplayName("Execute DDL")
    public int executeDdl(@DisplayName(SQL_QUERY_TEXT) @Text String sql,
                          @ParameterGroup(name = QUERY_SETTINGS) QuerySettings settings,
                          @Config AbstractDbConnector connector,
                          @Connection DbConnection connection,
                          StreamingHelper streamingHelper)
        throws SQLException
    {
        return super.executeDdl(sql,settings,connector,connection,streamingHelper);
    }
}