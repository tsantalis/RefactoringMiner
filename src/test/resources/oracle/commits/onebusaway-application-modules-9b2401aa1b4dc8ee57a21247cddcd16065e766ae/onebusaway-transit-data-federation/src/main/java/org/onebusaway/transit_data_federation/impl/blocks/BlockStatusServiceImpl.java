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
package org.onebusaway.transit_data_federation.impl.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.model.TargetTime;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockSequenceIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStatusService;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ServiceIntervalBlock;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockStatusServiceImpl implements BlockStatusService {

  /**
   * Catch late trips up to 30 minutes
   */
  private static final long TIME_BEFORE_WINDOW = 90 * 60 * 1000;

  /**
   * Catch early blocks up to 10 minutes
   */
  private static final long TIME_AFTER_WINDOW = 90 * 60 * 1000;

  private BlockCalendarService _blockCalendarService;

  private BlockLocationService _blockLocationService;

  private BlockGeospatialService _blockGeospatialService;

  private ExtendedCalendarService _extendedCalendarService;

  @Autowired
  public void setActive(BlockCalendarService activeCalendarService) {
    _blockCalendarService = activeCalendarService;
  }

  @Autowired
  public void setBlockLocationService(BlockLocationService blockLocationService) {
    _blockLocationService = blockLocationService;
  }

  @Autowired
  public void setBlockGeospatialService(
      BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
  }

  @Autowired
  public void setExtendedCalendarService(
      ExtendedCalendarService extendedCalendarSerivce) {
    _extendedCalendarService = extendedCalendarSerivce;
  }

  /****
   * {@link BlockStatusService} Interface
   ****/

  @Override
  public Map<BlockInstance, List<BlockLocation>> getBlocks(AgencyAndId blockId,
      long serviceDate, AgencyAndId vehicleId, long time) {

    List<BlockInstance> blockInstances = getBlockInstances(blockId,
        serviceDate, time);

    Map<BlockInstance, List<BlockLocation>> results = new HashMap<BlockInstance, List<BlockLocation>>();

    for (BlockInstance blockInstance : blockInstances) {
      List<BlockLocation> locations = new ArrayList<BlockLocation>();
      computeLocations(blockInstance, vehicleId, time, locations);
      results.put(blockInstance, locations);
    }

    return results;
  }

  @Override
  public BlockLocation getBlockForVehicle(AgencyAndId vehicleId, long time) {
    TargetTime target = new TargetTime(time, time);
    return _blockLocationService.getLocationForVehicleAndTime(vehicleId, target);
  }

  @Override
  public List<BlockLocation> getAllActiveBlocks(long time) {
    List<BlockInstance> instances = _blockCalendarService.getActiveBlocksInTimeRange(
        time, time);
    return getAsLocations(instances, time);
  }

  @Override
  public List<BlockLocation> getActiveBlocksForAgency(String agencyId, long time) {

    List<BlockInstance> instances = _blockCalendarService.getActiveBlocksForAgencyInTimeRange(
        agencyId, time, time);

    return getAsLocations(instances, time);
  }

  @Override
  public List<BlockLocation> getBlocksForRoute(AgencyAndId routeId, long time) {

    long timeFrom = time - TIME_BEFORE_WINDOW;
    long timeTo = time + TIME_AFTER_WINDOW;

    List<BlockInstance> instances = _blockCalendarService.getActiveBlocksForRouteInTimeRange(
        routeId, timeFrom, timeTo);

    return getAsLocations(instances, time);
  }

  @Override
  public List<BlockLocation> getBlocksForBounds(CoordinateBounds bounds,
      long time) {

    long timeFrom = time - TIME_BEFORE_WINDOW;
    long timeTo = time + TIME_AFTER_WINDOW;

    List<BlockInstance> instances = _blockGeospatialService.getActiveScheduledBlocksPassingThroughBounds(
        bounds, timeFrom, timeTo);

    List<BlockLocation> locations = getAsLocations(instances, time);
    List<BlockLocation> inRange = new ArrayList<BlockLocation>();
    for (BlockLocation location : locations) {
      CoordinatePoint p = location.getLocation();
      if (bounds.contains(p))
        inRange.add(location);
    }

    return inRange;
  }

  @Override
  public Map<BlockInstance, List<List<BlockLocation>>> getBlocksForIndex(
      BlockSequenceIndex index, List<Date> timestamps) {

    List<BlockInstance> instances = getBlockInstancesForIndexAndTimestamps(
        index, timestamps);

    Map<BlockInstance, List<List<BlockLocation>>> results = new HashMap<BlockInstance, List<List<BlockLocation>>>();

    for (BlockInstance instance : instances) {
      getBlockLocationsForInstanceAndTimestamps(instance, timestamps, results);
    }

    return results;
  }

  /****
   * Private Methods
   ****/

  private List<BlockInstance> getBlockInstances(AgencyAndId blockId,
      long serviceDate, long time) {

    if (serviceDate != 0) {
      BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
          blockId, serviceDate);
      if (blockInstance == null)
        return Collections.emptyList();
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();
      if (blockConfig.getFrequencies() == null)
        return Arrays.asList(blockInstance);

      List<BlockInstance> instances = new ArrayList<BlockInstance>();
      for (FrequencyEntry frequency : blockConfig.getFrequencies())
        instances.add(new BlockInstance(blockConfig,
            blockInstance.getServiceDate(), frequency));
      return instances;
    } else {

      List<BlockInstance> instances = _blockCalendarService.getActiveBlocks(
          blockId, time, time);

      if (instances.isEmpty()) {
        instances = _blockCalendarService.getClosestActiveBlocks(blockId, time);
      }

      return instances;
    }
  }

  private List<BlockLocation> getAsLocations(Iterable<BlockInstance> instances,
      long time) {
    List<BlockLocation> locations = new ArrayList<BlockLocation>();
    for (BlockInstance instance : instances)
      computeLocations(instance, null, time, locations);
    return locations;
  }

  /**
   * 
   * @param instance
   * @param vehicleId optional filter on location results. Can be null.
   * @param time
   * @param results
   */
  private void computeLocations(BlockInstance instance, AgencyAndId vehicleId,
      long time, List<BlockLocation> results) {

    if (instance == null)
      return;

    TargetTime target = new TargetTime(time, time);

    // Try real-time trips first
    List<BlockLocation> locations = _blockLocationService.getLocationsForBlockInstance(
        instance, target);

    if (!locations.isEmpty()) {

      if (vehicleId == null) {
        results.addAll(locations);
      } else {
        for (BlockLocation location : locations)
          if (vehicleId.equals(location.getVehicleId()))
            results.add(location);
      }

    } else {

      // If no real-time trips are available and no vehicle id was specified,
      // use scheduled trips
      if (vehicleId == null) {
        BlockLocation location = _blockLocationService.getScheduledLocationForBlockInstance(
            instance, time);

        if (location != null && location.isInService())
          results.add(location);
      }
    }
  }

  private List<BlockInstance> getBlockInstancesForIndexAndTimestamps(
      BlockSequenceIndex index, List<Date> timestamps) {

    Date tFrom = timestamps.get(0);
    Date tTo = timestamps.get(timestamps.size() - 1);

    ServiceIntervalBlock serviceIntervalBlock = index.getServiceIntervalBlock();
    List<BlockSequence> sequences = index.getSequences();

    Collection<Date> serviceDates = _extendedCalendarService.getServiceDatesWithinRange(
        index.getServiceIds(), serviceIntervalBlock.getRange(), tFrom, tTo);

    List<BlockInstance> instances = new ArrayList<BlockInstance>();

    for (Date serviceDate : serviceDates) {

      int effectiveFromTime = (int) ((tFrom.getTime() - serviceDate.getTime()) / 1000);
      int effectiveToTime = (int) ((tTo.getTime() - serviceDate.getTime()) / 1000);

      int indexFrom = Arrays.binarySearch(
          serviceIntervalBlock.getMaxArrivals(), effectiveFromTime);
      int indexTo = Arrays.binarySearch(
          serviceIntervalBlock.getMinDepartures(), effectiveToTime);

      if (indexFrom < 0)
        indexFrom = -(indexFrom + 1);
      if (indexTo < 0)
        indexTo = -(indexTo + 1);

      InstanceState state = new InstanceState(serviceDate.getTime());

      for (int i = indexFrom; i < indexTo; i++) {

        BlockSequence sequence = sequences.get(i);
        BlockConfigurationEntry blockConfig = sequence.getBlockConfig();
        BlockInstance instance = new BlockInstance(blockConfig, state);
        instances.add(instance);
      }
    }
    return instances;
  }

  private void getBlockLocationsForInstanceAndTimestamps(
      BlockInstance instance, List<Date> timestamps,
      Map<BlockInstance, List<List<BlockLocation>>> results) {

    Map<AgencyAndId, List<BlockLocation>> locations = _blockLocationService.getLocationsForBlockInstance(
        instance, timestamps, System.currentTimeMillis());

    if (locations.isEmpty()) {
      List<List<BlockLocation>> empty = Collections.emptyList();
      results.put(instance, empty);
    } else {
      List<List<BlockLocation>> asList = new ArrayList<List<BlockLocation>>(
          locations.values());
      results.put(instance, asList);
    }
  }
}
