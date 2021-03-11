package org.mule.extension.db.internal.domain.connection.generic;

import org.mule.db.commons.internal.domain.connection.DataSourceConfig;
import org.mule.db.commons.internal.domain.connection.DbConnectionParameters;
import org.mule.db.commons.internal.domain.connection.generic.GenericConnectionProvider;
import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.ClassValue;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 *  {@link DbConnectionParameters} for the {@link GenericConnectionProvider}
 *
 * @since 1.0
 */
public class GenericConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  /**
   * JDBC URL to use to connect to the database.
   */
  @Parameter
  @Placement(order = 1)
  @DisplayName("URL")
  private String url;

  /**
   * Fully-qualified name of the database driver class.
   */
  @Parameter
  @Placement(order = 2)
  @ClassValue(extendsOrImplements = "java.sql.Driver")
  private String driverClassName;

  /**
   * Database username
   */
  @Parameter
  @Placement(order = 3)
  @Optional
  private String user;

  /**
   * Database password
   */
  @Parameter
  @Placement(order = 4)
  @Password
  @Optional
  private String password;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getDriverClassName() {
    return driverClassName;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUser() {
    return user;
  }

}
