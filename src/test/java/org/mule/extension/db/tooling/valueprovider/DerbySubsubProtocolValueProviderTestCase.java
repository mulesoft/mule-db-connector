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
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;

import org.mule.extension.db.integration.DbArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class DerbySubsubProtocolValueProviderTestCase extends MuleArtifactFunctionalTestCase
    implements DbArtifactClassLoaderRunnerConfig {

  @Inject
  @Named(VALUE_PROVIDER_SERVICE_KEY)
  ValueProviderService valueProviderService;

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "integration/valueprovider/derby-value-provider-config.xml";
  }

  @Test
  public void retrieveSubsubProtocols() {
    ValueResult values =
        valueProviderService.getValues(Location.builder().globalName("dbConfig").addConnectionPart().build(), "subsubProtocol");
    assertThat(values.isSuccess(), is(true));
    assertThat(values.getValues(),
               hasItems(valueWithId("directory"), valueWithId("memory"), valueWithId("jar"), valueWithId("classpath")));
  }

}
