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
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class LargeObjectsRoundTripTestCase extends AbstractDbIntegrationTestCase {

  private static final String PAYLOAD = "a text";
  private static final int POSITION = 1234;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/complextypes/large-object-round-trip.xml"};
  }

  @Test
  public void insertClobType() throws Exception {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(PAYLOAD.getBytes());
    insert(byteArrayInputStream, "insertClobType");

    String value = IOUtils.toString((CursorStreamProvider) select("selectClobType"));

    assertThat(value, is(PAYLOAD));
  }

  @Test
  public void insertBlobType() throws Exception {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(PAYLOAD.getBytes());
    insert(byteArrayInputStream, "insertBlobType");

    Object value = select("selectBlobType");

    if (value instanceof CursorStreamProvider) {
      value = IOUtils.toByteArray((CursorStreamProvider) value);
    }

    assertThat(value, is(PAYLOAD.getBytes()));
  }

  private Object select(String flowName) throws Exception {
    return flowRunner(flowName).withVariable("position", POSITION)
        .keepStreamsOpen()
        .run()
        .getMessage()
        .getPayload()
        .getValue();
  }

  private CoreEvent insert(ByteArrayInputStream byteArrayInputStream, String insertBlobType) throws Exception {
    return flowRunner(insertBlobType).withVariable("id", POSITION).withPayload(byteArrayInputStream).run();
  }

}
