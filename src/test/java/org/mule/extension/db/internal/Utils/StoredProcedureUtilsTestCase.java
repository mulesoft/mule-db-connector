/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.Utils;

import static org.junit.Assert.assertEquals;

import org.mule.db.commons.internal.util.StoredProcedureUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Test;

public class StoredProcedureUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void getStoredProcedureName() throws Exception {
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call doSomething(?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call doSomething (?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call doSomething   (?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call   doSomething(?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call   doSomething   (?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call schema.doSomething (?,?,?)").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("call schema.doSomething(?,?,?").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{ call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{    call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{ call schema.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{ call schema.package.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{?=call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{? =call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{? = call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{? =     call doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{?=call schema.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{? =call schema.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething", StoredProcedureUtils.analyzeStoredOperation("{? = call schema.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{? =     call schema.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{?=call schema.package.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{? =call schema.package.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{? = call schema.package.doSomething(?,?,?) }").getFirst());
    assertEquals("doSomething",
                 StoredProcedureUtils.analyzeStoredOperation("{? =     call schema.package.doSomething(?,?,?) }").getFirst());
  }

  @Test
  public void getStoredProcedureSchema() throws Exception {
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("call doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("call doSomething (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("call doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("call   doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("call   doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{ call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{    call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{call doSomething(?,?,?) }"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureOwner("call schema.doSomething (?,?,?)"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureOwner("call schema.doSomething(?,?,?"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureOwner("{call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.of("schema"),
                 StoredProcedureUtils.getStoreProcedureOwner("{call schema.package.doSomething(?,?,?) }"));

    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{? = call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{? =    call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{? =call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoreProcedureOwner("{?=call doSomething(?,?,?) }"));
    assertEquals(Optional.of("schema"), StoredProcedureUtils.getStoreProcedureOwner("{? = call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.of("schema"),
                 StoredProcedureUtils.getStoreProcedureOwner("{? = call schema.package.doSomething(?,?,?) }"));
  }

  @Test
  public void getStoredProcedurePackage() throws Exception {
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call doSomething (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call   doSomething(?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call   doSomething   (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{ call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{    call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call schema.doSomething (?,?,?)"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("call schema.doSomething(?,?,?"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.of("package"),
                 StoredProcedureUtils.getStoredProcedureParentOwner("{call schema.package.doSomething(?,?,?) }"));

    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{? = call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{? =    call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{? =call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{?= call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{?=call doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{? =call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{?= call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.empty(), StoredProcedureUtils.getStoredProcedureParentOwner("{?=call schema.doSomething(?,?,?) }"));
    assertEquals(Optional.of("package"),
                 StoredProcedureUtils.getStoredProcedureParentOwner("{? = call schema.package.doSomething(?,?,?) }"));
    assertEquals(Optional.of("package"),
                 StoredProcedureUtils.getStoredProcedureParentOwner("{? =call schema.package.doSomething(?,?,?) }"));
    assertEquals(Optional.of("package"),
                 StoredProcedureUtils.getStoredProcedureParentOwner("{?= call schema.package.doSomething(?,?,?) }"));
    assertEquals(Optional.of("package"),
                 StoredProcedureUtils.getStoredProcedureParentOwner("{?=call schema.package.doSomething(?,?,?) }"));
  }

}
