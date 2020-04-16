/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.logger;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import org.mule.extension.db.api.logger.MuleMySqlLogger;

/**
 * Factory class that creates instances of {@link MuleMySqlLogger} wrapped by CGLIB's {@link Enhancer} implementing the
 * available Log interface (either com.mysql.cj.log.Log or com.mysql.jdbc.log.Log) at runtime.
 */
public class MuleMySqlLoggerEnhancerFactory {

  public static MuleMySqlLogger getEnhancedLogger() {
    Enhancer enhancer = new Enhancer();

    enhancer.setSuperclass(MuleMySqlLogger.class);
    enhancer.setClassLoader(Thread.currentThread().getContextClassLoader());
    enhancer.setInterfaces(new Class[] {getAvailableMySqlLogInterface()});
    enhancer.setCallback(NoOp.INSTANCE);

    return (MuleMySqlLogger) enhancer.create(new Class[] {String.class}, new Object[] {"MySql"});
  }

  private static Class<?> getAvailableMySqlLogInterface() {
    try {
      return Thread.currentThread().getContextClassLoader().loadClass("com.mysql.cj.log.Log");
    } catch (ClassNotFoundException e) {
      try {
        return Thread.currentThread().getContextClassLoader().loadClass("com.mysql.jdbc.log.Log");
      } catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException("Neither class, com.mysql.cj.log.Log or com.mysql.jdbc.log.Log, were found. " +
            "An unsupported driver was provided.", ex);
      }
    }
  }

}
