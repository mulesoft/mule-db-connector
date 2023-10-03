/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import org.junit.Test;
import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionParameters;
import org.mule.extension.db.internal.domain.logger.MuleMySqlLoggerEnhancerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MuleMySqlLoggerEnhancerFactoryTestCase {

  @Test
  public void verifyMuleMySqlLoggerClassIsCalled() {
    String TESTING_ENHANCER = "Testing Enhancer";
    MuleMySqlLogger delegate = mock(MuleMySqlLogger.class);
    MuleMySqlLogger logger =
        new MuleMySqlLoggerEnhancerFactory(Thread.currentThread().getContextClassLoader(), delegate).create();

    logger.logInfo(TESTING_ENHANCER);
    verify(delegate).logInfo(TESTING_ENHANCER);
  }

  @Test
  public void verifyMuleMySqlLoggerIsPlacedAsConnectionProperty() {
    MySqlConnectionParameters mySqlConnectionParameters = new MySqlConnectionParameters();

    assertThat(mySqlConnectionParameters.getConnectionProperties().get("logger"), containsString("MuleMySqlLogger"));
  }
}
