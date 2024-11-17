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
package org.onebusaway.transit_data_federation.impl.transit_graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.collections.adapter.IAdapter;
import org.onebusaway.collections.adapter.ListAdapter;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.serialization.EntryCallback;
import org.onebusaway.transit_data_federation.services.serialization.EntryIdAndCallback;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripPlannerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.strtree.STRtree;

public class TransitGraphImpl implements Serializable, TripPlannerGraph {

  private static final long serialVersionUID = 2L;

  private static Logger _log = LoggerFactory.getLogger(TransitGraphImpl.class);

  private static final AgencyEntryAdapter _agencyEntryAdapter = new AgencyEntryAdapter();

  private static final TripEntryAdapter _tripEntryAdapter = new TripEntryAdapter();

  private static final BlockEntryAdapter _blockEntryAdapter = new BlockEntryAdapter();

  private static final StopEntryAdapter _stopEntryAdapter = new StopEntryAdapter();

  private static final RouteCollectionEntryAdapter _routeCollectionEntryAdapter = new RouteCollectionEntryAdapter();

  private static final RouteEntryAdapter _routeEntryAdapter = new RouteEntryAdapter();

  private transient static ReadHelper _helper;

  private List<AgencyEntryImpl> _agencies = new ArrayList<AgencyEntryImpl>();

  private List<StopEntryImpl> _stops = new ArrayList<StopEntryImpl>();

  private List<TripEntryImpl> _trips = new ArrayList<TripEntryImpl>();

  private List<BlockEntryImpl> _blocks = new ArrayList<BlockEntryImpl>();

  private List<RouteCollectionEntryImpl> _routeCollections = new ArrayList<RouteCollectionEntryImpl>();

  private List<RouteEntryImpl> _routes = new ArrayList<RouteEntryImpl>();

  private transient STRtree _stopLocationTree = null;

  private transient Map<String, AgencyEntryImpl> _agencyEntriesById = new HashMap<String, AgencyEntryImpl>();

  private transient Map<AgencyAndId, StopEntryImpl> _stopEntriesById = new HashMap<AgencyAndId, StopEntryImpl>();

  private transient Map<AgencyAndId, TripEntryImpl> _tripEntriesById = new HashMap<AgencyAndId, TripEntryImpl>();

  private transient Map<AgencyAndId, BlockEntryImpl> _blockEntriesById = new HashMap<AgencyAndId, BlockEntryImpl>();

  private transient Map<AgencyAndId, RouteCollectionEntryImpl> _routeCollectionEntriesById = new HashMap<AgencyAndId, RouteCollectionEntryImpl>();

  private transient Map<AgencyAndId, RouteEntryImpl> _routeEntriesById = new HashMap<AgencyAndId, RouteEntryImpl>();

  public TransitGraphImpl() {

  }

  public void initialize() {
    if (_stopLocationTree == null) {
      System.out.println("initializing trip planner graph...");

      if (_stops.size() == 0) {

        _log.warn("no stops found for graph");

      } else {

        _stopLocationTree = new STRtree(_stops.size());

        for (int i = 0; i < _stops.size(); i++) {
          StopEntry stop = _stops.get(i);
          double x = stop.getStopLon();
          double y = stop.getStopLat();
          Envelope r = new Envelope(x, x, y, y);
          _stopLocationTree.insert(r, stop);
        }

        _stopLocationTree.build();
      }

      System.out.println("  stops=" + _stops.size());
      System.out.println("  trips= " + _trips.size());
    }

    if (_agencyEntriesById == null
        || _agencyEntriesById.size() < _agencies.size()) {
      refreshAgencyMapping();
    }

    if (_tripEntriesById == null || _tripEntriesById.size() < _trips.size()) {
      refreshTripMapping();
    }

    if (_blockEntriesById == null || _blockEntriesById.size() < _blocks.size()) {
      refreshBlockMapping();
    }

    if (_stopEntriesById == null || _stopEntriesById.size() < _stops.size())
      refreshStopMapping();

    if (_routeCollectionEntriesById == null
        || _routeCollectionEntriesById.size() < _routeCollections.size())
      refreshRouteCollectionMapping();

    if (_routeEntriesById == null || _routeEntriesById.size() < _routes.size())
      refreshRouteMapping();

    int i = 0;
    for (StopEntryImpl stop : _stops)
      stop.setIndex(i++);
  }

  public void initializeFromExistinGraph(TransitGraphImpl graph) {
    _agencies.addAll(graph._agencies);
    _stops.addAll(graph._stops);
    _routes.addAll(graph._routes);
    _routeCollections.addAll(graph._routeCollections);
    _trips.addAll(graph._trips);
    _blocks.addAll(graph._blocks);
    initialize();
  }

  public void putAgencyEntry(AgencyEntryImpl agencyEntry) {
    _agencies.add(agencyEntry);
  }

  public void putStopEntry(StopEntryImpl stopEntry) {
    _stops.add(stopEntry);
  }

  public List<StopEntryImpl> getStops() {
    return _stops;
  }

  public void putTripEntry(TripEntryImpl tripEntry) {
    _trips.add(tripEntry);
  }

  public List<TripEntryImpl> getTrips() {
    return _trips;
  }

  public void putBlockEntry(BlockEntryImpl blockEntry) {
    _blocks.add(blockEntry);
  }
  
  public List<BlockEntryImpl> getBlocks() {
    return _blocks;
  }

  public void putRouteEntry(RouteEntryImpl routeEntry) {
    _routes.add(routeEntry);
  }

  public List<RouteEntryImpl> getRoutes() {
    return _routes;
  }

  public void putRouteCollectionEntry(RouteCollectionEntryImpl routeCollection) {
    _routeCollections.add(routeCollection);
  }

  public void refreshAgencyMapping() {
    _agencyEntriesById = new HashMap<String, AgencyEntryImpl>();
    for (AgencyEntryImpl entry : _agencies)
      _agencyEntriesById.put(entry.getId(), entry);
  }

  public void refreshTripMapping() {
    _tripEntriesById = new HashMap<AgencyAndId, TripEntryImpl>();
    for (TripEntryImpl entry : _trips)
      _tripEntriesById.put(entry.getId(), entry);
  }

  public void refreshBlockMapping() {
    _blockEntriesById = new HashMap<AgencyAndId, BlockEntryImpl>();
    for (BlockEntryImpl entry : _blocks)
      _blockEntriesById.put(entry.getId(), entry);
  }

  public void refreshStopMapping() {
    _stopEntriesById = new HashMap<AgencyAndId, StopEntryImpl>();
    for (StopEntryImpl entry : _stops)
      _stopEntriesById.put(entry.getId(), entry);
  }

  public void refreshRouteMapping() {
    _routeEntriesById = new HashMap<AgencyAndId, RouteEntryImpl>();
    for (RouteEntryImpl entry : _routes)
      _routeEntriesById.put(entry.getId(), entry);
  }

  public void refreshRouteCollectionMapping() {
    _routeCollectionEntriesById = new HashMap<AgencyAndId, RouteCollectionEntryImpl>();
    for (RouteCollectionEntryImpl entry : _routeCollections)
      _routeCollectionEntriesById.put(entry.getId(), entry);
  }

  /****
   * {@link TripPlannerGraph} Interface
   ****/

  public List<AgencyEntry> getAllAgencies() {
    return new ListAdapter<AgencyEntryImpl, AgencyEntry>(_agencies,
        _agencyEntryAdapter);
  }

  public AgencyEntryImpl getAgencyForId(String id) {
    return _agencyEntriesById.get(id);
  }

  @Override
  public List<StopEntry> getAllStops() {
    return new ListAdapter<StopEntryImpl, StopEntry>(_stops, _stopEntryAdapter);
  }

  @Override
  public List<TripEntry> getAllTrips() {
    return new ListAdapter<TripEntryImpl, TripEntry>(_trips, _tripEntryAdapter);
  }

  @Override
  public List<BlockEntry> getAllBlocks() {
    return new ListAdapter<BlockEntryImpl, BlockEntry>(_blocks,
        _blockEntryAdapter);
  }

  @Override
  public List<RouteCollectionEntry> getAllRouteCollections() {
    return new ListAdapter<RouteCollectionEntryImpl, RouteCollectionEntry>(
        _routeCollections, _routeCollectionEntryAdapter);
  }

  @Override
  public List<RouteEntry> getAllRoutes() {
    return new ListAdapter<RouteEntryImpl, RouteEntry>(_routes,
        _routeEntryAdapter);
  }

  @Override
  public StopEntryImpl getStopEntryForId(AgencyAndId id) {
    return _stopEntriesById.get(id);
  }

  @Override
  public TripEntryImpl getTripEntryForId(AgencyAndId id) {
    return _tripEntriesById.get(id);
  }

  @Override
  public BlockEntry getBlockEntryForId(AgencyAndId blockId) {
    return _blockEntriesById.get(blockId);
  }

  @Override
  public RouteCollectionEntry getRouteCollectionForId(AgencyAndId id) {
    return _routeCollectionEntriesById.get(id);
  }

  @Override
  public RouteEntryImpl getRouteForId(AgencyAndId id) {
    return _routeEntriesById.get(id);
  }

  @Override
  public List<StopEntry> getStopsByLocation(CoordinateBounds bounds) {
    if (_stopLocationTree == null)
      return Collections.emptyList();
    Envelope r = new Envelope(bounds.getMinLon(), bounds.getMaxLon(),
        bounds.getMinLat(), bounds.getMaxLat());
    StopRTreeVisitor go = new StopRTreeVisitor();
    _stopLocationTree.query(r, go);
    return go.getStops();
  }

  private class StopRTreeVisitor implements ItemVisitor {

    private List<StopEntry> _nearbyStops = new ArrayList<StopEntry>();

    public List<StopEntry> getStops() {
      return _nearbyStops;
    }

    @Override
    public void visitItem(Object obj) {
      _nearbyStops.add((StopEntry) obj);
    }
  }

  /*****************************************************************************
   * Serialization Support
   ****************************************************************************/

  public static void handleStopEntryRead(StopEntryImpl stopEntryImpl) {
    _helper.handleStopEntryRead(stopEntryImpl);
  }

  public static void handleTripEntryRead(TripEntryImpl tripEntryImpl) {
    _helper.handleTripEntryRead(tripEntryImpl);
  }

  public static void addStopEntryCallback(AgencyAndId stopEntry,
      EntryCallback<StopEntryImpl> entry) {
    _helper.addStopEntryCallback(stopEntry, entry);
  }

  public static void addTripEntryCallback(AgencyAndId tripEntry,
      EntryCallback<TripEntryImpl> entry) {
    _helper.addTripEntryCallback(tripEntry, entry);
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    _helper = new ReadHelper();
    in.defaultReadObject();
    _helper.flush();
    _helper = null;

    initialize();

    // Do a GC run, since the graph-reading process requires a lot of data to be
    // loaded
    System.gc();
    System.gc();
  }

  private static class ReadHelper {

    private Map<AgencyAndId, StopEntryImpl> _stops = new HashMap<AgencyAndId, StopEntryImpl>();

    private Map<AgencyAndId, TripEntryImpl> _trips = new HashMap<AgencyAndId, TripEntryImpl>();

    private List<EntryIdAndCallback<AgencyAndId, StopEntryImpl>> _stopCallbacks = new ArrayList<EntryIdAndCallback<AgencyAndId, StopEntryImpl>>();
    private List<EntryIdAndCallback<AgencyAndId, TripEntryImpl>> _tripCallbacks = new ArrayList<EntryIdAndCallback<AgencyAndId, TripEntryImpl>>();

    public void handleStopEntryRead(StopEntryImpl stopEntryImpl) {
      _stops.put(stopEntryImpl.getId(), stopEntryImpl);
    }

    public void handleTripEntryRead(TripEntryImpl tripEntryImpl) {
      _trips.put(tripEntryImpl.getId(), tripEntryImpl);
    }

    public void addStopEntryCallback(AgencyAndId stopEntryId,
        EntryCallback<StopEntryImpl> callback) {
      _stopCallbacks.add(new EntryIdAndCallback<AgencyAndId, StopEntryImpl>(
          stopEntryId, callback));
    }

    public void addTripEntryCallback(AgencyAndId tripEntryId,
        EntryCallback<TripEntryImpl> callback) {
      _tripCallbacks.add(new EntryIdAndCallback<AgencyAndId, TripEntryImpl>(
          tripEntryId, callback));
    }

    public void flush() {

      for (EntryIdAndCallback<AgencyAndId, StopEntryImpl> ci : _stopCallbacks) {
        StopEntryImpl entry = _stops.get(ci.getId());
        if (entry == null)
          throw new IllegalStateException("no such stop entry: " + ci.getId());
        ci.getCallback().handle(entry);
      }

      for (EntryIdAndCallback<AgencyAndId, TripEntryImpl> ci : _tripCallbacks) {
        TripEntryImpl entry = _trips.get(ci.getId());
        if (entry == null)
          throw new IllegalStateException("no such trip entry: " + ci.getId());
        ci.getCallback().handle(entry);
      }

      _stopCallbacks.clear();
      _tripCallbacks.clear();

      _stopCallbacks = null;
      _tripCallbacks = null;

      _stops.clear();
      _trips.clear();

      _stops = null;
      _trips = null;
    }
  }

  private static class AgencyEntryAdapter implements
      IAdapter<AgencyEntryImpl, AgencyEntry> {

    @Override
    public AgencyEntry adapt(AgencyEntryImpl source) {
      return source;
    }
  }

  private static class TripEntryAdapter implements
      IAdapter<TripEntryImpl, TripEntry> {

    @Override
    public TripEntry adapt(TripEntryImpl source) {
      return source;
    }
  }

  private static class BlockEntryAdapter implements
      IAdapter<BlockEntryImpl, BlockEntry> {

    @Override
    public BlockEntry adapt(BlockEntryImpl source) {
      return source;
    }
  }

  private static class StopEntryAdapter implements
      IAdapter<StopEntryImpl, StopEntry> {

    @Override
    public StopEntry adapt(StopEntryImpl source) {
      return source;
    }
  }

  private static class RouteCollectionEntryAdapter implements
      IAdapter<RouteCollectionEntryImpl, RouteCollectionEntry> {

    @Override
    public RouteCollectionEntry adapt(RouteCollectionEntryImpl source) {
      return source;
    }
  }

  private static class RouteEntryAdapter implements
      IAdapter<RouteEntryImpl, RouteEntry> {

    @Override
    public RouteEntry adapt(RouteEntryImpl source) {
      return source;
    }
  }
}
