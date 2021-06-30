/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.unit;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class OracleXmlTypeTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "integration/storedprocedure/stored-procedure-oracle-xmltype-config.xml";
  }

  @Test
  public void testAppConfiguration() throws Exception {
    CoreEvent response = runFlow("oraclesXmlTypeTest");

    assertThat(response.getMessage(), is("SUCCESS"));
  }

}
