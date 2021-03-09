package org.mule.extension.db.internal.util;

import org.mule.db.commons.internal.util.CredentialsMaskUtils;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class OracleCredentialsMaskUtils extends CredentialsMaskUtils {

  public static final Pattern ORACLE_USER = compile("jdbc:oracle:.*:(.*)/.*@");
  public static final Pattern ORACLE_PASSWORD = compile("jdbc:oracle:.*:.*/(.*)@");

  public static String maskUrlUserAndPasswordForOracle(String input) {
    String zarlanga = CredentialsMaskUtils.maskUrlUserAndPassword(input);
    zarlanga = CredentialsMaskUtils.maskUrlPattern(zarlanga, ORACLE_PASSWORD, PASSWORD_MASK, PASSWORD_URL_PREFIX);
    zarlanga = CredentialsMaskUtils.maskUrlPattern(zarlanga, ORACLE_USER, USER_MASK, PASSWORD_URL_PREFIX);
    return zarlanga;
  }

}
