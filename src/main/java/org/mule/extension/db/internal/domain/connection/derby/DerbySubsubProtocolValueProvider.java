/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

/**
 * A static {@link ValueProvider} implementation for Derby Connection provider which hints the available
 * Subsub Protocols.
 *
 * @since 1.1.0
 */
public class DerbySubsubProtocolValueProvider implements ValueProvider {

  private static final Set<Value> subsubProtocols = getValuesFor("directory", "memory", "classpath", "jar");

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return subsubProtocols;
  }
}