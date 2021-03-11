/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.Utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.internal.util.OracleCredentialsMaskUtils.maskUrlUserAndPasswordForOracle;

public class OracleCredentialsMaskUtilsTestCase {

  @Test
  public void maskOracleUserAndPasswordUrl() {
    String originalUrl = "jdbc:oracle:thin:secretUser/secretP@ssword@somehost.com:1521/sid";
    String expectedUrl = "jdbc:oracle:thin:<<user>>/<<credentials>>@somehost.com:1521/sid";
    String scapedUrl = maskUrlUserAndPasswordForOracle(originalUrl);

    assertThat(scapedUrl, is(expectedUrl));
  }

}
