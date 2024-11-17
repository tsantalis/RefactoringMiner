/**
 * Copyright (C) 2011 Google, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.transit_data_federation.impl.service_alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.block;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.blockConfiguration;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.lsids;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.route;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.routeCollection;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.serviceIds;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stop;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.time;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.trip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Affects;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlertsCollection;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;

public class ServiceAlertsServiceImplTest {

  private ServiceAlertsServiceImpl _service;

  private File _serviceAlertsPath;

  @Before
  public void setup() throws IOException {
    _service = new ServiceAlertsServiceImpl();

    _serviceAlertsPath = File.createTempFile("Test-", "-"
        + ServiceAlertsServiceImpl.class.getName() + ".pb2");
    _service.setServiceAlertsPath(_serviceAlertsPath);
  }

  @Test
  public void testCreateServiceAlert() {
    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    assertTrue(serviceAlert.hasCreationTime());
    assertTrue(serviceAlert.hasId());
    assertEquals("1", serviceAlert.getId().getAgencyId());
  }

  @Test
  public void testSerialization() throws IOException {
    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    FileInputStream in = new FileInputStream(_serviceAlertsPath);
    ServiceAlertsCollection collection = ServiceAlertsCollection.parseFrom(in);
    in.close();

    assertEquals(1, collection.getServiceAlertsCount());
    ServiceAlert read = collection.getServiceAlerts(0);
    assertEquals(serviceAlert.getId().getAgencyId(), read.getId().getAgencyId());
    assertEquals(serviceAlert.getId().getId(), read.getId().getId());
    assertEquals(serviceAlert.getCreationTime(), read.getCreationTime());
  }

  @Test
  public void testGetAllServiceAlerts() {
    ServiceAlert.Builder builder1 = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert1 = _service.createOrUpdateServiceAlert(builder1,
        "1");

    ServiceAlert.Builder builder2 = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert2 = _service.createOrUpdateServiceAlert(builder2,
        "1");

    List<ServiceAlert> alerts = _service.getAllServiceAlerts();
    assertEquals(2, alerts.size());
    assertTrue(alerts.contains(serviceAlert1));
    assertTrue(alerts.contains(serviceAlert2));
  }

  @Test
  public void testGetServiceAlertsForFederatedAgencyId() {
    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    Affects.Builder affects = Affects.newBuilder();
    affects.setAgencyId("2");
    builder.addAffects(affects);
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    List<ServiceAlert> alerts = _service.getServiceAlertsForFederatedAgencyId("1");
    assertEquals(1, alerts.size());
    assertTrue(alerts.contains(serviceAlert));

    alerts = _service.getServiceAlertsForFederatedAgencyId("2");
    assertEquals(0, alerts.size());
  }

  @Test
  public void testGetServiceAlertForId() {
    ServiceAlert.Builder builder1 = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert1 = _service.createOrUpdateServiceAlert(builder1,
        "1");

    ServiceAlert.Builder builder2 = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert2 = _service.createOrUpdateServiceAlert(builder2,
        "1");

    ServiceAlert alert = _service.getServiceAlertForId(ServiceAlertLibrary.agencyAndId(serviceAlert1.getId()));
    assertSame(serviceAlert1, alert);

    alert = _service.getServiceAlertForId(ServiceAlertLibrary.agencyAndId(serviceAlert2.getId()));
    assertSame(serviceAlert2, alert);

    alert = _service.getServiceAlertForId(new AgencyAndId("1", "dne"));
    assertNull(alert);
  }

  @Test
  public void testGetServiceAlertsForAgencyId() {
    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    Affects.Builder affects = Affects.newBuilder();
    affects.setAgencyId("2");
    builder.addAffects(affects);
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    List<ServiceAlert> alerts = _service.getServiceAlertsForAgencyId(
        System.currentTimeMillis(), "1");
    assertEquals(0, alerts.size());

    alerts = _service.getServiceAlertsForAgencyId(System.currentTimeMillis(),
        "2");
    assertEquals(1, alerts.size());
    assertTrue(alerts.contains(serviceAlert));
  }

  @Test
  public void testGetServiceAlertsForStopCall() {

    /**
     * These alerts should match
     */
    ServiceAlert.Builder builder1 = ServiceAlert.newBuilder();
    Affects.Builder affects1 = Affects.newBuilder();
    affects1.setStopId(ServiceAlertLibrary.id("1", "10020"));
    affects1.setTripId(ServiceAlertLibrary.id("1", "TripA"));
    builder1.addAffects(affects1);
    ServiceAlert serviceAlert1 = _service.createOrUpdateServiceAlert(builder1,
        "1");

    ServiceAlert.Builder builder2 = ServiceAlert.newBuilder();
    Affects.Builder affects2 = Affects.newBuilder();
    affects2.setTripId(ServiceAlertLibrary.id("1", "TripA"));
    builder2.addAffects(affects2);
    ServiceAlert serviceAlert2 = _service.createOrUpdateServiceAlert(builder2,
        "1");

    ServiceAlert.Builder builder3 = ServiceAlert.newBuilder();
    Affects.Builder affects3 = Affects.newBuilder();
    affects3.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    builder3.addAffects(affects3);
    ServiceAlert serviceAlert3 = _service.createOrUpdateServiceAlert(builder3,
        "1");

    ServiceAlert.Builder builder4 = ServiceAlert.newBuilder();
    Affects.Builder affects4 = Affects.newBuilder();
    affects4.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    affects4.setDirectionId("1");
    builder4.addAffects(affects4);
    ServiceAlert serviceAlert4 = _service.createOrUpdateServiceAlert(builder4,
        "1");

    /**
     * These alerts shouldn't match
     */
    ServiceAlert.Builder builder5 = ServiceAlert.newBuilder();
    Affects.Builder affects5 = Affects.newBuilder();
    affects5.setStopId(ServiceAlertLibrary.id("1", "10021"));
    affects5.setTripId(ServiceAlertLibrary.id("1", "TripA"));
    builder5.addAffects(affects5);
    _service.createOrUpdateServiceAlert(builder5, "1");

    ServiceAlert.Builder builder6 = ServiceAlert.newBuilder();
    Affects.Builder affects6 = Affects.newBuilder();
    affects6.setStopId(ServiceAlertLibrary.id("1", "10020"));
    affects6.setTripId(ServiceAlertLibrary.id("1", "TripB"));
    builder6.addAffects(affects6);
    _service.createOrUpdateServiceAlert(builder6, "1");

    ServiceAlert.Builder builder7 = ServiceAlert.newBuilder();
    Affects.Builder affects7 = Affects.newBuilder();
    affects7.setTripId(ServiceAlertLibrary.id("1", "TripB"));
    builder7.addAffects(affects7);
    _service.createOrUpdateServiceAlert(builder7, "1");

    ServiceAlert.Builder builder8 = ServiceAlert.newBuilder();
    Affects.Builder affects8 = Affects.newBuilder();
    affects8.setRouteId(ServiceAlertLibrary.id("1", "RouteY"));
    builder8.addAffects(affects8);
    _service.createOrUpdateServiceAlert(builder8, "1");

    ServiceAlert.Builder builder9 = ServiceAlert.newBuilder();
    Affects.Builder affects9 = Affects.newBuilder();
    affects9.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    affects9.setDirectionId("0");
    builder9.addAffects(affects9);
    _service.createOrUpdateServiceAlert(builder9, "1");

    RouteEntryImpl route = route("RouteX");
    routeCollection("RouteX", route);
    StopEntryImpl stop = stop("10020", 47.0, -122.0);
    TripEntryImpl trip = trip("TripA");
    trip.setRoute(route);
    trip.setDirectionId("1");
    stopTime(0, stop, trip, time(8, 53), 0);
    BlockEntryImpl block = block("block");
    BlockConfigurationEntry blockConfig = blockConfiguration(block,
        serviceIds(lsids("a"), lsids()), trip);

    BlockInstance blockInstance = new BlockInstance(blockConfig,
        System.currentTimeMillis());
    List<ServiceAlert> alerts = _service.getServiceAlertsForStopCall(
        System.currentTimeMillis(), blockInstance,
        blockConfig.getStopTimes().get(0), new AgencyAndId("1", "1111"));
    assertEquals(4, alerts.size());
    assertTrue(alerts.contains(serviceAlert1));
    assertTrue(alerts.contains(serviceAlert2));
    assertTrue(alerts.contains(serviceAlert3));
    assertTrue(alerts.contains(serviceAlert4));
  }

  @Test
  public void testGetServiceAlertsForStopId() {
    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    Affects.Builder affects = Affects.newBuilder();
    affects.setStopId(ServiceAlertLibrary.id("1", "10020"));
    builder.addAffects(affects);
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    List<ServiceAlert> alerts = _service.getServiceAlertsForStopId(
        System.currentTimeMillis(), new AgencyAndId("1", "10020"));
    assertEquals(1, alerts.size());
    assertTrue(alerts.contains(serviceAlert));

    alerts = _service.getServiceAlertsForStopId(System.currentTimeMillis(),
        new AgencyAndId("1", "10021"));
    assertEquals(0, alerts.size());
  }

  @Test
  public void testGetServiceAlertsForVehicleJourney() {

    /**
     * These alerts should match
     */
    ServiceAlert.Builder builder2 = ServiceAlert.newBuilder();
    Affects.Builder affects2 = Affects.newBuilder();
    affects2.setTripId(ServiceAlertLibrary.id("1", "TripA"));
    builder2.addAffects(affects2);
    ServiceAlert serviceAlert2 = _service.createOrUpdateServiceAlert(builder2,
        "1");

    ServiceAlert.Builder builder3 = ServiceAlert.newBuilder();
    Affects.Builder affects3 = Affects.newBuilder();
    affects3.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    builder3.addAffects(affects3);
    ServiceAlert serviceAlert3 = _service.createOrUpdateServiceAlert(builder3,
        "1");

    ServiceAlert.Builder builder4 = ServiceAlert.newBuilder();
    Affects.Builder affects4 = Affects.newBuilder();
    affects4.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    affects4.setDirectionId("1");
    builder4.addAffects(affects4);
    ServiceAlert serviceAlert4 = _service.createOrUpdateServiceAlert(builder4,
        "1");

    /**
     * These alerts shouldn't match
     */
    ServiceAlert.Builder builder1 = ServiceAlert.newBuilder();
    Affects.Builder affects1 = Affects.newBuilder();
    affects1.setStopId(ServiceAlertLibrary.id("1", "10020"));
    affects1.setTripId(ServiceAlertLibrary.id("1", "TripA"));
    builder1.addAffects(affects1);
    _service.createOrUpdateServiceAlert(builder1, "1");

    ServiceAlert.Builder builder7 = ServiceAlert.newBuilder();
    Affects.Builder affects7 = Affects.newBuilder();
    affects7.setTripId(ServiceAlertLibrary.id("1", "TripB"));
    builder7.addAffects(affects7);
    _service.createOrUpdateServiceAlert(builder7, "1");

    ServiceAlert.Builder builder8 = ServiceAlert.newBuilder();
    Affects.Builder affects8 = Affects.newBuilder();
    affects8.setRouteId(ServiceAlertLibrary.id("1", "RouteY"));
    builder8.addAffects(affects8);
    _service.createOrUpdateServiceAlert(builder8, "1");

    ServiceAlert.Builder builder9 = ServiceAlert.newBuilder();
    Affects.Builder affects9 = Affects.newBuilder();
    affects9.setRouteId(ServiceAlertLibrary.id("1", "RouteX"));
    affects9.setDirectionId("0");
    builder9.addAffects(affects9);
    _service.createOrUpdateServiceAlert(builder9, "1");

    RouteEntryImpl route = route("RouteX");
    routeCollection("RouteX", route);
    StopEntryImpl stop = stop("10020", 47.0, -122.0);
    TripEntryImpl trip = trip("TripA");
    trip.setRoute(route);
    trip.setDirectionId("1");
    stopTime(0, stop, trip, time(8, 53), 0);
    BlockEntryImpl block = block("block");
    BlockConfigurationEntry blockConfig = blockConfiguration(block,
        serviceIds(lsids("a"), lsids()), trip);

    BlockTripInstance blockTripInstance = new BlockTripInstance(
        blockConfig.getTrips().get(0), new InstanceState(
            System.currentTimeMillis()));

    List<ServiceAlert> alerts = _service.getServiceAlertsForVehicleJourney(
        System.currentTimeMillis(), blockTripInstance, new AgencyAndId("1",
            "1111"));
    assertEquals(3, alerts.size());
    assertTrue(alerts.contains(serviceAlert2));
    assertTrue(alerts.contains(serviceAlert3));
    assertTrue(alerts.contains(serviceAlert4));
  }

  @Test
  public void testRemoveServiceAlertsForFederatedAgencyId() {

    ServiceAlert.Builder builder1 = ServiceAlert.newBuilder();
    _service.createOrUpdateServiceAlert(builder1, "1");

    ServiceAlert.Builder builder2 = ServiceAlert.newBuilder();
    _service.createOrUpdateServiceAlert(builder2, "1");

    _service.removeAllServiceAlertsForFederatedAgencyId("2");

    assertEquals(2, _service.getAllServiceAlerts().size());
    assertEquals(2, _service.getServiceAlertsForFederatedAgencyId("1").size());

    _service.removeAllServiceAlertsForFederatedAgencyId("1");

    assertEquals(0, _service.getAllServiceAlerts().size());
    assertEquals(0, _service.getServiceAlertsForFederatedAgencyId("1").size());
  }

  @Test
  public void testRemoveServiceAlert() {

    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    ServiceAlert serviceAlert = _service.createOrUpdateServiceAlert(builder,
        "1");

    AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getId());
    _service.removeServiceAlert(id);

    assertNull(_service.getServiceAlertForId(id));
  }

  @Test
  public void testUpdateServiceAlert() {

    ServiceAlert.Builder builder = ServiceAlert.newBuilder();
    Affects.Builder affects = Affects.newBuilder();
    affects.setAgencyId("2");
    builder.addAffects(affects);
    ServiceAlert serviceAlert1 = _service.createOrUpdateServiceAlert(builder,
        "1");

    builder = ServiceAlert.newBuilder(serviceAlert1);
    builder.clearAffects();
    affects = Affects.newBuilder();
    affects.setStopId(ServiceAlertLibrary.id("1", "10020"));
    builder.addAffects(affects);

    ServiceAlert serviceAlert2 = _service.createOrUpdateServiceAlert(builder,
        null);

    List<ServiceAlert> alerts = _service.getServiceAlertsForAgencyId(
        System.currentTimeMillis(), "2");
    assertEquals(0, alerts.size());

    alerts = _service.getServiceAlertsForStopId(System.currentTimeMillis(),
        new AgencyAndId("1", "10020"));
    assertEquals(1, alerts.size());
    assertTrue(alerts.contains(serviceAlert2));
  }
}
