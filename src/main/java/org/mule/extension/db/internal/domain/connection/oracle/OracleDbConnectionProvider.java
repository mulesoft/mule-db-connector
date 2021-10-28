/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.mule.db.commons.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.db.commons.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.db.commons.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionParameters.DRIVER_CLASS_NAME;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.mule.extension.db.internal.util.MigrationUtils.mapDataSourceConfig;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.mule.db.commons.api.exception.connection.DbError;
import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.db.commons.internal.domain.connection.DbConnectionProvider;
import org.mule.db.commons.internal.domain.connection.JdbcConnectionFactory;
import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.util.OracleCredentialsMaskUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;


/**
 * Creates connections to a Oracle database
 *
 * @since 1.0
 */
@DisplayName("Oracle Connection")
@Alias("oracle")
@ExternalLib(name = "Oracle JDBC Driver", description = "A JDBC driver which supports connecting to an Oracle Database",
    nameRegexpMatcher = DRIVER_FILE_NAME_PATTERN, requiredClassName = DRIVER_CLASS_NAME, type = JAR)
public class OracleDbConnectionProvider extends DbConnectionProvider {

  private static final String INVALID_CREDENTIALS_ORACLE_CODE = "ORA-01017";
  private static final String UNKNOWN_SID_ORACLE_CODE = "ORA-12505";
  private static final String IO_ERROR = "IO Error: The Network Adapter could not establish the connection";

  @ParameterGroup(name = CONNECTION)
  private OracleConnectionParameters oracleConnectionParameters;

  Map<String, Map<Integer, ResolvedDbType>> resolvedDbTypesCache = new ConcurrentHashMap<>();

  @Override
  public java.util.Optional<DataSource> getDataSource() {
    java.util.Optional<TlsContextFactory> tlsContextFactoryOptional = oracleConnectionParameters.getTlsContextFactory();

    if (tlsContextFactoryOptional.isPresent()) {
      TlsContextFactory tlsContextFactory = tlsContextFactoryOptional.get();
      try {
        Class<?> oracleDataSource = org.apache.commons.lang3.ClassUtils.getClass("oracle.jdbc.pool.OracleDataSource");
        Constructor<?> oracleDataSourceConstructor = oracleDataSource.getConstructor();
        Object oracleDataSourceInstance = oracleDataSourceConstructor.newInstance();

        Properties sslInfo = new Properties();

        // Set the key store, type, and password
        if (tlsContextFactory.isKeyStoreConfigured()) {
          sslInfo.put("javax.net.ssl.keyStore", tlsContextFactory.getKeyStoreConfiguration().getPath());
          sslInfo.put("javax.net.ssl.keyStoreType", tlsContextFactory.getKeyStoreConfiguration().getType());
          sslInfo.put("javax.net.ssl.keyStorePassword", tlsContextFactory.getKeyStoreConfiguration().getPassword());
        }

        // Set the trust store, type, and password
        if (tlsContextFactory.isTrustStoreConfigured()) {
          sslInfo.put("javax.net.ssl.trustStore", tlsContextFactory.getTrustStoreConfiguration().getPath());
          sslInfo.put("javax.net.ssl.trustStoreType", tlsContextFactory.getTrustStoreConfiguration().getType());
          sslInfo.put("javax.net.ssl.trustStorePassword", tlsContextFactory.getTrustStoreConfiguration().getPassword());
        }

        Method setUrlMethod =
            oracleDataSourceInstance.getClass().getMethod("setURL", String.class);
        Method setUserMethod =
            oracleDataSourceInstance.getClass().getMethod("setUser", String.class);
        Method setPasswordMethod =
            oracleDataSourceInstance.getClass().getMethod("setPassword", String.class);
        Method setConnectionPropertiesMethod =
            oracleDataSourceInstance.getClass().getMethod("setConnectionProperties", Properties.class);

        setUrlMethod.invoke(oracleDataSourceInstance, oracleConnectionParameters.getUrl());
        setUserMethod.invoke(oracleDataSourceInstance, oracleConnectionParameters.getUser());
        setPasswordMethod.invoke(oracleDataSourceInstance, oracleConnectionParameters.getPassword());
        setConnectionPropertiesMethod.invoke(oracleDataSourceInstance, sslInfo);

        return of((DataSource) oracleDataSourceInstance);
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException
          | InvocationTargetException e) {
        // no possible ssl connection, kill app
        e.printStackTrace();
        throw new ModuleException(e.getMessage(), CONNECTIVITY, new ConnectionException(e.getMessage()));
      }
    }

    return empty();
  }

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(mapDataSourceConfig(oracleConnectionParameters));
  }

  @Override
  protected JdbcConnectionFactory createJdbcConnectionFactory() {
    return new OracleJdbcConnectionFactory.Builder()
        .withMaskCredentialsFunction(OracleCredentialsMaskUtils::maskUrlUserAndPasswordForOracle).build();
  }

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new OracleDbConnection(connection, super.resolveCustomTypes(), resolvedDbTypesCache);
  }

  @Override
  public java.util.Optional<DbError> getDbVendorErrorType(SQLException e) {
    String message = e.getMessage();
    if (message.contains(INVALID_CREDENTIALS_ORACLE_CODE)) {
      return of(INVALID_CREDENTIALS);
    } else if (message.contains(UNKNOWN_SID_ORACLE_CODE)) {
      return of(INVALID_DATABASE);
    } else if (message.contains(IO_ERROR)) {
      return of(CANNOT_REACH);
    }
    return empty();
  }

}
