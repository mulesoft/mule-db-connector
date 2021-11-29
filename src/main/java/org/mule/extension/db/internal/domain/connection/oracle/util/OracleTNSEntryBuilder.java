package org.mule.extension.db.internal.domain.connection.oracle.util;

import static java.util.Objects.requireNonNull;

/**
 *
 * Builder class to facilitate the creation of Oracle's URLs that use TNS Entries
 *
 * @since 1.11.0
 */
public final class OracleTNSEntryBuilder {

  private String protocol;
  private String host;
  private Integer port;
  private String instanceName;
  private String serviceName;

  public OracleTNSEntryBuilder() {}

  public OracleTNSEntryBuilder withProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public OracleTNSEntryBuilder withHost(String host) {
    this.host = host;
    return this;
  }

  public OracleTNSEntryBuilder withPort(Integer port) {
    this.port = port;
    return this;
  }

  public OracleTNSEntryBuilder withInstanceName(String instanceName) {
    this.instanceName = instanceName;
    return this;
  }

  public OracleTNSEntryBuilder withServiceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public String build() {
    requireNonNull(protocol, "Protocol can't be null");
    requireNonNull(host, "Host can't be null");
    requireNonNull(port, "Port can't be null");

    StringBuilder buf = new StringBuilder();

    buf.append("(DESCRIPTION=");

    buf.append("(ADDRESS=");
    buf.append("(PROTOCOL=").append(protocol).append(")");
    buf.append("(PORT=").append(port).append(")");
    buf.append("(HOST=").append(host).append(")");
    buf.append(")");

    buf.append("(CONNECT_DATA=");
    if (instanceName != null) {
      buf.append("(INSTANCE_NAME=").append(instanceName).append(")");
    }
    if (serviceName != null) {
      buf.append("(SERVICE_NAME=").append(serviceName).append(")");
    }
    buf.append(")");

    return buf.append(")").toString();
  }

}
