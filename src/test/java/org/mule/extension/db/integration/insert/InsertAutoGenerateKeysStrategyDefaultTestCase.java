/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.insert;

public class InsertAutoGenerateKeysStrategyDefaultTestCase extends AbstractInsertAutoGeneratedKeyTestCase {

  @Override
  protected Class getIdFieldJavaClass() {
    return testDatabase.getDefaultAutoGeneratedKeyClass();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-auto-generated-key-default-config.xml"};
  }
}
