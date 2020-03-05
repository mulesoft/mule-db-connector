/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.Parameterized;

public class OracleUDTMetadataTestCase extends AbstractOracleUDTMetadataTestCase {

  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "With-Out-Mapping"});
    }
    return configs;
  }

  @Override
  void validateStructType(MetadataType metadataType) {
    assertThat(metadataType, instanceOf(ArrayType.class));
  }
}
