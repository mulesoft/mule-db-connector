/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;

public class AutoObjectMapper implements ObjectMapper {

  private final ModelMapper mapper;

  public AutoObjectMapper() {
    this.mapper = new ModelMapper();
    this.mapper.getConfiguration()
        .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
        .setFieldMatchingEnabled(true);
  }

  public <S, T> T map(S sourceObj, Class<T> targetType) {
    return this.mapper.map(sourceObj, targetType);
  }
}
