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
package org.onebusaway.transit_data_federation.impl.beans;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordQueryBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data_federation.impl.realtime.BlockLocationRecord;
import org.onebusaway.transit_data_federation.impl.realtime.BlockLocationRecordDao;
import org.onebusaway.transit_data_federation.model.TargetTime;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.beans.TripDetailsBeanService;
import org.onebusaway.transit_data_federation.services.beans.VehicleStatusBeanService;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocationService;
import org.onebusaway.transit_data_federation.services.realtime.VehicleStatus;
import org.onebusaway.transit_data_federation.services.realtime.VehicleStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class VehicleStatusBeanServiceImpl implements VehicleStatusBeanService {

  private VehicleStatusService _vehicleStatusService;

  private TripDetailsBeanService _tripDetailsBeanService;

  private BlockLocationRecordDao _blockLocationRecordDao;

  private VehicleLocationListener _vehicleLocationListener;

  private BlockLocationService _blockLocationService;

  @Autowired
  public void setVehicleStatusService(VehicleStatusService vehicleStatusService) {
    _vehicleStatusService = vehicleStatusService;
  }

  @Autowired
  public void setTripDetailsBeanService(
      TripDetailsBeanService tripDetailsBeanService) {
    _tripDetailsBeanService = tripDetailsBeanService;
  }

  @Autowired
  public void setBlockLocationRecordDao(
      BlockLocationRecordDao blockLocationRecordDao) {
    _blockLocationRecordDao = blockLocationRecordDao;
  }

  @Autowired
  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  public VehicleStatusBean getVehicleForId(AgencyAndId vehicleId, long time) {
    VehicleStatus status = _vehicleStatusService.getVehicleStatusForId(vehicleId);
    if (status == null)
      return null;
    return getStatusAsBean(status, time);
  }

  @Override
  public ListBean<VehicleStatusBean> getAllVehiclesForAgency(String agencyId,
      long time) {

    List<VehicleStatus> statuses = _vehicleStatusService.getAllVehicleStatuses();

    List<VehicleStatusBean> beans = new ArrayList<VehicleStatusBean>();

    for (VehicleStatus status : statuses) {
      AgencyAndId vid = status.getVehicleId();
      if (!vid.getAgencyId().equals(agencyId))
        continue;

      VehicleStatusBean bean = getStatusAsBean(status, time);
      beans.add(bean);
    }

    return new ListBean<VehicleStatusBean>(beans, false);
  }

  @Override
  public VehicleLocationRecordBean getVehicleLocationRecordForVehicleId(
      AgencyAndId vehicleId, long targetTime) {

    TargetTime target = new TargetTime(targetTime, targetTime);

    BlockLocation blockLocation = _blockLocationService.getLocationForVehicleAndTime(
        vehicleId, target);
    if (blockLocation == null)
      return null;
    return getBlockLocationAsVehicleLocationRecord(blockLocation);
  }

  @Override
  public ListBean<VehicleLocationRecordBean> getVehicleLocations(
      VehicleLocationRecordQueryBean query) {

    AgencyAndId blockId = AgencyAndIdLibrary.convertFromString(query.getBlockId());
    AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(query.getTripId());
    AgencyAndId vehicleId = AgencyAndIdLibrary.convertFromString(query.getVehicleId());

    List<BlockLocationRecord> records = _blockLocationRecordDao.getBlockLocationRecords(
        blockId, tripId, vehicleId, query.getServiceDate(),
        query.getFromTime(), query.getToTime(), 1000);

    List<VehicleLocationRecordBean> beans = new ArrayList<VehicleLocationRecordBean>(
        records.size());

    for (BlockLocationRecord record : records) {
      VehicleLocationRecordBean bean = getBlockLocationRecordAsVehicleLocationRecord(record);
      beans.add(bean);
    }

    return new ListBean<VehicleLocationRecordBean>(beans, false);
  }

  @Override
  public void submitVehicleLocation(VehicleLocationRecordBean bean) {

    VehicleLocationRecord r = new VehicleLocationRecord();
    r.setTimeOfRecord(bean.getTimeOfRecord());
    r.setTimeOfLocationUpdate(bean.getTimeOfLocationUpdate());
    r.setServiceDate(bean.getServiceDate());

    r.setBlockId(AgencyAndIdLibrary.convertFromString(bean.getBlockId()));
    r.setTripId(AgencyAndIdLibrary.convertFromString(bean.getTripId()));
    r.setVehicleId(AgencyAndIdLibrary.convertFromString(bean.getVehicleId()));

    if (bean.getPhase() != null)
      r.setPhase(EVehiclePhase.valueOf(bean.getPhase().toUpperCase()));
    r.setStatus(bean.getStatus());

    CoordinatePoint p = bean.getCurrentLocation();
    if (p != null) {
      r.setCurrentLocationLat(p.getLat());
      r.setCurrentLocationLon(p.getLon());
    }

    if (bean.isCurrentOrientationSet())
      r.setCurrentOrientation(bean.getCurrentOrientation());
    if (bean.isDistanceAlongBlockSet())
      r.setDistanceAlongBlock(bean.getDistanceAlongBlock());
    if (bean.isScheduleDeviationSet())
      r.setScheduleDeviation(bean.getScheduleDeviation());

    _vehicleLocationListener.handleVehicleLocationRecord(r);
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vehicleId) {
    _vehicleLocationListener.resetVehicleLocation(vehicleId);
  }

  /****
   * 
   ****/

  private VehicleStatusBean getStatusAsBean(VehicleStatus status, long time) {

    VehicleLocationRecord record = status.getRecord();

    VehicleStatusBean bean = new VehicleStatusBean();
    bean.setLastUpdateTime(record.getTimeOfRecord());
    bean.setLastLocationUpdateTime(record.getTimeOfLocationUpdate());

    EVehiclePhase phase = record.getPhase();
    if (phase != null)
      bean.setPhase(phase.toLabel());

    bean.setStatus(record.getStatus());

    if (record.isCurrentLocationSet())
      bean.setLocation(new CoordinatePoint(record.getCurrentLocationLat(),
          record.getCurrentLocationLon()));

    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));

    TripDetailsBean details = _tripDetailsBeanService.getTripForVehicle(
        record.getVehicleId(), time, new TripDetailsInclusionBean(true, false,
            true));
    if (details != null && details.getStatus() != null) {
      bean.setTrip(details.getTrip());
      bean.setTripStatus(details.getStatus());
    }

    List<VehicleLocationRecord> allRecords = status.getAllRecords();
    if (!CollectionsLibrary.isEmpty(allRecords)) {
      List<VehicleLocationRecordBean> allRecordBeans = new ArrayList<VehicleLocationRecordBean>();
      bean.setAllRecords(allRecordBeans);
      for (VehicleLocationRecord r : allRecords) {
        VehicleLocationRecordBean rBean = getVehicleLocationRecordAsBean(r);
        allRecordBeans.add(rBean);
      }
    }

    return bean;
  }

  private VehicleLocationRecordBean getVehicleLocationRecordAsBean(
      VehicleLocationRecord record) {

    VehicleLocationRecordBean bean = new VehicleLocationRecordBean();
    bean.setBlockId(AgencyAndIdLibrary.convertToString(record.getBlockId()));

    if (record.getPhase() != null)
      bean.setPhase(record.getPhase().toLabel());

    if (record.isCurrentLocationSet()) {
      CoordinatePoint location = new CoordinatePoint(
          record.getCurrentLocationLat(), record.getCurrentLocationLon());
      bean.setCurrentLocation(location);
    }
    if (record.isCurrentOrientationSet())
      bean.setCurrentOrientation(record.getCurrentOrientation());

    if (record.isDistanceAlongBlockSet())
      bean.setDistanceAlongBlock(record.getDistanceAlongBlock());

    if (record.isScheduleDeviationSet())
      bean.setScheduleDeviation(record.getScheduleDeviation());

    bean.setStatus(record.getStatus());
    bean.setServiceDate(record.getServiceDate());
    bean.setTimeOfRecord(record.getTimeOfRecord());
    bean.setTripId(AgencyAndIdLibrary.convertToString(record.getTripId()));
    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
    return bean;
  }

  private VehicleLocationRecordBean getBlockLocationRecordAsVehicleLocationRecord(
      BlockLocationRecord record) {

    VehicleLocationRecordBean bean = new VehicleLocationRecordBean();
    bean.setBlockId(AgencyAndIdLibrary.convertToString(record.getBlockId()));
    if (record.getPhase() != null)
      bean.setPhase(record.getPhase().toLabel());
    CoordinatePoint location = record.getLocation();
    if (location != null)
      bean.setCurrentLocation(location);
    if (record.getOrientation() != null)
      bean.setCurrentOrientation(record.getOrientation());
    if (record.getDistanceAlongBlock() != null)
      bean.setDistanceAlongBlock(record.getDistanceAlongBlock());
    if (record.getScheduleDeviation() != null)
      bean.setScheduleDeviation(record.getScheduleDeviation());
    bean.setStatus(record.getStatus());
    bean.setServiceDate(record.getServiceDate());
    bean.setTimeOfRecord(record.getTime());
    bean.setTripId(AgencyAndIdLibrary.convertToString(record.getTripId()));
    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
    return bean;
  }

  private VehicleLocationRecordBean getBlockLocationAsVehicleLocationRecord(
      BlockLocation record) {
    VehicleLocationRecordBean bean = new VehicleLocationRecordBean();
    bean.setBlockId(AgencyAndIdLibrary.convertToString(record.getBlockInstance().getBlock().getBlock().getId()));
    if (record.getPhase() != null)
      bean.setPhase(record.getPhase().toLabel());
    CoordinatePoint location = record.getLocation();
    if (location != null)
      bean.setCurrentLocation(location);

    if (record.isOrientationSet())
      bean.setCurrentOrientation(record.getOrientation());
    if (record.isDistanceAlongBlockSet())
      bean.setDistanceAlongBlock(record.getDistanceAlongBlock());
    if (record.isScheduleDeviationSet())
      bean.setScheduleDeviation(record.getScheduleDeviation());
    bean.setStatus(record.getStatus());
    bean.setServiceDate(record.getBlockInstance().getServiceDate());
    bean.setTimeOfRecord(record.getLastUpdateTime());
    bean.setTimeOfLocationUpdate(record.getLastLocationUpdateTime());
    bean.setTripId(AgencyAndIdLibrary.convertToString(record.getActiveTrip().getTrip().getId()));
    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
    return bean;
  }
}
