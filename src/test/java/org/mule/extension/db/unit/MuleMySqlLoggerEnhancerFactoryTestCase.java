/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.extension.db.internal.domain.logger.MuleMySqlLoggerEnhancerFactory;

import static org.mockito.Mockito.verify;

public class MuleMySqlLoggerEnhancerFactoryTestCase {

  @Test
  public void verifyMuleMySqlLoggerClassIsCalled() {
    MuleMySqlLogger muleMySqlLogger = Mockito.spy(MuleMySqlLoggerEnhancerFactory.getEnhancedLogger());
    String TESTING_ENHANCER = "Testing Enhancer";

    muleMySqlLogger.logInfo(TESTING_ENHANCER);
    verify(muleMySqlLogger).logInfo(TESTING_ENHANCER);
  }

}
