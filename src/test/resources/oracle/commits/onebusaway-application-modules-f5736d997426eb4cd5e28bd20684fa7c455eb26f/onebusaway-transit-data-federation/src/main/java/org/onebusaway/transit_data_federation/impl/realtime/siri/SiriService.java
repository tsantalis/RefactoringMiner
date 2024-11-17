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
package org.onebusaway.transit_data_federation.impl.realtime.siri;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.Duration;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.AffectedApplicationStructure;
import org.onebusaway.siri.OneBusAwayAffects;
import org.onebusaway.siri.OneBusAwayAffectsStructure.Applications;
import org.onebusaway.siri.OneBusAwayConsequence;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data_federation.impl.service_alerts.ServiceAlertLibrary;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Affects;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Consequence;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Consequence.Effect;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Id;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert.Cause;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TimeRange;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString.Translation;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.AbstractServiceDeliveryStructure;
import uk.org.siri.siri.AffectedCallStructure;
import uk.org.siri.siri.AffectedOperatorStructure;
import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure.Calls;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.Operators;
import uk.org.siri.siri.AffectsScopeStructure.StopPoints;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.BlockRefStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import uk.org.siri.siri.VehicleJourneyRefStructure;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;
import uk.org.siri.siri.VehicleRefStructure;
import uk.org.siri.siri.WorkflowStatusEnumeration;

@Component
public class SiriService {

  private static final Logger _log = LoggerFactory.getLogger(SiriService.class);

  private TransitGraphDao _transitGraphDao;

  private ServiceAlertsService _serviceAlertsService;

  private VehicleLocationListener _vehicleLocationListener;

  private BlockCalendarService _blockCalendarService;

  /**
   * Time, in minutes,
   */
  private int _blockInstanceSearchWindow = 30;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setServiceAlertService(ServiceAlertsService serviceAlertsService) {
    _serviceAlertsService = serviceAlertsService;
  }

  @Autowired
  public void set(VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  /**
   * @param blockInstanceSearchWindow time, in minutes
   */
  public void setBlockInstanceSearchWindow(int blockInstanceSearchWindow) {
    _blockInstanceSearchWindow = blockInstanceSearchWindow;
  }

  public synchronized void handleServiceDelivery(
      ServiceDelivery serviceDelivery,
      AbstractServiceDeliveryStructure deliveryForModule,
      ESiriModuleType moduleType, SiriEndpointDetails endpointDetails) {

    switch (moduleType) {
      case VEHICLE_MONITORING:
        handleVehicleMonitoring(serviceDelivery,
            (VehicleMonitoringDeliveryStructure) deliveryForModule,
            endpointDetails);
        break;
      case SITUATION_EXCHANGE:
        handleSituationExchange(serviceDelivery,
            (SituationExchangeDeliveryStructure) deliveryForModule,
            endpointDetails);
        break;
    }

  }

  /****
   * Private Methods
   ****/

  private void handleVehicleMonitoring(ServiceDelivery serviceDelivery,
      VehicleMonitoringDeliveryStructure deliveryForModule,
      SiriEndpointDetails endpointDetails) {

    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();

    Date now = new Date();
    long timeFrom = now.getTime() - _blockInstanceSearchWindow * 60 * 1000;
    long timeTo = now.getTime() + _blockInstanceSearchWindow * 60 * 1000;

    for (VehicleActivityStructure vehicleActivity : deliveryForModule.getVehicleActivity()) {

      Date time = vehicleActivity.getRecordedAtTime();
      if (time == null)
        time = now;

      MonitoredVehicleJourney mvj = vehicleActivity.getMonitoredVehicleJourney();

      Duration delay = mvj.getDelay();
      if (delay == null)
        continue;

      VehicleRefStructure vehicleRef = mvj.getVehicleRef();
      if (vehicleRef == null || vehicleRef.getValue() == null)
        continue;

      BlockEntry block = getBlockForMonitoredVehicleJourney(mvj,
          endpointDetails);
      if (block == null) {
        TripEntry trip = getTripForMonitoredVehicleJourney(mvj, endpointDetails);
        if (trip != null)
          block = trip.getBlock();
      }

      if (block == null)
        continue;

      List<BlockInstance> instances = _blockCalendarService.getActiveBlocks(
          block.getId(), timeFrom, timeTo);

      // TODO : We currently assume that a block won't overlap with itself
      if (instances.size() != 1)
        continue;

      BlockInstance instance = instances.get(0);

      VehicleLocationRecord r = new VehicleLocationRecord();
      r.setTimeOfRecord(time.getTime());
      r.setServiceDate(instance.getServiceDate());
      r.setBlockId(block.getId());

      String agencyId = block.getId().getAgencyId();
      r.setVehicleId(new AgencyAndId(agencyId, vehicleRef.getValue()));

      r.setScheduleDeviation(delay.getTimeInMillis(now) / 1000);

      LocationStructure location = mvj.getVehicleLocation();
      if (location != null) {
        r.setCurrentLocationLat(location.getLatitude().doubleValue());
        r.setCurrentLocationLon(location.getLongitude().doubleValue());
      }

      records.add(r);
    }

    if (!records.isEmpty())
      _vehicleLocationListener.handleVehicleLocationRecords(records);
  }

  private BlockEntry getBlockForMonitoredVehicleJourney(
      MonitoredVehicleJourney mvj, SiriEndpointDetails endpointDetails) {

    BlockRefStructure blockRef = mvj.getBlockRef();
    if (blockRef == null || blockRef.getValue() == null)
      return null;

    for (String agencyId : endpointDetails.getDefaultAgencyIds()) {
      AgencyAndId blockId = new AgencyAndId(agencyId, blockRef.getValue());
      BlockEntry blockEntry = _transitGraphDao.getBlockEntryForId(blockId);
      if (blockEntry != null)
        return blockEntry;
    }

    /**
     * Try parsing the id itself
     */
    try {
      AgencyAndId blockId = AgencyAndId.convertFromString(blockRef.getValue());
      return _transitGraphDao.getBlockEntryForId(blockId);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private TripEntry getTripForMonitoredVehicleJourney(
      MonitoredVehicleJourney mvj, SiriEndpointDetails endpointDetails) {

    FramedVehicleJourneyRefStructure fvjRef = mvj.getFramedVehicleJourneyRef();
    if (fvjRef == null || fvjRef.getDatedVehicleJourneyRef() == null)
      return null;

    for (String agencyId : endpointDetails.getDefaultAgencyIds()) {
      AgencyAndId tripId = new AgencyAndId(agencyId,
          fvjRef.getDatedVehicleJourneyRef());
      TripEntry tripEntry = _transitGraphDao.getTripEntryForId(tripId);
      if (tripEntry != null)
        return tripEntry;
    }

    /**
     * Try parsing the id itself
     */
    try {
      AgencyAndId tripId = AgencyAndId.convertFromString(fvjRef.getDatedVehicleJourneyRef());
      return _transitGraphDao.getTripEntryForId(tripId);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private void handleSituationExchange(ServiceDelivery serviceDelivery,
      SituationExchangeDeliveryStructure sxDelivery,
      SiriEndpointDetails endpointDetails) {

    Situations situations = sxDelivery.getSituations();

    if (situations == null)
      return;

    List<ServiceAlert.Builder> serviceAlertsToUpdate = new ArrayList<ServiceAlert.Builder>();
    List<AgencyAndId> serviceAlertIdsToRemove = new ArrayList<AgencyAndId>();

    for (PtSituationElementStructure ptSituation : situations.getPtSituationElement()) {

      ServiceAlert.Builder serviceAlert = getPtSituationAsServiceAlert(
          ptSituation, endpointDetails);

      WorkflowStatusEnumeration progress = ptSituation.getProgress();
      boolean remove = (progress != null && (progress == WorkflowStatusEnumeration.CLOSING || progress == WorkflowStatusEnumeration.CLOSED));

      if (remove) {
        AgencyAndId situationId = ServiceAlertLibrary.agencyAndId(serviceAlert.getId());
        serviceAlertIdsToRemove.add(situationId);
      } else {
        serviceAlertsToUpdate.add(serviceAlert);
      }
    }

    String defaultAgencyId = null;
    if (!CollectionsLibrary.isEmpty(endpointDetails.getDefaultAgencyIds()))
      defaultAgencyId = endpointDetails.getDefaultAgencyIds().get(0);

    for (ServiceAlert.Builder serviceAlert : serviceAlertsToUpdate)
      _serviceAlertsService.createOrUpdateServiceAlert(serviceAlert,
          defaultAgencyId);
    _serviceAlertsService.removeServiceAlerts(serviceAlertIdsToRemove);
  }

  private ServiceAlert.Builder getPtSituationAsServiceAlert(
      PtSituationElementStructure ptSituation,
      SiriEndpointDetails endpointDetails) {

    ServiceAlert.Builder serviceAlert = ServiceAlert.newBuilder();
    EntryQualifierStructure serviceAlertNumber = ptSituation.getSituationNumber();
    String situationId = serviceAlertNumber.getValue();

    if (!endpointDetails.getDefaultAgencyIds().isEmpty()) {
      String agencyId = endpointDetails.getDefaultAgencyIds().get(0);
      serviceAlert.setId(ServiceAlertLibrary.id(agencyId, situationId));
    } else {
      AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
      serviceAlert.setId(ServiceAlertLibrary.id(id));
    }

    handleDescriptions(ptSituation, serviceAlert);
    handleOtherFields(ptSituation, serviceAlert);
    handlReasons(ptSituation, serviceAlert);
    handleAffects(ptSituation, serviceAlert);
    handleConsequences(ptSituation, serviceAlert);

    return serviceAlert;
  }

  private void handleDescriptions(PtSituationElementStructure ptSituation,
      ServiceAlert.Builder serviceAlert) {

    TranslatedString summary = translation(ptSituation.getSummary());
    if (summary != null)
      serviceAlert.setSummary(summary);

    TranslatedString description = translation(ptSituation.getDescription());
    if (description != null)
      serviceAlert.setDescription(description);
  }

  private void handleOtherFields(PtSituationElementStructure ptSituation,
      ServiceAlert.Builder serviceAlert) {

    SeverityEnumeration severity = ptSituation.getSeverity();
    if (severity != null) {
      ESeverity severityEnum = ESeverity.valueOfTpegCode(severity.value());
      serviceAlert.setSeverity(ServiceAlertLibrary.convertSeverity(severityEnum));
    }

    if (ptSituation.getPublicationWindow() != null) {
      HalfOpenTimestampRangeStructure window = ptSituation.getPublicationWindow();
      TimeRange.Builder range = TimeRange.newBuilder();
      if (window.getStartTime() != null)
        range.setStart(window.getStartTime().getTime());
      if (window.getEndTime() != null)
        range.setEnd(window.getEndTime().getTime());
      if (range.hasStart() || range.hasEnd())
        serviceAlert.addPublicationWindow(range);
    }
  }

  private void handlReasons(PtSituationElementStructure ptSituation,
      ServiceAlert.Builder serviceAlert) {

    Cause cause = getReasonAsCause(ptSituation);
    if (cause != null)
      serviceAlert.setCause(cause);
  }

  private Cause getReasonAsCause(PtSituationElementStructure ptSituation) {
    if (ptSituation.getEnvironmentReason() != null)
      return Cause.WEATHER;
    if (ptSituation.getEquipmentReason() != null) {
      switch (ptSituation.getEquipmentReason()) {
        case CONSTRUCTION_WORK:
          return Cause.CONSTRUCTION;
        case CLOSED_FOR_MAINTENANCE:
        case MAINTENANCE_WORK:
        case EMERGENCY_ENGINEERING_WORK:
        case LATE_FINISH_TO_ENGINEERING_WORK:
        case REPAIR_WORK:
          return Cause.MAINTENANCE;
        default:
          return Cause.TECHNICAL_PROBLEM;
      }
    }
    if (ptSituation.getPersonnelReason() != null) {
      switch (ptSituation.getPersonnelReason()) {
        case INDUSTRIAL_ACTION:
        case UNOFFICIAL_INDUSTRIAL_ACTION:
          return Cause.STRIKE;
      }
      return Cause.OTHER_CAUSE;
    }
    /**
     * There are really so many possibilities here that it's tricky to translate
     * them all
     */
    if (ptSituation.getMiscellaneousReason() != null) {
      switch (ptSituation.getMiscellaneousReason()) {
        case ACCIDENT:
        case COLLISION:
          return Cause.ACCIDENT;
        case DEMONSTRATION:
        case MARCH:
          return Cause.DEMONSTRATION;
        case PERSON_ILL_ON_VEHICLE:
        case FATALITY:
          return Cause.MEDICAL_EMERGENCY;
        case POLICE_REQUEST:
        case BOMB_ALERT:
        case CIVIL_EMERGENCY:
        case EMERGENCY_SERVICES:
        case EMERGENCY_SERVICES_CALL:
          return Cause.POLICE_ACTIVITY;
      }
    }

    return null;
  }

  /****
   * Affects
   ****/

  private void handleAffects(PtSituationElementStructure ptSituation,
      ServiceAlert.Builder serviceAlert) {

    AffectsScopeStructure affectsStructure = ptSituation.getAffects();

    if (affectsStructure == null)
      return;

    Operators operators = affectsStructure.getOperators();

    if (operators != null
        && !CollectionsLibrary.isEmpty(operators.getAffectedOperator())) {

      for (AffectedOperatorStructure operator : operators.getAffectedOperator()) {
        OperatorRefStructure operatorRef = operator.getOperatorRef();
        if (operatorRef == null || operatorRef.getValue() == null)
          continue;
        String agencyId = operatorRef.getValue();
        Affects.Builder affects = Affects.newBuilder();
        affects.setAgencyId(agencyId);
        serviceAlert.addAffects(affects);
      }
    }

    StopPoints stopPoints = affectsStructure.getStopPoints();

    if (stopPoints != null
        && !CollectionsLibrary.isEmpty(stopPoints.getAffectedStopPoint())) {

      for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoint()) {
        StopPointRefStructure stopRef = stopPoint.getStopPointRef();
        if (stopRef == null || stopRef.getValue() == null)
          continue;
        AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(stopRef.getValue());
        Id id = ServiceAlertLibrary.id(stopId);
        Affects.Builder affects = Affects.newBuilder();
        affects.setStopId(id);
        serviceAlert.addAffects(affects);
      }
    }

    VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
    if (vjs != null
        && !CollectionsLibrary.isEmpty(vjs.getAffectedVehicleJourney())) {

      for (AffectedVehicleJourneyStructure vj : vjs.getAffectedVehicleJourney()) {

        Affects.Builder affects = Affects.newBuilder();
        if (vj.getLineRef() != null) {
          AgencyAndId routeId = AgencyAndIdLibrary.convertFromString(vj.getLineRef().getValue());
          Id id = ServiceAlertLibrary.id(routeId);
          affects.setRouteId(id);
        }

        if (vj.getDirectionRef() != null)
          affects.setDirectionId(vj.getDirectionRef().getValue());

        List<VehicleJourneyRefStructure> tripRefs = vj.getVehicleJourneyRef();
        Calls stopRefs = vj.getCalls();

        boolean hasTripRefs = !CollectionsLibrary.isEmpty(tripRefs);
        boolean hasStopRefs = stopRefs != null
            && !CollectionsLibrary.isEmpty(stopRefs.getCall());

        if (!(hasTripRefs || hasStopRefs)) {
          if (affects.hasRouteId())
            serviceAlert.addAffects(affects);
        } else if (hasTripRefs && hasStopRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(vjRef.getValue());
            affects.setTripId(ServiceAlertLibrary.id(tripId));
            for (AffectedCallStructure call : stopRefs.getCall()) {
              AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(call.getStopPointRef().getValue());
              affects.setStopId(ServiceAlertLibrary.id(stopId));
              serviceAlert.addAffects(affects);
            }
          }
        } else if (hasTripRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(vjRef.getValue());
            affects.setTripId(ServiceAlertLibrary.id(tripId));
            serviceAlert.addAffects(affects);
          }
        } else {
          for (AffectedCallStructure call : stopRefs.getCall()) {
            AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(call.getStopPointRef().getValue());
            affects.setStopId(ServiceAlertLibrary.id(stopId));
            serviceAlert.addAffects(affects);
          }
        }
      }
    }

    ExtensionsStructure extension = affectsStructure.getExtensions();
    if (extension != null && extension.getAny() != null) {
      Object ext = extension.getAny();
      if (ext instanceof OneBusAwayAffects) {
        OneBusAwayAffects obaAffects = (OneBusAwayAffects) ext;

        Applications applications = obaAffects.getApplications();
        if (applications != null
            && !CollectionsLibrary.isEmpty(applications.getAffectedApplication())) {

          List<AffectedApplicationStructure> apps = applications.getAffectedApplication();

          for (AffectedApplicationStructure sApp : apps) {
            Affects.Builder affects = Affects.newBuilder();
            affects.setApplicationId(sApp.getApiKey());
            serviceAlert.addAffects(affects);
          }
        }
      }
    }
  }

  private void handleConsequences(PtSituationElementStructure ptSituation,
      ServiceAlert.Builder serviceAlert) {

    PtConsequencesStructure consequences = ptSituation.getConsequences();

    if (consequences == null || consequences.getConsequence() == null)
      return;

    for (PtConsequenceStructure consequence : consequences.getConsequence()) {
      Consequence.Builder builder = Consequence.newBuilder();
      if (consequence.getCondition() != null)
        builder.setEffect(getConditionAsEffect(consequence.getCondition()));
      ExtensionsStructure extensions = consequence.getExtensions();
      if (extensions != null) {
        Object obj = extensions.getAny();
        if (obj instanceof OneBusAwayConsequence) {
          OneBusAwayConsequence obaConsequence = (OneBusAwayConsequence) obj;
          if (obaConsequence.getDiversionPath() != null)
            builder.setDetourPath(obaConsequence.getDiversionPath());
        }
      }
      if (builder.hasDetourPath() || builder.hasEffect())
        serviceAlert.addConsequence(builder);
    }
  }

  private Effect getConditionAsEffect(ServiceConditionEnumeration condition) {
    switch (condition) {

      case CANCELLED:
      case NO_SERVICE:
        return Effect.NO_SERVICE;

      case DELAYED:
        return Effect.SIGNIFICANT_DELAYS;

      case DIVERTED:
        return Effect.DETOUR;

      case ADDITIONAL_SERVICE:
      case EXTENDED_SERVICE:
      case SHUTTLE_SERVICE:
      case SPECIAL_SERVICE:
      case REPLACEMENT_SERVICE:
        return Effect.ADDITIONAL_SERVICE;

      case DISRUPTED:
      case INTERMITTENT_SERVICE:
      case SHORT_FORMED_SERVICE:
        return Effect.REDUCED_SERVICE;

      case ALTERED:
      case ARRIVES_EARLY:
      case REPLACEMENT_TRANSPORT:
      case SPLITTING_TRAIN:
        return Effect.MODIFIED_SERVICE;

      case ON_TIME:
      case FULL_LENGTH_SERVICE:
      case NORMAL_SERVICE:
        return Effect.OTHER_EFFECT;

      case UNDEFINED_SERVICE_INFORMATION:
      case UNKNOWN:
        return Effect.UNKNOWN_EFFECT;

      default:
        _log.warn("unknown condition: " + condition);
        return Effect.UNKNOWN_EFFECT;
    }
  }

  private TranslatedString translation(DefaultedTextStructure text) {
    if (text == null)
      return null;
    String value = text.getValue();
    if (value == null)
      return null;

    Translation.Builder translation = Translation.newBuilder();
    translation.setText(value);
    if (text.getLang() != null)
      translation.setLanguage(text.getLang());

    TranslatedString.Builder tsBuilder = TranslatedString.newBuilder();
    tsBuilder.addTranslation(translation);
    return tsBuilder.build();
  }
}
