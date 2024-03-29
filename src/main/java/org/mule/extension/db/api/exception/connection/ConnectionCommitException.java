/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import org.mule.extension.db.internal.util.ExcludeFromGeneratedCoverage;

/**
 * Thrown to indicate an error while committing a connection
 * @deprecated since 1.9.4. Replace with equivalent on mule-db-client. To be removed in the next major (2.0).
 */
@Deprecated
@ExcludeFromGeneratedCoverage
public class ConnectionCommitException extends RuntimeException {

  public ConnectionCommitException(Throwable throwable) {
    super(throwable);
  }
}
