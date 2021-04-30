/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.of;
import static java.util.Optional.empty;

import java.util.Optional;

/**
 * Utils for the oracle db connection
 *
 * @since 1.5.2
 */
public class OracleConnectionUtils {

  private OracleConnectionUtils() {}

  public static Optional<String> getOwnerFrom(String typeName) {
    return typeName.contains(".") ? of(typeName.substring(0, typeName.indexOf('.'))) : empty();
  }

  public static String getTypeSimpleName(String typeName) {
    if (!typeName.contains(".")) {
      return typeName;
    } else {
      return typeName.substring(typeName.indexOf('.') + 1);
    }
  }
}