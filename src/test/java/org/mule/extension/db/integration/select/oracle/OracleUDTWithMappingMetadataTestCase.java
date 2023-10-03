/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select.oracle;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.select.Fruit;
import org.mule.metadata.api.model.MetadataType;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.Parameterized;

public class OracleUDTWithMappingMetadataTestCase extends AbstractOracleUDTMetadataTestCase {



  @Parameterized.Parameters(name = "{2}-{4}")
  public static List<Object[]> parameters() {
    List<Object[]> oracleResource = TestDbConfig.getOracleResource();
    ArrayList<Object[]> configs = new ArrayList<>();
    if (!oracleResource.isEmpty()) {
      OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      configs.add(new Object[] {"integration/config/oracle-with-column-types-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType(), emptyList(), "With-Mapping"});
    }
    return configs;
  }

  @Override
  void validateStructType(MetadataType metadataType) {
    assertThat(typeLoader.load(Fruit.class), equalTo(metadataType));
  }
}
