/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.runtime.core.api.util.proxy.TargetInvocationHandler;

import java.lang.reflect.Method;

public class MuleMySqlLoggerInvocationHandler implements TargetInvocationHandler {

  private MuleMySqlLogger target;

  public MuleMySqlLoggerInvocationHandler(final MuleMySqlLogger target) {
    this.target = target;
  }

  @Override
  public Object getTargetObject() {
    return target;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return method.invoke(proxy, args);
  }

}
