/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.generic;

import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import org.mule.db.commons.internal.domain.connection.generic.GenericConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.api.connection.ConnectionProvider;

/**
 * {@link ConnectionProvider} that creates connections for any kind of database using a JDBC URL
 * and the required JDBC Driver Class
 */
@DisplayName("Generic Connection")
@Alias("generic")
@ExternalLib(name = "JDBC Driver", description = "A JDBC driver which supports connecting to the Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, type = JAR)
public class DbGenericConnectionProvider extends GenericConnectionProvider {

}
