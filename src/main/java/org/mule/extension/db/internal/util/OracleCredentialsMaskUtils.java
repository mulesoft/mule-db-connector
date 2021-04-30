/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.util;

import org.mule.db.commons.internal.util.CredentialsMaskUtils;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class OracleCredentialsMaskUtils extends CredentialsMaskUtils {

  public static final Pattern ORACLE_USER = compile("jdbc:oracle:.*:(.*)/.*@");
  public static final Pattern ORACLE_PASSWORD = compile("jdbc:oracle:.*:.*/(.*)@");

  public static String maskUrlUserAndPasswordForOracle(String input) {
    String oracleInputMasked = CredentialsMaskUtils.maskUrlUserAndPassword(input);
    oracleInputMasked =
        CredentialsMaskUtils.maskUrlPattern(oracleInputMasked, ORACLE_PASSWORD, PASSWORD_MASK, PASSWORD_URL_PREFIX);
    oracleInputMasked = CredentialsMaskUtils.maskUrlPattern(oracleInputMasked, ORACLE_USER, USER_MASK, PASSWORD_URL_PREFIX);
    return oracleInputMasked;
  }

}
