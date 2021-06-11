/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.unit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
  This test is placed here due to a collision between a necessary dependency (com.oracle.database.xml:xmlparserv2) and
  a package it brings with (org.w3c.dom.validation) and the runtime. More specifically:
  "Attempt to override lookup strategy ParentFirstLookupStrategy for package: org.w3c.dom.validation".
  So this is a FunctionalTestCase.
 */
public class StoredProcedureXMLTypeTestCase {

  @Test
  public void testXMLTypeInputAndOutputParam() throws Exception {
    Alien firstAlien = AbstractTestDatabase.ALIEN_TEST_VALUES[1];
    Alien alienToUpdate = new Alien("SomePlanet", firstAlien.getName(), firstAlien.getGender(), firstAlien.isFriendly());
    Map<String, String> payload = new HashMap<>();
    payload.put("name", alienToUpdate.getName());
    payload.put("description", alienToUpdate.getXml());

    runProcedure("xmlTypeInputParam", payload);

    Map<String, Object> result = runProcedure("xmlTypeOutputParam", payload);
    assertEquals(result.get("description"), alienToUpdate.getXml());
  }

}
