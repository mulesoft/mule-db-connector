/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.sql.Types.DECIMAL;
import static java.sql.Types.NUMERIC;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.db.commons.internal.domain.type.ResolvedDbType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BigDecimalDbTypeTestCase extends AbstractMuleTestCase {

  private final PreparedStatement statement = mock(PreparedStatement.class);
  private final ResolvedDbType resolvedDbType;
  private final int sqlType;

  public BigDecimalDbTypeTestCase(int sqlType) {
    this.sqlType = sqlType;
    resolvedDbType = new ResolvedDbType(sqlType, "BigDecimalDbType");
  }

  @Parameterized.Parameters
  public static Collection<Object> data() {
    return asList(new Object[] {DECIMAL, NUMERIC});
  }

  @Test
  public void setBigDecimalValue() throws Exception {
    int index = 0;
    BigDecimal bigDecimalValue = new BigDecimal(1234.1234);
    resolvedDbType.setParameterValue(statement, index, bigDecimalValue, null);
    verify(statement).setObject(index, bigDecimalValue, sqlType, bigDecimalValue.scale());
  }

  @Test
  public void setBigDecimalValueFromDouble() throws Exception {
    Double doubleValue = 1234.1234;
    verifyScale(doubleValue, 4);
  }

  @Test
  public void setBigDecimalValueFromFloat() throws Exception {
    Float floatValue = 1234.1234f;
    verifyScale(floatValue, 4);
  }

  @Test
  public void setBigDecimalValueFromInteger() throws Exception {
    Integer integerValue = 1234;
    resolvedDbType.setParameterValue(statement, 0, integerValue, null);
    verify(statement).setObject(anyInt(), any(), anyInt());
  }

  private void verifyScale(Object value, int scale) throws Exception {
    final BigDecimal[] bigDecimal = new BigDecimal[1];
    doAnswer(invocation -> {
      bigDecimal[0] = (BigDecimal) invocation.getArguments()[1];
      return null;
    }).when(statement).setObject(anyInt(), any(), anyInt(), anyInt());
    resolvedDbType.setParameterValue(statement, 0, value, null);
    assertThat(bigDecimal[0].scale(), is(scale));
  }


}
