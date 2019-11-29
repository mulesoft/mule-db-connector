package org.mule.extension.db.integration.connectivity;

import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.mule.extension.db.integration.TestDbConfig.getMySqlResource;

public class DataSourcePoolingMySqlTestCase extends DataSourcePoolingTestCase {

    private static final String DB_HOST_KEY = "db.host";
    private static final String DB_HOST_VALUE = "0.0.0.0";
    private static final String DB_PORT_KEY = "db.port";
    private static final String DB_PORT_VALUE = "3306";

    @Before
    public void setUp() {
        System.setProperty(DB_HOST_KEY, DB_HOST_VALUE);
        System.setProperty(DB_PORT_KEY, DB_PORT_VALUE);
        setConcurrentRequests(2);
    }

    @Parameterized.Parameters(name = "{2}")
    public static List<Object[]> parameters() {
        return getMySqlResource();
    }

    @Override
    protected String[] getFlowConfigurationResources() {
        return new String[] {"integration/connectivity/mysql-db-pooling-config.xml",
                "integration/connectivity/connection-pooling-config.xml"};
    }

}
