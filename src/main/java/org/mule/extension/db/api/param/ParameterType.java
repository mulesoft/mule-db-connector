
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.db.commons.internal.domain.type.DbType;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

/**
 * Allows specifying the type of a given parameter
 *
 * @since 1.0
 * @deprecated since 1.9.4. Replace with equivalent on mule-db-client. To be removed in the next major (2.0).
 */
@Deprecated
public class ParameterType {

  public ParameterType() {}

  public ParameterType(String key, TypeClassifier typeClassifier) {
    this.key = key;
    this.typeClassifier = typeClassifier;
  }

  /**
   * The name of the input parameter.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String key;

  @ParameterGroup(name = "Type")
  private TypeClassifier typeClassifier;

  public String getKey() {
    return key;
  }

  public DbType getDbType() {
    return typeClassifier.getDbType();
  }

  public TypeClassifier getTypeClassifier() {
    return typeClassifier;
  }
}
