/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.complextypes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Language;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;

import org.junit.Test;

public class EncodingTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/complextypes/large-object-round-trip.xml"};
  }

  @Test
  public void selectTextWithCharactersFromDifferentCharsets() throws Exception {
    Object value = flowRunner("getLanguage").withVariable("languageName", "Spanish")
        .keepStreamsOpen()
        .run()
        .getMessage()
        .getPayload()
        .getValue();

    String stringValue = IOUtils.toString((CursorStreamProvider) value);

    assertThat(stringValue, is(Language.SPANISH.getSampleText()));
  }

}
