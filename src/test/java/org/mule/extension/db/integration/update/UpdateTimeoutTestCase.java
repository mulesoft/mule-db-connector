/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.mule.extension.db.AllureConstants.DbFeature.DB_EXTENSION;

import org.mule.extension.db.integration.AbstractQueryTimeoutTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(DB_EXTENSION)
@Story("Update Statement")
public class UpdateTimeoutTestCase extends AbstractQueryTimeoutTestCase {

  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-timeout-config.xml"};
  }
}
