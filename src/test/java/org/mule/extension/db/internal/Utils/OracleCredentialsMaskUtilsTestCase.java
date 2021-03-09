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
