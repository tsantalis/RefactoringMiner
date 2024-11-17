/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Affects;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Id;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlertsCollection;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ServiceAlertsServiceImpl implements ServiceAlertsService {

  private static Logger _log = LoggerFactory.getLogger(ServiceAlertsServiceImpl.class);

  private ConcurrentMap<AgencyAndId, ServiceAlert> _serviceAlerts = new ConcurrentHashMap<AgencyAndId, ServiceAlert>();

  /**
   * This map groups service alert ids by the agency id in their
   * {@link ServiceAlert#getId()} id.
   */
  private ConcurrentMap<String, Set<AgencyAndId>> _serviceAlertIdsByServiceAlertAgencyId = new ConcurrentHashMap<String, Set<AgencyAndId>>();

  /**
   * This map groups service alert ids by any agency id mentioned in
   * {@link Affects#getAgencyId()}.
   */
  private ConcurrentMap<String, Set<AgencyAndId>> _serviceAlertIdsByAgencyId = new ConcurrentHashMap<String, Set<AgencyAndId>>();

  private ConcurrentMap<AgencyAndId, Set<AgencyAndId>> _serviceAlertIdsByStopId = new ConcurrentHashMap<AgencyAndId, Set<AgencyAndId>>();

  private ConcurrentMap<AgencyAndId, Set<AgencyAndId>> _serviceAlertIdsByRouteId = new ConcurrentHashMap<AgencyAndId, Set<AgencyAndId>>();

  private ConcurrentMap<RouteAndDirectionRef, Set<AgencyAndId>> _serviceAlertIdsByRouteAndDirectionId = new ConcurrentHashMap<RouteAndDirectionRef, Set<AgencyAndId>>();

  private ConcurrentMap<RouteAndStopCallRef, Set<AgencyAndId>> _serviceAlertIdsByRouteAndStop = new ConcurrentHashMap<RouteAndStopCallRef, Set<AgencyAndId>>();

  private ConcurrentMap<RouteDirectionAndStopCallRef, Set<AgencyAndId>> _serviceAlertIdsByRouteDirectionAndStopCall = new ConcurrentHashMap<RouteDirectionAndStopCallRef, Set<AgencyAndId>>();

  private ConcurrentMap<AgencyAndId, Set<AgencyAndId>> _serviceAlertIdsByTripId = new ConcurrentHashMap<AgencyAndId, Set<AgencyAndId>>();

  private ConcurrentMap<TripAndStopCallRef, Set<AgencyAndId>> _serviceAlertIdsByTripAndStopId = new ConcurrentHashMap<TripAndStopCallRef, Set<AgencyAndId>>();

  private FederatedTransitDataBundle _bundle;

  private File _serviceAlertsPath;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  public void setServiceAlertsPath(File path) {
    _serviceAlertsPath = path;
  }

  @PostConstruct
  public void start() {
    loadServieAlerts();
  }

  @PreDestroy
  public void stop() {
    saveServiceAlerts();
  }

  /****
   * {@link ServiceAlertsService} Interface
   ****/

  @Override
  public synchronized ServiceAlert createOrUpdateServiceAlert(
      ServiceAlert.Builder builder, String defaultAgencyId) {

    if (!builder.hasId()) {
      UUID uuid = UUID.randomUUID();
      Id id = ServiceAlertLibrary.id(defaultAgencyId, uuid.toString());
      builder.setId(id);
    }

    if (!builder.hasCreationTime())
      builder.setCreationTime(System.currentTimeMillis());
    builder.setModifiedTime(System.currentTimeMillis());

    ServiceAlert serviceAlert = builder.build();
    updateReferences(serviceAlert);
    saveServiceAlerts();
    return serviceAlert;
  }
  
  @Override
  public synchronized void removeServiceAlert(AgencyAndId serviceAlertId) {
    removeServiceAlerts(Arrays.asList(serviceAlertId));
  }

  @Override
  public synchronized void removeServiceAlerts(List<AgencyAndId> serviceAlertIds) {

    for (AgencyAndId serviceAlertId : serviceAlertIds) {

      ServiceAlert existingServiceAlert = _serviceAlerts.remove(serviceAlertId);

      if (existingServiceAlert != null) {
        updateReferences(existingServiceAlert, null);
      }
    }

    saveServiceAlerts();
  }

  @Override
  public synchronized void removeAllServiceAlertsForFederatedAgencyId(
      String agencyId) {
    Set<AgencyAndId> ids = _serviceAlertIdsByServiceAlertAgencyId.get(agencyId);
    if (ids != null)
      removeServiceAlerts(new ArrayList<AgencyAndId>(ids));
  }

  @Override
  public ServiceAlert getServiceAlertForId(AgencyAndId serviceAlertId) {
    return _serviceAlerts.get(serviceAlertId);
  }

  @Override
  public List<ServiceAlert> getAllServiceAlerts() {
    return new ArrayList<ServiceAlert>(_serviceAlerts.values());
  }

  @Override
  public List<ServiceAlert> getServiceAlertsForFederatedAgencyId(String agencyId) {
    Set<AgencyAndId> serviceAlertIds = _serviceAlertIdsByServiceAlertAgencyId.get(agencyId);
    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }

  @Override
  public List<ServiceAlert> getServiceAlertsForAgencyId(long time,
      String agencyId) {
    Set<AgencyAndId> serviceAlertIds = new HashSet<AgencyAndId>();
    getServiceAlertIdsForKey(_serviceAlertIdsByAgencyId, agencyId,
        serviceAlertIds);
    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }

  @Override
  public List<ServiceAlert> getServiceAlertsForStopId(long time,
      AgencyAndId stopId) {

    Set<AgencyAndId> serviceAlertIds = new HashSet<AgencyAndId>();
    getServiceAlertIdsForKey(_serviceAlertIdsByAgencyId, stopId.getAgencyId(),
        serviceAlertIds);
    getServiceAlertIdsForKey(_serviceAlertIdsByStopId, stopId, serviceAlertIds);
    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }

  @Override
  public List<ServiceAlert> getServiceAlertsForStopIds(long time,
      Iterable<AgencyAndId> stopIds) {
    Set<AgencyAndId> serviceAlertIds = new HashSet<AgencyAndId>();
    for (AgencyAndId stopId : stopIds) {
      getServiceAlertIdsForKey(_serviceAlertIdsByAgencyId,
          stopId.getAgencyId(), serviceAlertIds);
      getServiceAlertIdsForKey(_serviceAlertIdsByStopId, stopId,
          serviceAlertIds);
    }
    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }


  @Override
  public List<ServiceAlert> getServiceAlertsForStopCall(long time,
      BlockInstance blockInstance, BlockStopTimeEntry blockStopTime,
      AgencyAndId vehicleId) {

    BlockTripEntry blockTrip = blockStopTime.getTrip();
    TripEntry trip = blockTrip.getTrip();
    AgencyAndId tripId = trip.getId();
    AgencyAndId lineId = trip.getRouteCollection().getId();
    String directionId = trip.getDirectionId();
    StopTimeEntry stopTime = blockStopTime.getStopTime();
    StopEntry stop = stopTime.getStop();
    AgencyAndId stopId = stop.getId();

    Set<AgencyAndId> serviceAlertIds = new HashSet<AgencyAndId>();
    /*
     * TODO: Temporarily disable
     */
    /*
     * getServiceAlertIdsForKey(_serviceAlertsIdsByAgencyId,
     * lineId.getAgencyId(), serviceAlertIds);
     */
    getServiceAlertIdsForKey(_serviceAlertIdsByRouteId, lineId, serviceAlertIds);
    RouteAndStopCallRef routeAndStopCallRef = new RouteAndStopCallRef(lineId,
        stopId);
    getServiceAlertIdsForKey(_serviceAlertIdsByRouteAndStop,
        routeAndStopCallRef, serviceAlertIds);

    /**
     * Remember that direction is optional
     */
    if (directionId != null) {
      RouteAndDirectionRef lineAndDirectionRef = new RouteAndDirectionRef(
          lineId, directionId);
      RouteDirectionAndStopCallRef lineDirectionAndStopCallRef = new RouteDirectionAndStopCallRef(
          lineId, directionId, stopId);

      getServiceAlertIdsForKey(_serviceAlertIdsByRouteAndDirectionId,
          lineAndDirectionRef, serviceAlertIds);
      getServiceAlertIdsForKey(_serviceAlertIdsByRouteDirectionAndStopCall,
          lineDirectionAndStopCallRef, serviceAlertIds);
    }

    getServiceAlertIdsForKey(_serviceAlertIdsByTripId, trip.getId(),
        serviceAlertIds);
    TripAndStopCallRef tripAndStopCallRef = new TripAndStopCallRef(tripId,
        stopId);
    getServiceAlertIdsForKey(_serviceAlertIdsByTripAndStopId,
        tripAndStopCallRef, serviceAlertIds);

    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }

  @Override
  public List<ServiceAlert> getServiceAlertsForVehicleJourney(long time,
      BlockTripInstance blockTripInstance,
      AgencyAndId vehicleId) {

    BlockTripEntry blockTrip = blockTripInstance.getBlockTrip();
    TripEntry trip = blockTrip.getTrip();
    AgencyAndId lineId = trip.getRouteCollection().getId();
    RouteAndDirectionRef lineAndDirectionRef = new RouteAndDirectionRef(lineId,
        trip.getDirectionId());

    Set<AgencyAndId> serviceAlertIds = new HashSet<AgencyAndId>();
    getServiceAlertIdsForKey(_serviceAlertIdsByAgencyId, lineId.getAgencyId(),
        serviceAlertIds);
    getServiceAlertIdsForKey(_serviceAlertIdsByRouteId, lineId, serviceAlertIds);
    getServiceAlertIdsForKey(_serviceAlertIdsByRouteAndDirectionId,
        lineAndDirectionRef, serviceAlertIds);
    getServiceAlertIdsForKey(_serviceAlertIdsByTripId, trip.getId(),
        serviceAlertIds);
    return getServiceAlertIdsAsObjects(serviceAlertIds);
  }

  /****
   * Private Methods
   ****/

  private void updateReferences(ServiceAlert serviceAlert) {
    AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getId());
    ServiceAlert existingServiceAlert = _serviceAlerts.put(id, serviceAlert);
    updateReferences(existingServiceAlert, serviceAlert);
  }

  private void updateReferences(ServiceAlert existingServiceAlert,
      ServiceAlert serviceAlert) {

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByServiceAlertAgencyId,
        AffectsServiceAlertAgencyKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByAgencyId, AffectsAgencyKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByStopId, AffectsStopKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByRouteId, AffectsRouteKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByRouteAndDirectionId,
        AffectsRouteAndDirectionKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByRouteAndStop, AffectsRouteAndStopKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByRouteDirectionAndStopCall,
        AffectsRouteDirectionAndStopCallKeyFactory.INSTANCE);

    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByTripId, AffectsTripKeyFactory.INSTANCE);
    updateReferences(existingServiceAlert, serviceAlert,
        _serviceAlertIdsByTripAndStopId, AffectsTripAndStopKeyFactory.INSTANCE);
  }

  private <T> void updateReferences(ServiceAlert existingServiceAlert,
      ServiceAlert serviceAlert, ConcurrentMap<T, Set<AgencyAndId>> map,
      AffectsKeyFactory<T> affectsKeyFactory) {

    Set<T> existingEffects = Collections.emptySet();
    if (existingServiceAlert != null) {
      existingEffects = affectsKeyFactory.getKeysForAffects(existingServiceAlert);
    }

    Set<T> newEffects = Collections.emptySet();
    if (serviceAlert != null) {
      newEffects = affectsKeyFactory.getKeysForAffects(serviceAlert);
    }

    for (T existingEffect : existingEffects) {
      if (newEffects.contains(existingEffect))
        continue;
      AgencyAndId id = ServiceAlertLibrary.agencyAndId(existingServiceAlert.getId());
      Set<AgencyAndId> ids = map.get(existingEffect);
      ids.remove(id);
      if (ids.isEmpty())
        map.remove(existingEffect);
    }

    for (T newEffect : newEffects) {
      if (existingEffects.contains(newEffect))
        continue;
      AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getId());
      Set<AgencyAndId> ids = map.get(newEffect);
      if (ids == null) {
        ids = new HashSet<AgencyAndId>();
        map.put(newEffect, ids);
      }
      ids.add(id);
    }
  }

  private <T> void getServiceAlertIdsForKey(
      ConcurrentMap<T, Set<AgencyAndId>> serviceAlertIdsByKey, T key,
      Collection<AgencyAndId> matches) {
    Set<AgencyAndId> ids = serviceAlertIdsByKey.get(key);
    if (ids != null)
      matches.addAll(ids);
  }

  private List<ServiceAlert> getServiceAlertIdsAsObjects(
      Collection<AgencyAndId> serviceAlertIds) {
    if (serviceAlertIds == null || serviceAlertIds.isEmpty())
      return Collections.emptyList();
    List<ServiceAlert> serviceAlerts = new ArrayList<ServiceAlert>(
        serviceAlertIds.size());
    for (AgencyAndId serviceAlertId : serviceAlertIds) {
      ServiceAlert serviceAlert = _serviceAlerts.get(serviceAlertId);
      if (serviceAlert != null)
        serviceAlerts.add(serviceAlert);
    }
    return serviceAlerts;
  }

  /****
   * Serialization
   ****/

  private synchronized void loadServieAlerts() {

    File path = getServiceAlertsPath();

    if (path == null || !path.exists())
      return;

    InputStream in = null;

    try {

      in = new BufferedInputStream(new FileInputStream(path));
      ServiceAlertsCollection collection = ServiceAlertsCollection.parseFrom(in);
      for (ServiceAlert serviceAlert : collection.getServiceAlertsList())
        updateReferences(serviceAlert);

    } catch (Exception ex) {
      _log.error("error loading service alerts from path " + path, ex);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
          _log.error("error closing service alerts path " + path, ex);
        }
      }
    }
  }

  private synchronized void saveServiceAlerts() {

    File path = getServiceAlertsPath();

    if (path == null)
      return;

    ServiceAlertsCollection.Builder builder = ServiceAlertsCollection.newBuilder();
    builder.addAllServiceAlerts(_serviceAlerts.values());
    ServiceAlertsCollection collection = builder.build();

    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(path));
      collection.writeTo(out);
      out.close();
    } catch (Exception ex) {
      _log.error("error saving service alerts to path " + path, ex);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          _log.error("error closing service output to path " + path, ex);
        }
      }
    }
  }

  private File getServiceAlertsPath() {
    if (_serviceAlertsPath != null)
      return _serviceAlertsPath;
    return _bundle.getServiceAlertsPath();
  }

}
