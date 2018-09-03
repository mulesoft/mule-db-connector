/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.tooling.valueprovider;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.DbArtifactClassLoaderRunnerConfig;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;

import javax.inject.Inject;

import org.junit.Test;

public class ColumnValueProviderTestCase extends AbstractDbIntegrationTestCase
    implements DbArtifactClassLoaderRunnerConfig {

  @Inject
  ValueProviderService valueProviderService;

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/valueprovider/derby-on-table-row-config.xml"};
  }

  @Test
  public void retrieveSubsubProtocols() {
    Location sourceLocation = Location.builder().globalName("value-providers-id-watermark").addSourcePart().build();
    ValueResult idValues = valueProviderService.getValues(sourceLocation, "idColumn");
    ValueResult watermarkValues = valueProviderService.getValues(sourceLocation, "watermarkColumn");

    assertValues(idValues);
    assertValues(watermarkValues);
  }

  private void assertValues(ValueResult values) {
    assertThat(values.isSuccess(), is(true));
    assertThat(values.getValues(),
               hasItems(valueWithId("POSITION"), valueWithId("NAME"), valueWithId("DESCRIPTION"), valueWithId("PICTURE"),
                        valueWithId("ID")));
  }

}
