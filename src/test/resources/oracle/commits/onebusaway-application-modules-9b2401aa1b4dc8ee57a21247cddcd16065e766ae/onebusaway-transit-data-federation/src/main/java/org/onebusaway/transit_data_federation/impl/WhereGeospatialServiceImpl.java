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
package org.onebusaway.transit_data_federation.impl;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.beans.GeospatialBeanService;
import org.onebusaway.transit_data_federation.services.beans.RouteBeanService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.strtree.STRtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

@Component
class WhereGeospatialServiceImpl implements GeospatialBeanService {
  
  private static Logger _log = LoggerFactory.getLogger(WhereGeospatialServiceImpl.class);

  private TransitGraphDao _transitGraphDao;

  private STRtree _tree;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }


  @PostConstruct
  @Refreshable(dependsOn = RefreshableResources.STOP_GEOSPATIAL_INDEX)
  public void initialize() {

    List<StopEntry> stops = _transitGraphDao.getAllStops();
    
    if (stops.size() == 0) {
      _tree = null;
      return;
    }
    
    _tree = new STRtree(stops.size());

    for (StopEntry stop : stops) {
      float x = (float) stop.getStopLon();
      float y = (float) stop.getStopLat();
      Envelope env = new Envelope(x, x, y, y);
      _tree.insert(env, stop.getId());
    }

    _tree.build();
  }

  /****
   * {@link RouteBeanService} Interface
   ****/

  @Override
  public List<AgencyAndId> getStopsByBounds(CoordinateBounds bounds) {
    
    if( _tree == null) {
      _log.warn("Stop tree is empty!");
      return Collections.emptyList();
    }
    
    double xMin = bounds.getMinLon();
    double yMin = bounds.getMinLat();
    double xMax = bounds.getMaxLon();
    double yMax = bounds.getMaxLat();

    TreeVisistor v = new TreeVisistor();
    _tree.query(new Envelope(xMin, xMax, yMin, yMax), v);
    return v.getIdsInRange();
  }

  private class TreeVisistor implements ItemVisitor {

    private List<AgencyAndId> _idsInRange = new ArrayList<AgencyAndId>();

    public List<AgencyAndId> getIdsInRange() {
      return _idsInRange;
    }

    @Override
    public void visitItem(Object obj) {
      _idsInRange.add((AgencyAndId) obj);
    }
  }

}
