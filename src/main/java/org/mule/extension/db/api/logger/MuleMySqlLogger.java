/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.logger;

import com.mysql.cj.log.Log;
import org.mule.extension.db.internal.domain.connection.mysql.MuleMySqlLoggerInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * Logger which captures the driver logs and dispatches them using the application logger.
 * This is used to prevent logs from MySql driver be printed in the mule container logs.
 *
 * @since 1.0
 */
public class MuleMySqlLogger implements Log {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleMySqlLogger.class);
  private String name;

  /**
   * Creates a new {@link MuleMySqlLogger}
   */
  public MuleMySqlLogger(String name) {
    this.name = name;

    //    MuleMySqlLoggerInvocationHandler handler = new MuleMySqlLoggerInvocationHandler();
    //    MuleMySqlLogger e = null;
    //
    //    try {
    //      Class.forName("com.mysql.cj.jdbc.Driver");
    //      e = (MuleMySqlLogger) Proxy.newProxyInstance(MuleMySqlLogger.class.getClassLoader(),
    //                                                   new Class[] {Class.forName("com.mysql.cj.log.Log")},
    //                                                   handler);
    //    } catch (ClassNotFoundException ex) {
    //      try {
    //        e = (MuleMySqlLogger) Proxy.newProxyInstance(MuleMySqlLogger.class.getClassLoader(),
    //                                                     new Class[] {Class.forName("com.mysql.jdbc.log.Log")},
    //                                                     handler);
    //      } catch (ClassNotFoundException exc) {
    //        // there is no driver loaded, fuck it
    //        exc.printStackTrace();
    //      }
    //    }

  }

  /**
   * {@inheritDoc}
   */
  public boolean isDebugEnabled() {
    return LOGGER.isDebugEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isErrorEnabled() {
    return LOGGER.isErrorEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isFatalEnabled() {
    return LOGGER.isErrorEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isInfoEnabled() {
    return LOGGER.isInfoEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isTraceEnabled() {
    return LOGGER.isTraceEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isWarnEnabled() {
    return LOGGER.isWarnEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public void logDebug(Object msg) {
    LOGGER.debug(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logDebug(Object msg, Throwable e) {
    LOGGER.debug(msg.toString(), e);
  }

  /**
   * {@inheritDoc}
   */
  public void logError(Object msg) {
    LOGGER.error(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logError(Object msg, Throwable e) {
    LOGGER.error(msg.toString(), e);
  }

  /**
   * {@inheritDoc}
   */
  public void logFatal(Object msg) {
    LOGGER.error(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logFatal(Object msg, Throwable e) {
    LOGGER.error(msg.toString(), e);
  }

  /**
   * {@inheritDoc}
   */
  public void logInfo(Object msg) {
    LOGGER.info(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logInfo(Object msg, Throwable e) {
    LOGGER.info(msg.toString(), e);
  }

  /**
   * {@inheritDoc}
   */
  public void logTrace(Object msg) {
    LOGGER.trace(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logTrace(Object msg, Throwable e) {
    LOGGER.trace(msg.toString(), e);
  }

  /**
   * {@inheritDoc}
   */
  public void logWarn(Object msg) {
    LOGGER.warn(msg.toString());
  }

  /**
   * {@inheritDoc}
   */
  public void logWarn(Object msg, Throwable e) {
    LOGGER.warn(msg.toString(), e);
  }
}
