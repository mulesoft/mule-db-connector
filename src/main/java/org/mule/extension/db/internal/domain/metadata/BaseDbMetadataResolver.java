/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;

import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.util.LazyValue;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseDbMetadataResolver {

  protected BaseTypeBuilder typeBuilder;
  protected ClassTypeLoader typeLoader;
  private LazyValue<Map<Integer, MetadataType>> dbToMetaDataType = new LazyValue<>(this::initializeDbToMetaDataType);

  protected QueryTemplate parseQuery(String query) {
    return new SimpleQueryTemplateParser().parse(query);
  }

  protected PreparedStatement getStatement(MetadataContext context, QueryTemplate query)
      throws ConnectionException, MetadataResolvingException {
    DbConnection connection = context.<DbConnection>getConnection()
        .orElseThrow(() -> new MetadataResolvingException("A connection is required to resolve Metadata but none was provided",
                                                          INVALID_CONFIGURATION));
    PreparedStatement statement;
    try {
      statement = connection.getJdbcConnection().prepareStatement(query.getSqlText());
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), UNKNOWN, e);
    }
    return statement;
  }

  protected MetadataType getDataTypeMetadataModel(int typeId, String columnClassName) {
    if (typeId == Types.JAVA_OBJECT) {
      return typeLoader.load(columnClassName).orElse(typeBuilder.anyType().build());
    } else if (typeId == Types.STRUCT) {
      try {
        if (Struct.class.isAssignableFrom(Class.forName(columnClassName))) {
          ArrayTypeBuilder arrayTypeBuilder = BaseTypeBuilder.create(JAVA).arrayType();
          arrayTypeBuilder.of().anyType();
          return arrayTypeBuilder.build();
        } else {
          return typeLoader.load(columnClassName).orElse(typeBuilder.anyType().build());
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

    }

    return getDataTypeMetadataModel(typeId);
  }

  protected MetadataType getDataTypeMetadataModel(int columnTypeName) {
    return dbToMetaDataType.get().getOrDefault(columnTypeName, typeBuilder.anyType().build());
  }

  private Map<Integer, MetadataType> initializeDbToMetaDataType() {
    final Map<Integer, MetadataType> dbToMetaDataType = new HashMap<>();

    NumberType numberType = typeBuilder.numberType().build();
    StringType stringType = typeBuilder.stringType().build();
    BinaryType binaryType = typeBuilder.binaryType().build();
    AnyType anyType = typeBuilder.anyType().build();

    dbToMetaDataType.put(Types.BIT, typeBuilder.booleanType().build());
    dbToMetaDataType.put(Types.BOOLEAN, typeBuilder.booleanType().build());

    dbToMetaDataType.put(Types.TINYINT, numberType);
    dbToMetaDataType.put(Types.SMALLINT, numberType);
    dbToMetaDataType.put(Types.INTEGER, numberType);
    dbToMetaDataType.put(Types.BIGINT, numberType);
    dbToMetaDataType.put(Types.FLOAT, numberType);
    dbToMetaDataType.put(Types.REAL, numberType);
    dbToMetaDataType.put(Types.DOUBLE, numberType);
    dbToMetaDataType.put(Types.NUMERIC, numberType);
    dbToMetaDataType.put(Types.DECIMAL, numberType);

    dbToMetaDataType.put(Types.CHAR, stringType);
    dbToMetaDataType.put(Types.VARCHAR, stringType);
    dbToMetaDataType.put(Types.LONGNVARCHAR, stringType);
    dbToMetaDataType.put(Types.CLOB, stringType);
    dbToMetaDataType.put(Types.NCHAR, stringType);
    dbToMetaDataType.put(Types.NVARCHAR, stringType);
    dbToMetaDataType.put(Types.NCLOB, stringType);

    dbToMetaDataType.put(Types.BINARY, binaryType);
    dbToMetaDataType.put(Types.VARBINARY, binaryType);
    dbToMetaDataType.put(Types.LONGVARBINARY, binaryType);
    dbToMetaDataType.put(Types.BLOB, binaryType);

    dbToMetaDataType.put(Types.DATE, typeBuilder.dateType().build());
    dbToMetaDataType.put(Types.TIMESTAMP, typeBuilder.dateType().build());
    dbToMetaDataType.put(Types.TIME, typeBuilder.timeType().build());

    dbToMetaDataType.put(Types.OTHER, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.JAVA_OBJECT, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.DISTINCT, typeBuilder.anyType().build());

    dbToMetaDataType.put(Types.ARRAY, typeBuilder.arrayType().of(anyType).build());

    dbToMetaDataType.put(Types.NULL, typeBuilder.nullType().build());

    dbToMetaDataType.put(Types.SQLXML, BaseTypeBuilder.create(XML).objectType().build());

    dbToMetaDataType.put(Types.STRUCT, typeBuilder.arrayType().of(anyType).build());
    dbToMetaDataType.put(Types.REF, typeLoader.load(Ref.class));
    dbToMetaDataType.put(Types.DATALINK, typeLoader.load(URL.class));
    dbToMetaDataType.put(Types.ROWID, typeLoader.load(RowId.class));

    return dbToMetaDataType;
  }
}
