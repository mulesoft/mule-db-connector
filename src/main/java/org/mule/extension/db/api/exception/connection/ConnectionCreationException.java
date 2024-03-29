/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Thrown to indicate an error creating a connection
 * @deprecated since 1.9.4. Replace with equivalent on mule-db-client. To be removed in the next major (2.0).
 */
@Deprecated
@ExcludeFromGeneratedCoverage
public class ConnectionCreationException extends ConnectionException {

  public ConnectionCreationException(String message) {
    super(message);
  }

  public ConnectionCreationException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public ConnectionCreationException(String message, Throwable throwable, DbError dbError) {
    super(message, new ModuleException(dbError, throwable));
  }
}
