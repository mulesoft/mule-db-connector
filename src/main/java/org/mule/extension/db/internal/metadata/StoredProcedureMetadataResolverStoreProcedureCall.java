package org.mule.extension.db.internal.metadata;

import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class StoredProcedureMetadataResolverStoreProcedureCall implements OutputTypeResolver<StoredProcedureCall> {

  public String getCategoryName() {
    return "DbCategory";
  }

  public MetadataType getOutputType(MetadataContext context, StoredProcedureCall storedProcedureCall)
      throws MetadataResolvingException, ConnectionException {
    return context.getTypeBuilder().objectType().build();
  }
}
