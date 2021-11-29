/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.oracle.util.OracleTNSEntryURLBuilder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 *
 * {@link DataSourceConfig} implementation for Oracle databases.
 *
 * @since 1.0
 */
public class OracleConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
  private static final String JDBC_URL_PREFIX = "jdbc:oracle:thin:@";

  /**
   * Configures the host of the database.
   */
  @Parameter
  @Placement(order = 1)
  private String host;

  /**
   * Configures the port of the database.
   */
  @Parameter
  @Optional(defaultValue = "1521")
  @Placement(order = 2)
  private Integer port;

  /**
   * The user to use for authentication against the database.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String user;

  /**
   * The password to use for authentication against the database.
   */
  @Parameter
  @Optional
  @Placement(order = 4)
  @Password
  private String password;

  /**
   * The name of the database instance.
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  private String instance;

  /**
   * The name of the database service name.
   */
  @Parameter
  @Optional
  @Placement(order = 6)
  private String serviceName;

  /**
   * A factory for TLS contexts. A TLS context is configured with a key store and a trust store. Allows to create TLS secured
   * connections.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = SECURITY_TAB)
  @DisplayName("TLS Context")
  @Summary("The TLS factory used to create TLS secured connections")
  private TlsContextFactory tlsContextFactory;

  @Override
  public String getUrl() {
    checkInstanceAndServiceName();

    StringBuilder buf = new StringBuilder(JDBC_URL_PREFIX);
    return tlsContextFactory != null ? generateSecureUrl() : generateBasicUrl(buf);
  }

  @Override
  public String getDriverClassName() {
    return DRIVER_CLASS_NAME;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public java.util.Optional<TlsContextFactory> getTlsContextFactory() {
    return ofNullable(tlsContextFactory);
  }

  private String generateBasicUrl(StringBuilder buf) {
    buf.append(host);
    buf.append(":");
    buf.append(port);

    if (instance != null) {
      buf.append(":");
      buf.append(instance);
    }
    if (serviceName != null) {
      buf.append("/");
      buf.append(serviceName);
    }

    return buf.toString();
  }

  private String generateSecureUrl() {
    return new OracleTNSEntryURLBuilder()
        .withProtocol("TCPS")
        .withHost(host)
        .withPort(port)
        .withInstanceName(instance)
        .withServiceName(serviceName)
        .build();
  }

  private void checkInstanceAndServiceName() {
    if (instance != null && serviceName != null) {
      String errorMessage = "Instance (SID) : [" + instance + "] and Service Name : [" + serviceName
          + "] were provided at the same time, please configure only one";

      throw new ModuleException(errorMessage, CONNECTIVITY, new ConnectionException(errorMessage));
    }
  }

}
