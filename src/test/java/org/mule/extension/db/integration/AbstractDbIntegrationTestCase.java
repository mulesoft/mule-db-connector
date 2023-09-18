/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration;

import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.db.commons.internal.domain.connection.DbConnection;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.test.runner.RunnerDelegateTo;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractDbIntegrationTestCase extends MuleArtifactFunctionalTestCase
    implements DbArtifactClassLoaderRunnerConfig {

  @Parameterized.Parameter(0)
  public String dataSourceConfigResource;

  @Parameterized.Parameter(1)
  public AbstractTestDatabase testDatabase;

  @Parameterized.Parameter(2)
  public DbTestUtil.DbType dbType;

  @Parameterized.Parameter(3)
  public List<String> vendorFlows;

  protected final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  protected final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Before
  public void configDB() throws SQLException {
    executeWithDataSources(dataSource -> testDatabase.createDefaultDatabaseConfig(dataSource));
  }

  protected void withConnections(CheckedConsumer<Connection> connectionConsumer) {
    executeWithDataSources(dataSource -> {
      Connection connection = dataSource.getConnection();
      try {
        connection.setAutoCommit(false);
        connectionConsumer.accept(connection);
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
      } finally {
        connection.close();
      }
    });
  }

  protected void executeWithDataSources(CheckedConsumer<DataSource> dataSourceConsumer) {
    dataSourceConsumer.accept(getDefaultDataSource());
    for (DbConfig dbConfig : additionalConfigs()) {
      dataSourceConsumer.accept(getDataSource(dbConfig.getName(), dbConfig.getVariables()));
    }
  }

  protected Map<String, Object> additionalVariables() {
    return emptyMap();
  }

  protected DataSource getDefaultDataSource() {
    return getDataSource("dbConfig");
  }

  protected DataSource getDataSource(String configName) {
    return getDataSource(configName, additionalVariables());
  }

  protected DataSource getDataSource(String configName, Map<String, Object> variables) {
    try {
      ConfigurationProvider configurationProvider = registry.<ConfigurationProvider>lookupByName(configName).get();
      ConnectionProvider<DbConnection> connectionProviderWrapper =
          configurationProvider
              .get(getEvent(variables))
              .getConnectionProvider().get();

      Method method = unwrapProviderWrapper(connectionProviderWrapper).getClass().getMethod("getConfiguredDataSource");
      return (DataSource) method.invoke(unwrapProviderWrapper(connectionProviderWrapper));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private CoreEvent getEvent(Map<String, Object> variables) throws MuleException {
    CoreEvent.Builder builder = CoreEvent.builder(testEvent());
    variables
        .entrySet()
        .forEach(entry -> builder.addVariable(entry.getKey(), entry.getValue()));
    return builder.build();
  }

  @Override
  protected final String[] getConfigFiles() {
    StringBuilder builder = new StringBuilder();

    builder.append(getDatasourceConfigurationResource());

    for (String resource : getFlowConfigurationResources()) {
      if (builder.length() != 0) {
        builder.append(",");
      }

      builder.append(resource);
    }

    return builder.toString().split(",");
  }

  protected final String getDatasourceConfigurationResource() {
    return dataSourceConfigResource;
  }

  protected abstract String[] getFlowConfigurationResources();

  protected void assertPlanetRecordsFromQuery(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();
    List<Record> records = new ArrayList<>(names.length);

    for (String name : names) {
      addCondition(conditionBuilder, name);
      records.add(new Record(new Field("NAME", replace(name, "'", ""))));
    }

    List<Map<String, String>> result =
        selectData(format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());

    assertRecords(result, records.toArray(new Record[0]));
  }

  protected void assertAffectedRows(StatementResult result, int expected) {
    assertThat(result.getAffectedRows(), is(expected));
  }

  protected void assertDeletedPlanetRecords(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();

    for (String name : names) {
      addCondition(conditionBuilder, name);
    }

    List<Map<String, String>> result =
        selectData(format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());
    assertThat(result.size(), equalTo(0));
  }

  protected void assertPlanetObjectType(ObjectType type) {
    assertThat(type.getFields().size(), equalTo(5));
    assertFieldOfType(type, "ID", testDatabase.getIdFieldMetaDataType());
    assertFieldRequirement(type, "ID", true);
    assertFieldOfType(type, "POSITION", testDatabase.getPositionFieldMetaDataType());
    assertFieldOfType(type, "NAME", typeBuilder.stringType().build());
    switch (testDatabase.getDbType()) {
      case MYSQL: {
        assertFieldOfType(type, "PICTURE", typeBuilder.binaryType().build());
        assertFieldOfType(type, "DESCRIPTION", typeBuilder.anyType().build());
        break;
      }
      case SQLSERVER: {
        assertFieldOfType(type, "PICTURE", typeBuilder.binaryType().build());
        assertFieldOfType(type, "DESCRIPTION", typeBuilder.stringType().build());
        break;
      }
      default: {
        assertFieldOfType(type, "PICTURE", typeBuilder.binaryType().build());
        assertFieldOfType(type, "DESCRIPTION", typeBuilder.stringType().build());
      }
    }
  }

  private String addCondition(StringBuilder conditionBuilder, String name) {
    if (conditionBuilder.length() != 0) {
      conditionBuilder.append(",");
    }

    if (!(startsWith(name, "'") && endsWith(name, "'"))) {
      name = format("'%s'", name);
    }

    conditionBuilder.append(name);
    return name;
  }

  protected Map<String, Object> runProcedure(String flowName) throws Exception {
    return runProcedure(flowName, null);
  }

  protected Map<String, Object> runProcedure(String flowName, Object payload) throws Exception {
    FlowRunner runner = flowRunner(flowName).keepStreamsOpen();
    if (payload != null) {
      runner.withPayload(payload);
    }

    Message response = runner.run().getMessage();
    assertThat(response.getPayload().getValue(), is(instanceOf(Map.class)));
    return (Map<String, Object>) response.getPayload().getValue();
  }

  protected void assertFieldRequirement(ObjectType record, String name, boolean required) {
    Optional<ObjectFieldType> field = record.getFieldByName(name);
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().isRequired(), is(required));
  }

  protected void assertFieldOfType(ObjectType record, String name, MetadataType type) {
    Optional<ObjectFieldType> field = record.getFieldByName(name);
    assertThat(field.isPresent(), is(true));
    assertThat(field.get().getValue(), equalTo(type));
  }

  protected void assertOutputPayload(MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata, MetadataType type) {
    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getModel().getOutput().getType(), is(type));
  }

  public List<DbConfig> additionalConfigs() {
    return emptyList();
  }

  @Override
  protected FlowRunner flowRunner(String flowName) {
    if (vendorFlows.contains(flowName)) {
      return super.flowRunner(flowName + dbType);
    } else {
      return super.flowRunner(flowName);
    }
  }

  public static class DbConfig {

    private final String name;
    private final Map<String, Object> variables;

    public DbConfig(String name, Map<String, Object> variables) {
      this.name = name;
      this.variables = variables;
    }

    public String getName() {
      return name;
    }

    public Map<String, Object> getVariables() {
      return variables;
    }
  }
}
