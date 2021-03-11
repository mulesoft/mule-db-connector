package org.mule.extension.db.internal.domain.connection;

import static org.mule.db.commons.api.param.TransactionIsolation.NOT_CONFIGURED;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.db.commons.api.param.TransactionIsolation;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public abstract class BaseDbConnectionParameters {

  private static final String TRANSACTIONS = "Transactions";

  /**
   * The transaction isolation level to set on the driver when connecting the database.
   */
  @Parameter
  @Optional(defaultValue = "NOT_CONFIGURED")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = TRANSACTIONS)
  private TransactionIsolation transactionIsolation = NOT_CONFIGURED;

  /**
   * Indicates whether or not the created datasource has to support XA transactions. Default is false.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = TRANSACTIONS)
  @DisplayName("Use XA Transactions")
  private boolean useXaTransactions = false;

  public TransactionIsolation getTransactionIsolation() {
    return transactionIsolation;
  }

  public boolean isUseXaTransactions() {
    return useXaTransactions;
  }

}
