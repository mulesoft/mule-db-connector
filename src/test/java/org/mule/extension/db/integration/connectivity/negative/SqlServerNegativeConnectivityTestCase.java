/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.connectivity.negative;

import static org.hamcrest.CoreMatchers.is;
import static org.mule.extension.db.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.extension.db.api.exception.connection.DbError.INVALID_CREDENTIALS;
import static org.mule.extension.db.api.exception.connection.DbError.INVALID_DATABASE;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SqlServerNegativeConnectivityTestCase extends AbstractDbNegativeConnectivityTestCase {

  @Test
  public void invalidHost() {
    utils.assertFailedConnection("sqlServerInvalidHost", IS_CONNECTION_EXCEPTION, is(errorType(CANNOT_REACH)));
  }

  @Test
  public void invalidCredentials() {
    utils.assertFailedConnection("sqlServerInvalidCredentials", IS_CONNECTION_EXCEPTION, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void invalidCredentialsForUser() {
    utils.assertFailedConnection("sqlServerInvalidCredentialsForUser", IS_CONNECTION_EXCEPTION,
                                 is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void invalidDatabase() {
    utils.assertFailedConnection("sqlServerInvalidDatabase", IS_CONNECTION_EXCEPTION, is(errorType(INVALID_DATABASE)));
  }
}
