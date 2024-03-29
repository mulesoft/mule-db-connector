/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import static org.mule.extension.db.api.param.DbNameConstants.SQL_QUERY_TEXT;

/**
 * Parameters to configure an operation which executes a SQL script
 *
 * @since 1.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class BulkScript {

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

  public BulkScript() {}

  public String getSql() {
    return sql;
  }

  @ExcludeFromGeneratedCoverage
  public void setSql(String sql) {
    this.sql = sql;
  }


  public String getFile() {
    return file;
  }

  @ExcludeFromGeneratedCoverage
  public void setFile(String file) {
    this.file = file;
  }
}
