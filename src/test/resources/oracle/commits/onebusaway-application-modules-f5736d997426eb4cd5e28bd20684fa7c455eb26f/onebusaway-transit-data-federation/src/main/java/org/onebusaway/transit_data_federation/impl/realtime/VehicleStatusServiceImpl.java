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
package org.onebusaway.transit_data_federation.impl.realtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockVehicleLocationListener;
import org.onebusaway.transit_data_federation.services.realtime.VehicleLocationCacheElement;
import org.onebusaway.transit_data_federation.services.realtime.VehicleLocationCacheElements;
import org.onebusaway.transit_data_federation.services.realtime.VehicleLocationRecordCache;
import org.onebusaway.transit_data_federation.services.realtime.VehicleStatus;
import org.onebusaway.transit_data_federation.services.realtime.VehicleStatusService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class VehicleStatusServiceImpl implements VehicleLocationListener,
    VehicleStatusService {

  private ConcurrentHashMap<AgencyAndId, VehicleLocationRecord> _vehicleRecordsById = new ConcurrentHashMap<AgencyAndId, VehicleLocationRecord>();

  private TransitGraphDao _transitGraphDao;

  private BlockVehicleLocationListener _blockVehicleLocationService;

  private VehicleLocationRecordCache _vehicleLocationRecordCache;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setBlockVehicleLocationService(
      BlockVehicleLocationListener service) {
    _blockVehicleLocationService = service;
  }

  @Autowired
  public void setVehicleLocationRecordCache(
      VehicleLocationRecordCache vehicleLocationRecordCache) {
    _vehicleLocationRecordCache = vehicleLocationRecordCache;
  }

  /****
   * {@link VehicleLocationListener} Interface
   ****/

  @Override
  public void handleVehicleLocationRecord(VehicleLocationRecord record) {

    if (record.getTimeOfRecord() == 0)
      throw new IllegalArgumentException("you must specify a record time");

    if( record.getVehicleId() != null)
      _vehicleRecordsById.put(record.getVehicleId(), record);

    AgencyAndId blockId = record.getBlockId();

    if (blockId == null) {
      AgencyAndId tripId = record.getTripId();
      if (tripId != null) {
        TripEntry tripEntry = _transitGraphDao.getTripEntryForId(tripId);
        if (tripEntry == null)
          throw new IllegalArgumentException("trip not found with id=" + tripId);
        BlockEntry block = tripEntry.getBlock();
        blockId = block.getId();
      }
    }

    // TODO : Maybe not require service date?
    if (blockId != null && record.getServiceDate() != 0)
      _blockVehicleLocationService.handleVehicleLocationRecord(record);
  }

  @Override
  public void handleVehicleLocationRecords(List<VehicleLocationRecord> records) {
    for (VehicleLocationRecord record : records)
      handleVehicleLocationRecord(record);
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vehicleId) {
    _vehicleRecordsById.remove(vehicleId);
    _blockVehicleLocationService.resetVehicleLocation(vehicleId);
  }

  /****
   * {@link VehicleStatusService} Interface
   ****/

  @Override
  public VehicleStatus getVehicleStatusForId(AgencyAndId vehicleId) {

    VehicleLocationRecord record = _vehicleRecordsById.get(vehicleId);
    if (record == null)
      return null;

    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();
    VehicleLocationCacheElements elements = _vehicleLocationRecordCache.getRecordForVehicleId(vehicleId);
    if (elements != null) {
      for (VehicleLocationCacheElement element : elements.getElements())
        records.add(element.getRecord());
    }

    VehicleStatus status = new VehicleStatus();
    status.setRecord(record);
    status.setAllRecords(records);

    return status;
  }

  @Override
  public List<VehicleStatus> getAllVehicleStatuses() {
    ArrayList<VehicleStatus> statuses = new ArrayList<VehicleStatus>();
    for (VehicleLocationRecord record : _vehicleRecordsById.values()) {
      VehicleStatus status = new VehicleStatus();
      status.setRecord(record);
      statuses.add(status);
    }
    return statuses;
  }
}
