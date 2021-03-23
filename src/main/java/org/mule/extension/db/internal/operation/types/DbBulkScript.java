package org.mule.extension.db.internal.operation.types;

import org.mule.db.commons.api.param.BulkScript;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import static org.mule.db.commons.api.param.DbNameConstants.SQL_QUERY_TEXT;

/**
 * Parameters to configure an operation which executes a SQL script
 *
 * @since 1.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class DbBulkScript implements BulkScript {

  /**
   * The text of the SQL query to be executed
   */
  @Parameter
  @Optional
  @Text
  @DisplayName(SQL_QUERY_TEXT)
  @Placement(order = 1)
  private String sql;

  /**
   * The location of a file to load. The file can point to a resource on the classpath or on a disk.
   */
  @Parameter
  @Optional
  @DisplayName("Script Path")
  private String file;

  public String getSql() {
    return sql;
  }

  public String getFile() {
    return file;
  }

}
