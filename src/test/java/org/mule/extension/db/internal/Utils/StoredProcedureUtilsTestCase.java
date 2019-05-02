/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.Utils;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.mule.extension.db.internal.util.StoredProcedureUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class StoredProcedureUtilsTestCase extends AbstractMuleTestCase {

  private String sqlQuery;

  public StoredProcedureUtilsTestCase(String sqlQuery) {
    this.sqlQuery = sqlQuery;
  }

  @Parameterized.Parameters
  public static Collection<Object> data() {
    return asList("call doSomething(?,?,?)",
                  "call doSomething (?,?,?)",
                  "call doSomething   (?,?,?)",
                  "call doSomething(?,?,?)",
                  "call   doSomething(?,?,?)",
                  "call   doSomething   (?,?,?)",
                  "call schema.doSomething (?,?,?)",
                  "call schema.doSomething(?,?,?",
                  "call c##schema.doSomething(?,?,?)",
                  "{ call doSomething(?,?,?) }",
                  "{    call doSomething(?,?,?) }",
                  "{call doSomething(?,?,?) }");

  }

  @Test
  public void getStoredProcedureName() throws Exception {
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName(sqlQuery));
  }

}
