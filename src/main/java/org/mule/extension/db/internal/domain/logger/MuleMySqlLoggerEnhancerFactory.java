/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.logger;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.mule.extension.db.api.logger.MuleMySqlLogger;

/**
 * Factory class that creates instances of {@link MuleMySqlLogger} wrapped by CGLIB's {@link Enhancer} implementing the
 * available Log interface (either com.mysql.cj.log.Log or com.mysql.jdbc.log.Log) at runtime and delegating the method
 * call to the passed instance.
 */
public class MuleMySqlLoggerEnhancerFactory {

  public static final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";
  public static final String NEW_MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
  private ClassLoader classLoader;
  private MuleMySqlLogger delegatedLogger;

  public MuleMySqlLoggerEnhancerFactory(ClassLoader classLoader, MuleMySqlLogger delegatedLogger) {
    this.classLoader = classLoader;
    this.delegatedLogger = delegatedLogger;
  }

  public MuleMySqlLogger create() {
    Enhancer enhancer = new Enhancer();

    enhancer.setSuperclass(delegatedLogger.getClass());
    enhancer.setClassLoader(classLoader);
    enhancer.setInterfaces(new Class[] {getAvailableMySqlLogInterface()});
    enhancer
        .setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> methodProxy.invoke(delegatedLogger, args));

    return (MuleMySqlLogger) enhancer.create(new Class[] {String.class}, new Object[] {"MySql"});
  }

  private Class<?> getAvailableMySqlLogInterface() {
    try {
      return classLoader.loadClass("com.mysql.cj.log.Log");
    } catch (ClassNotFoundException e) {
      try {
        return classLoader.loadClass("com.mysql.jdbc.log.Log");
      } catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException("Neither class, com.mysql.cj.log.Log or com.mysql.jdbc.log.Log, were found. " +
            "An unsupported driver was provided.", ex);
      }
    }
  }

}
