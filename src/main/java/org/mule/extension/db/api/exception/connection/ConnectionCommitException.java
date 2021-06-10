/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.api.exception.connection;

/**
 * Thrown to indicate an error while committing a connection
 *
 */
public class ConnectionCommitException extends RuntimeException {

  public ConnectionCommitException(Throwable throwable) {
    super(throwable);
  }
}
