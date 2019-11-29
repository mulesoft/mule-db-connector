package org.mule.extension.db.integration.connectivity;

import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.mule.extension.db.integration.TestDbConfig.getSqlServerResource;

public class DataSourceSqlServerTestCase extends DataSourcePoolingTestCase {

    private static final String DB_HOST_KEY = "db.host";
    private static final String DB_HOST_VALUE = "0.0.0.0";
    private static final String DB_USER_KEY = "db.user";
    private static final String DB_USER_VALUE = "sa";

    @Before
    public void setUp() {
        System.setProperty(DB_HOST_KEY, DB_HOST_VALUE);
        System.setProperty(DB_USER_KEY, DB_USER_VALUE);
        setConcurrentRequests(2);
    }

    @Parameterized.Parameters(name = "{2}")
    public static List<Object[]> parameters() {
        return getSqlServerResource();
    }

    @Override
    protected String[] getFlowConfigurationResources() {
        return new String[] {"integration/connectivity/sqlserver-db-pooling-config.xml",
                "integration/connectivity/connection-pooling-config.xml"};
    }
}
