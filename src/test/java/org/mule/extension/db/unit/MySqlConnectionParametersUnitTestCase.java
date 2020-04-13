/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionParameters;

import static org.hamcrest.MatcherAssert.assertThat;

public class MySqlConnectionParametersUnitTestCase {

  private MySqlConnectionParameters mySqlConnectionParameters;

  @Before
  public void setUpSqlServerConnectionParameters() {
    mySqlConnectionParameters = new MySqlConnectionParameters();
  }

  @Test
  public void validateEnhancedMuleMySqlLoggerClassIsLoaded() {
    assertThat(mySqlConnectionParameters.getConnectionProperties().get("logger"), CoreMatchers.containsString("CGLIB$$"));
  }

}
