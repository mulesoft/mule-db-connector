/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.unit;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.extension.db.internal.domain.connection.mysql.MySqlConnectionParameters;

import static org.mockito.Mockito.verify;

public class MuleMySqlLoggerEnhancerTestCase {

  private MuleMySqlLogger muleMySqlLogger;
  private final String TESTING_ENHANCER = "Testing Enhancer";

  @Before
  public void setUp() {
    Enhancer enhancer = new Enhancer();
    Class<?> finalInterface = MySqlConnectionParameters.getAvailableInterface();

    enhancer.setClassLoader(Thread.currentThread().getContextClassLoader());
    enhancer.setInterfaces(new Class[] {finalInterface});
    enhancer.setSuperclass(MuleMySqlLogger.class);
    enhancer.setCallback(NoOp.INSTANCE);

    Object enhancedClass = enhancer.create(new Class[] {String.class}, new Object[] {"MySql"});
    muleMySqlLogger = (MuleMySqlLogger) Mockito.spy(enhancedClass);
  }

  @Test
  public void verifyMuleMySqlLoggerClassIsCalled() {
    muleMySqlLogger.logInfo(TESTING_ENHANCER);

    verify(muleMySqlLogger).logInfo(TESTING_ENHANCER);
  }

}
