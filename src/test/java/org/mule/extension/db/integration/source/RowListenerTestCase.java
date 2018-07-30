/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.model.AbstractTestDatabase.ADDITIONAL_PLANET_VALUES;
import static org.mule.extension.db.integration.model.AbstractTestDatabase.PLANET_TEST_VALUES;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.tck.probe.PollingProber.check;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Planet;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

public class RowListenerTestCase extends AbstractDbIntegrationTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  public static List<Map<String, Object>> PAYLOADS;


  public static final class CapturePayloadProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) {
      if (PAYLOADS != null) {
        PAYLOADS.add((Map<String, Object>) event.getMessage().getPayload().getValue());
      }
      return event;
    }
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    PAYLOADS = new CopyOnWriteArrayList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    PAYLOADS = null;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/source/row-listener-config.xml"};
  }

  @Test
  public void listenPlanets() throws Exception {
    listenPlanets("listenPlanets", PLANET_TEST_VALUES);
  }

  @Test
  public void listenPlanetsWithWatermark() throws Exception {
    listenPlanets("listenPlanetsWithWatermark", PLANET_TEST_VALUES);

    PAYLOADS.clear();

    withConnections(testDatabase::addAdditionalPlanets);

    assertAllPresent(ADDITIONAL_PLANET_VALUES);
    assertNonePresent(PLANET_TEST_VALUES);
  }

  @Test
  public void idempotentListen() throws Exception {
    withConnections(connection -> testDatabase.removePlanets(connection, Planet.EARTH, Planet.MARS));
    Planet[] planetsToCreate = {Planet.VENUS};
    withConnections(connection -> testDatabase.populatePlanetTable(connection, planetsToCreate));

    listenPlanets("idempotentListen", planetsToCreate);
  }

  @Test
  public void datasense() throws Exception {
    startFlow("listenPlanets");
    assertPlanetObjectType(getListenerOutputMetadata("PLANET"));
  }

  private ObjectType getListenerOutputMetadata(String table) {
    Location location = builder().globalName("listenPlanets").addSourcePart().build();


    MetadataResult<ComponentMetadataDescriptor<SourceModel>> metadata =
        metadataService.getSourceMetadata(location, newKey(table).build());
    assertThat(metadata.isSuccess(), is(true));
    return (ObjectType) metadata.get().getModel().getOutput().getType();
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void assertAllPresent(Planet[] planets) {
    assertPresence(planets, true);
  }

  private void assertNonePresent(Planet[] planets) {
    assertPresence(planets, false);
  }

  private void assertPresence(Planet[] planets, boolean present) {
    check(TIMEOUT_MILLIS, 500, () -> {
      if (present) {
        assertThat(PAYLOADS, hasSize(planets.length));
      }

      for (Planet planet : planets) {
        if (PAYLOADS.stream().filter(map -> planet.getName().equals(map.get("NAME"))).findFirst().isPresent() == present) {
          return true;
        }
      }

      return false;
    });
  }

  private void listenPlanets(String flowName, Planet[] planets) throws Exception {
    startFlow(flowName);
    assertAllPresent(planets);
  }
}
