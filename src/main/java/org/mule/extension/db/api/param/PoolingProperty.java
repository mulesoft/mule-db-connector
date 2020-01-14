/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

/**
 * A property to be applied to a connection pool's profile configuration.
 */
@Alias("property")
public class PoolingProperty {

  @Parameter
  @Optional(defaultValue = "")
  private String name;

  @Parameter
  @Optional(defaultValue = "")
  private int value;

  public String getName() {
    return name;
  }

  public int getValue() {
    return value;
  }
}
