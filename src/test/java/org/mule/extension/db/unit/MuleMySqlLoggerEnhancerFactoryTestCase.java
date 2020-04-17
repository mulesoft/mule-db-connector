/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.extension.db.internal.domain.logger.MuleMySqlLoggerEnhancerFactory;

public class MuleMySqlLoggerEnhancerFactoryTestCase {

  @Test
  public void verifyMuleMySqlLoggerClassIsCalled() {
    String TESTING_ENHANCER = "Testing Enhancer";
    MuleMySqlLogger delegate = Mockito.mock(MuleMySqlLogger.class);
    MuleMySqlLogger logger =
        new MuleMySqlLoggerEnhancerFactory(Thread.currentThread().getContextClassLoader(), delegate).create();

    logger.logInfo(TESTING_ENHANCER);
    Mockito.verify(delegate).logInfo(TESTING_ENHANCER);
  }

}
