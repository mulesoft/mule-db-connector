/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.Utils;

import static org.junit.Assert.assertEquals;

import org.mule.extension.db.internal.util.StoredProcedureUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Test;

public class StoredProcedureUtilsTestCase extends AbstractMuleTestCase {


  @Test
  public void getStoredProcedureName() throws Exception {
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call doSomething(?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call doSomething (?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call doSomething   (?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call   doSomething(?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call   doSomething   (?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call schema.doSomething (?,?,?)"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("call schema.doSomething(?,?,?"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("{ call doSomething(?,?,?) }"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("{    call doSomething(?,?,?) }"));
    assertEquals("doSomething", StoredProcedureUtils.getStoredProcedureName("{call doSomething(?,?,?) }"));
  }

  @Test
  public void getStoredProcedureSchema() throws Exception {
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("call doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("call doSomething (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("call doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("call   doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("call   doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("{ call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("{    call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureSchema("{call doSomething(?,?,?) }"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureSchema("call schema.doSomething (?,?,?)"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureSchema("call schema.doSomething(?,?,?"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureSchema("{call schema.doSomething(?,?,?) }"));
  }

}
