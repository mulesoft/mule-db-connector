/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Interface to extract the common {@link ArtifactClassLoaderRunnerConfig} for all DB Test Cases without the
 * need of extend of a unique abstract test case.
 *
 * @since 1.0
 */
@ArtifactClassLoaderRunnerConfig(
    applicationSharedRuntimeLibs = {"org.apache.derby:derby", "mysql:mysql-connector-java", "com.microsoft.sqlserver:mssql-jdbc",
        "com.oracle.database.jdbc:ojdbc8", "org.mule.connectors:mule-db-client", "com.experlog:xapool", "com.mchange:c3p0",
        "com.mchange:mchange-commons-java", "com.github.ben-manes.caffeine:caffeine", "org.apache.commons:commons-lang3",
        "commons-collections:commons-collections", "commons-io:commons-io"})
public interface DbArtifactClassLoaderRunnerConfig {
}
