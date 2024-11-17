/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data_federation.impl.federated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.exceptions.NoSuchTripServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.federations.annotations.FederatedByAgencyIdMethod;
import org.onebusaway.federations.annotations.FederatedByEntityIdMethod;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureForStopQueryBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RegisterAlarmQueryBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopCalendarDaysBean;
import org.onebusaway.transit_data.model.StopRouteScheduleBean;
import org.onebusaway.transit_data.model.StopScheduleBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.StopsWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.ScheduledBlockLocationBean;
import org.onebusaway.transit_data.model.oba.LocalSearchResult;
import org.onebusaway.transit_data.model.oba.MinTravelTimeToStopsBean;
import org.onebusaway.transit_data.model.oba.TimedPlaceBean;
import org.onebusaway.transit_data.model.problems.PlannedTripProblemReportBean;
import org.onebusaway.transit_data.model.problems.StopProblemReportBean;
import org.onebusaway.transit_data.model.problems.StopProblemReportQueryBean;
import org.onebusaway.transit_data.model.problems.StopProblemReportSummaryBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportQueryBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportSummaryBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateQueryBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordQueryBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.tripplanning.ConstraintsBean;
import org.onebusaway.transit_data.model.tripplanning.ItinerariesBean;
import org.onebusaway.transit_data.model.tripplanning.TransitLocationBean;
import org.onebusaway.transit_data.model.tripplanning.TransitShedConstraintsBean;
import org.onebusaway.transit_data.model.tripplanning.VertexBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripsForAgencyQueryBean;
import org.onebusaway.transit_data.model.trips.TripsForBoundsQueryBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.ArrivalAndDepartureAlarmService;
import org.onebusaway.transit_data_federation.services.ArrivalAndDepartureQuery;
import org.onebusaway.transit_data_federation.services.beans.AgencyBeanService;
import org.onebusaway.transit_data_federation.services.beans.ArrivalsAndDeparturesBeanService;
import org.onebusaway.transit_data_federation.services.beans.BlockBeanService;
import org.onebusaway.transit_data_federation.services.beans.ItinerariesBeanService;
import org.onebusaway.transit_data_federation.services.beans.RouteBeanService;
import org.onebusaway.transit_data_federation.services.beans.RoutesBeanService;
import org.onebusaway.transit_data_federation.services.beans.ServiceAlertsBeanService;
import org.onebusaway.transit_data_federation.services.beans.ShapeBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopScheduleBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopWithArrivalsAndDeparturesBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopsBeanService;
import org.onebusaway.transit_data_federation.services.beans.TripBeanService;
import org.onebusaway.transit_data_federation.services.beans.TripDetailsBeanService;
import org.onebusaway.transit_data_federation.services.beans.VehicleStatusBeanService;
import org.onebusaway.transit_data_federation.services.realtime.CurrentVehicleEstimationService;
import org.onebusaway.transit_data_federation.services.reporting.UserReportingService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TransitDataServiceImpl implements TransitDataService {

  @Autowired
  private TransitGraphDao _transitGraphDao;

  @Autowired
  private AgencyBeanService _agencyBeanService;

  @Autowired
  private AgencyService _agencyService;

  @Autowired
  private StopBeanService _stopBeanService;

  @Autowired
  private RouteBeanService _routeBeanService;

  @Autowired
  private StopScheduleBeanService _stopScheduleBeanService;

  @Autowired
  private StopWithArrivalsAndDeparturesBeanService _stopWithArrivalsAndDepaturesBeanService;

  @Autowired
  private ArrivalsAndDeparturesBeanService _arrivalsAndDeparturesBeanService;

  @Autowired
  private ArrivalAndDepartureAlarmService _arrivalAndDepartureAlarmService;

  @Autowired
  private StopsBeanService _stopsBeanService;

  @Autowired
  private RoutesBeanService _routesBeanService;

  @Autowired
  private TripBeanService _tripBeanService;

  @Autowired
  private TripDetailsBeanService _tripDetailsBeanService;

  @Autowired
  private BlockBeanService _blockBeanService;

  @Autowired
  private ItinerariesBeanService _itinerariesBeanService;

  @Autowired
  private ShapeBeanService _shapeBeanService;

  @Autowired
  private ServiceAlertsBeanService _serviceAlertsBeanService;

  @Autowired
  private UserReportingService _userReportingService;

  @Autowired
  private CurrentVehicleEstimationService _currentVehicleEstimateService;

  @Autowired
  private VehicleStatusBeanService _vehicleStatusBeanService;

  /****
   * {@link TransitDataService} Interface
   ****/

  @Override
  public Map<String, List<CoordinateBounds>> getAgencyIdsWithCoverageArea() {

    Map<String, CoordinateBounds> agencyIdsAndCoverageAreas = _agencyService.getAgencyIdsAndCoverageAreas();
    Map<String, List<CoordinateBounds>> result = new HashMap<String, List<CoordinateBounds>>();
    for (Map.Entry<String, CoordinateBounds> entry : agencyIdsAndCoverageAreas.entrySet()) {
      String agencyId = entry.getKey();
      CoordinateBounds bounds = entry.getValue();
      List<CoordinateBounds> coverage = Arrays.asList(bounds);
      result.put(agencyId, coverage);
    }
    return result;
  }

  @Override
  public List<AgencyWithCoverageBean> getAgenciesWithCoverage()
      throws ServiceException {

    Map<String, CoordinateBounds> agencyIdsAndCoverageAreas = _agencyService.getAgencyIdsAndCoverageAreas();
    List<AgencyWithCoverageBean> beans = new ArrayList<AgencyWithCoverageBean>();

    for (Map.Entry<String, CoordinateBounds> entry : agencyIdsAndCoverageAreas.entrySet()) {

      String agencyId = entry.getKey();
      CoordinateBounds bounds = entry.getValue();

      AgencyBean agencyBean = _agencyBeanService.getAgencyForId(agencyId);
      if (agencyBean == null)
        throw new ServiceException("agency not found: " + agencyId);

      AgencyWithCoverageBean bean = new AgencyWithCoverageBean();
      bean.setAgency(agencyBean);
      bean.setLat((bounds.getMaxLat() + bounds.getMinLat()) / 2);
      bean.setLon((bounds.getMaxLon() + bounds.getMinLon()) / 2);
      bean.setLatSpan(bounds.getMaxLat() - bounds.getMinLat());
      bean.setLonSpan(bounds.getMaxLon() - bounds.getMinLon());

      beans.add(bean);
    }
    return beans;
  }

  @Override
  public AgencyBean getAgency(String agencyId) throws ServiceException {
    return _agencyBeanService.getAgencyForId(agencyId);
  }

  @Override
  public StopScheduleBean getScheduleForStop(String stopId, Date date)
      throws ServiceException {

    StopScheduleBean bean = new StopScheduleBean();
    bean.setDate(date);

    AgencyAndId id = convertAgencyAndId(stopId);
    StopBean stopBean = _stopBeanService.getStopForId(id);
    if (stopBean == null)
      return null;
    bean.setStop(stopBean);

    ServiceDate serviceDate = new ServiceDate(date);
    List<StopRouteScheduleBean> routes = _stopScheduleBeanService.getScheduledArrivalsForStopAndDate(
        id, serviceDate);
    bean.setRoutes(routes);

    StopCalendarDaysBean calendarDays = _stopScheduleBeanService.getCalendarForStop(id);
    bean.setCalendarDays(calendarDays);

    return bean;
  }

  @Override
  public StopsBean getStops(SearchQueryBean query) throws ServiceException {
    return _stopsBeanService.getStops(query);
  }

  @Override
  public StopBean getStop(String stopId) throws ServiceException {
    AgencyAndId id = convertAgencyAndId(stopId);
    return _stopBeanService.getStopForId(id);
  }

  @Override
  public ListBean<String> getStopIdsForAgencyId(String agencyId) {
    return _stopsBeanService.getStopsIdsForAgencyId(agencyId);
  }

  @Override
  public StopWithArrivalsAndDeparturesBean getStopWithArrivalsAndDepartures(
      String stopId, ArrivalsAndDeparturesQueryBean query)
      throws ServiceException {
    AgencyAndId id = convertAgencyAndId(stopId);
    return _stopWithArrivalsAndDepaturesBeanService.getArrivalsAndDeparturesByStopId(
        id, query);
  }

  @Override
  public StopsWithArrivalsAndDeparturesBean getStopsWithArrivalsAndDepartures(
      Collection<String> stopIds, ArrivalsAndDeparturesQueryBean query)
      throws ServiceException {
    Set<AgencyAndId> ids = convertAgencyAndIds(stopIds);
    return _stopWithArrivalsAndDepaturesBeanService.getArrivalsAndDeparturesForStopIds(
        ids, query);
  }

  @Override
  public ArrivalAndDepartureBean getArrivalAndDepartureForStop(
      ArrivalAndDepartureForStopQueryBean query) throws ServiceException {

    ArrivalAndDepartureQuery adQuery = createArrivalAndDepartureQuery(query);

    return _arrivalsAndDeparturesBeanService.getArrivalAndDepartureForStop(adQuery);
  }

  @Override
  public String registerAlarmForArrivalAndDepartureAtStop(
      ArrivalAndDepartureForStopQueryBean query, RegisterAlarmQueryBean alarm) {

    ArrivalAndDepartureQuery adQuery = createArrivalAndDepartureQuery(query);

    AgencyAndId alarmId = _arrivalAndDepartureAlarmService.registerAlarmForArrivalAndDepartureAtStop(
        adQuery, alarm);

    return AgencyAndIdLibrary.convertToString(alarmId);
  }

  @Override
  public void cancelAlarmForArrivalAndDepartureAtStop(String alarmId) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(alarmId);
    _arrivalAndDepartureAlarmService.cancelAlarmForArrivalAndDepartureAtStop(id);
  }

  @Override
  public RouteBean getRouteForId(String routeId) throws ServiceException {
    return _routeBeanService.getRouteForId(convertAgencyAndId(routeId));
  }

  @Override
  public ListBean<String> getRouteIdsForAgencyId(String agencyId) {
    return _routesBeanService.getRouteIdsForAgencyId(agencyId);
  }

  @Override
  public ListBean<RouteBean> getRoutesForAgencyId(String agencyId) {
    return _routesBeanService.getRoutesForAgencyId(agencyId);
  }

  @Override
  public StopsForRouteBean getStopsForRoute(String routeId) {
    return _routeBeanService.getStopsForRoute(convertAgencyAndId(routeId));
  }

  @Override
  public TripBean getTrip(String tripId) throws ServiceException {
    return _tripBeanService.getTripForId(convertAgencyAndId(tripId));
  }

  @Override
  public TripDetailsBean getSingleTripDetails(TripDetailsQueryBean query)
      throws ServiceException {
    return _tripDetailsBeanService.getTripForId(query);
  }

  @Override
  public ListBean<TripDetailsBean> getTripDetails(TripDetailsQueryBean query)
      throws ServiceException {
    return _tripDetailsBeanService.getTripsForId(query);
  }

  @Override
  public ListBean<TripDetailsBean> getTripsForBounds(
      TripsForBoundsQueryBean query) {
    return _tripDetailsBeanService.getTripsForBounds(query);
  }

  @Override
  public ListBean<TripDetailsBean> getTripsForRoute(TripsForRouteQueryBean query) {
    return _tripDetailsBeanService.getTripsForRoute(query);
  }

  @Override
  public ListBean<TripDetailsBean> getTripsForAgency(
      TripsForAgencyQueryBean query) {
    return _tripDetailsBeanService.getTripsForAgency(query);
  }

  @Override
  public BlockBean getBlockForId(String blockId) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    return _blockBeanService.getBlockForId(id);
  }

  @Override
  public BlockInstanceBean getBlockInstance(String blockId, long serviceDate) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    return _blockBeanService.getBlockInstance(id, serviceDate);
  }

  @Override
  public ScheduledBlockLocationBean getScheduledBlockLocationFromScheduledTime(
      String blockId, long serviceDate, int scheduledTime) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    return _blockBeanService.getScheduledBlockLocationFromScheduledTime(id,
        serviceDate, scheduledTime);
  }

  @Override
  public VehicleStatusBean getVehicleForAgency(String vehicleId, long time) {
    AgencyAndId vid = AgencyAndIdLibrary.convertFromString(vehicleId);
    return _vehicleStatusBeanService.getVehicleForId(vid, time);
  }

  @Override
  public ListBean<VehicleStatusBean> getAllVehiclesForAgency(String agencyId,
      long time) {
    return _vehicleStatusBeanService.getAllVehiclesForAgency(agencyId, time);
  }

  @Override
  public VehicleLocationRecordBean getVehicleLocationRecordForVehicleId(
      String vehicleId, long targetTime) {
    AgencyAndId id = convertAgencyAndId(vehicleId);
    return _vehicleStatusBeanService.getVehicleLocationRecordForVehicleId(id,
        targetTime);
  }

  @Override
  public TripDetailsBean getTripDetailsForVehicleAndTime(
      TripForVehicleQueryBean query) {
    AgencyAndId id = convertAgencyAndId(query.getVehicleId());
    return _tripDetailsBeanService.getTripForVehicle(id,
        query.getTime().getTime(), query.getInclusion());
  }

  @Override
  public RoutesBean getRoutes(SearchQueryBean query) throws ServiceException {
    return _routesBeanService.getRoutesForQuery(query);
  }

  @Override
  public EncodedPolylineBean getShapeForId(String shapeId) {
    AgencyAndId id = convertAgencyAndId(shapeId);
    return _shapeBeanService.getPolylineForShapeId(id);
  }

  @Override
  public ListBean<String> getShapeIdsForAgencyId(String agencyId) {
    return _shapeBeanService.getShapeIdsForAgencyId(agencyId);
  }

  @Override
  public ListBean<CurrentVehicleEstimateBean> getCurrentVehicleEstimates(
      CurrentVehicleEstimateQueryBean query) {
    return _currentVehicleEstimateService.getCurrentVehicleEstimates(query);
  }

  @Override
  public ItinerariesBean getItinerariesBetween(TransitLocationBean from,
      TransitLocationBean to, long targetTime, ConstraintsBean constraints)
      throws ServiceException {
    return _itinerariesBeanService.getItinerariesBetween(from, to, targetTime,
        constraints);
  }

  @Override
  public void reportProblemWithPlannedTrip(TransitLocationBean from,
      TransitLocationBean to, long targetTime, ConstraintsBean constraints,
      PlannedTripProblemReportBean report) {
    _userReportingService.reportProblemWithPlannedTrip(from, to, targetTime,
        constraints, report);
  }

  @Override
  public ListBean<VertexBean> getStreetGraphForRegion(double latFrom,
      double lonFrom, double latTo, double lonTo) throws ServiceException {
    return _itinerariesBeanService.getStreetGraphForRegion(latFrom, lonFrom,
        latTo, lonTo);
  }

  @Override
  public MinTravelTimeToStopsBean getMinTravelTimeToStopsFrom(
      CoordinatePoint location, long time,
      TransitShedConstraintsBean constraints) throws ServiceException {
    return _itinerariesBeanService.getMinTravelTimeToStopsFrom(location, time,
        constraints);
  }

  public List<TimedPlaceBean> getLocalPaths(String agencyId,
      ConstraintsBean constraints,
      MinTravelTimeToStopsBean minTravelTimeToStops,
      List<LocalSearchResult> localResults) throws ServiceException {
    return _itinerariesBeanService.getLocalPaths(constraints,
        minTravelTimeToStops, localResults);
  }

  /****
   * 
   ****/

  public ListBean<VehicleLocationRecordBean> getVehicleLocationRecords(
      VehicleLocationRecordQueryBean query) {
    return _vehicleStatusBeanService.getVehicleLocations(query);
  }

  @Override
  public void submitVehicleLocation(VehicleLocationRecordBean record) {
    _vehicleStatusBeanService.submitVehicleLocation(record);
  }

  @FederatedByEntityIdMethod
  public void resetVehicleLocation(String vehicleId) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(vehicleId);
    _vehicleStatusBeanService.resetVehicleLocation(id);
  }

  /****
   * Service Alert Methods
   ****/

  @Override
  public ServiceAlertBean createServiceAlert(String agencyId,
      ServiceAlertBean situation) {
    return _serviceAlertsBeanService.createServiceAlert(agencyId, situation);
  }

  @Override
  public void updateServiceAlert(ServiceAlertBean situation) {
    _serviceAlertsBeanService.updateServiceAlert(situation);
  }

  @Override
  public ServiceAlertBean getServiceAlertForId(String situationId) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
    return _serviceAlertsBeanService.getServiceAlertForId(id);
  }

  @Override
  public void removeServiceAlert(String situationId) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
    _serviceAlertsBeanService.removeServiceAlert(id);
  }

  @Override
  public ListBean<ServiceAlertBean> getAllServiceAlertsForAgencyId(
      String agencyId) {
    List<ServiceAlertBean> situations = _serviceAlertsBeanService.getServiceAlertsForFederatedAgencyId(agencyId);
    return new ListBean<ServiceAlertBean>(situations, false);
  }

  @Override
  public void removeAllServiceAlertsForAgencyId(String agencyId) {
    _serviceAlertsBeanService.removeAllServiceAlertsForFederatedAgencyId(agencyId);
  }

  @Override
  public ListBean<ServiceAlertBean> getServiceAlerts(SituationQueryBean query) {
    if (!CollectionsLibrary.isEmpty(query.getStopIds())) {
      List<AgencyAndId> stopIds = new ArrayList<AgencyAndId>();
      for (String stopId : query.getStopIds()) {
        stopIds.add(AgencyAndIdLibrary.convertFromString(stopId));
      }
      List<ServiceAlertBean> situations = _serviceAlertsBeanService.getServiceAlertsForStopIds(
          query.getTime(), stopIds);
      return new ListBean<ServiceAlertBean>(situations, false);
    } else {
      List<ServiceAlertBean> situations = _serviceAlertsBeanService.getServiceAlerts(query);
      return new ListBean<ServiceAlertBean>(situations, false);
    }
  }

  @Override
  public void reportProblemWithStop(StopProblemReportBean problem) {
    _userReportingService.reportProblemWithStop(problem);
  }

  @Override
  public void reportProblemWithTrip(TripProblemReportBean problem) {
    _userReportingService.reportProblemWithTrip(problem);
  }

  @Override
  public ListBean<StopProblemReportSummaryBean> getStopProblemReportSummaries(
      StopProblemReportQueryBean query) {
    return _userReportingService.getStopProblemReportSummaries(query);
  }

  @Override
  public ListBean<TripProblemReportSummaryBean> getTripProblemReportSummaries(
      TripProblemReportQueryBean query) {
    return _userReportingService.getTripProblemReportSummaries(query);
  }

  @Override
  @FederatedByAgencyIdMethod()
  public ListBean<StopProblemReportBean> getStopProblemReports(
      StopProblemReportQueryBean query) {
    return _userReportingService.getStopProblemReports(query);
  }

  @Override
  public ListBean<TripProblemReportBean> getTripProblemReports(
      TripProblemReportQueryBean query) {
    return _userReportingService.getTripProblemReports(query);
  }

  @Override
  public List<StopProblemReportBean> getAllStopProblemReportsForStopId(
      String stopId) {
    return _userReportingService.getAllStopProblemReportsForStopId(convertAgencyAndId(stopId));
  }

  @Override
  public List<TripProblemReportBean> getAllTripProblemReportsForTripId(
      String tripId) {
    return _userReportingService.getAllTripProblemReportsForTripId(convertAgencyAndId(tripId));
  }

  @Override
  public StopProblemReportBean getStopProblemReportForStopIdAndId(
      String stopId, long id) {
    return _userReportingService.getStopProblemReportForId(id);
  }

  @Override
  public TripProblemReportBean getTripProblemReportForTripIdAndId(
      String tripId, long id) {
    return _userReportingService.getTripProblemReportForId(id);
  }

  @Override
  public void deleteStopProblemReportForStopIdAndId(String stopId, long id) {
    _userReportingService.deleteStopProblemReportForId(id);
  }

  @Override
  public void updateTripProblemReport(TripProblemReportBean tripProblemReport) {
    _userReportingService.updateTripProblemReport(tripProblemReport);
  }

  @Override
  public void deleteTripProblemReportForTripIdAndId(String tripId, long id) {
    _userReportingService.deleteTripProblemReportForId(id);
  }

  @Override
  public List<String> getAllTripProblemReportLabels() {
    return _userReportingService.getAllTripProblemReportLabels();
  }

  /****
   * Private Methods
   ****/

  private AgencyAndId convertAgencyAndId(String id) {
    return AgencyAndIdLibrary.convertFromString(id);
  }

  private Set<AgencyAndId> convertAgencyAndIds(Iterable<String> ids) {
    Set<AgencyAndId> converted = new HashSet<AgencyAndId>();
    for (String id : ids)
      converted.add(convertAgencyAndId(id));
    return converted;
  }

  private ArrivalAndDepartureQuery createArrivalAndDepartureQuery(
      ArrivalAndDepartureForStopQueryBean query) {

    ArrivalAndDepartureQuery adQuery = new ArrivalAndDepartureQuery();

    AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(query.getStopId());
    StopEntry stop = _transitGraphDao.getStopEntryForId(stopId, true);

    AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(query.getTripId());
    TripEntry trip = _transitGraphDao.getTripEntryForId(tripId);
    if (trip == null)
      throw new NoSuchTripServiceException(query.getTripId());

    adQuery.setStop(stop);
    adQuery.setStopSequence(query.getStopSequence());
    adQuery.setTrip(trip);
    adQuery.setServiceDate(query.getServiceDate());
    adQuery.setVehicleId(AgencyAndIdLibrary.convertFromString(query.getVehicleId()));
    adQuery.setTime(query.getTime());

    return adQuery;
  }

}
