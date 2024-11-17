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
package org.onebusaway.transit_data_federation.impl.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.lucene.queryParser.ParseException;
import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.exceptions.InvalidArgumentServiceException;
import org.onebusaway.exceptions.NoSuchAgencyServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.model.SearchResult;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.RouteCollectionSearchService;
import org.onebusaway.transit_data_federation.services.RouteService;
import org.onebusaway.transit_data_federation.services.beans.GeospatialBeanService;
import org.onebusaway.transit_data_federation.services.beans.RouteBeanService;
import org.onebusaway.transit_data_federation.services.beans.RoutesBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopBeanService;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.strtree.STRtree;

@Component
class RoutesBeanServiceImpl implements RoutesBeanService {

  private static Logger _log = LoggerFactory.getLogger(RoutesBeanServiceImpl.class);

  private static final double MIN_SEARCH_SCORE = 1.0;

  @Autowired
  private RouteService _routeService;

  @Autowired
  private RouteCollectionSearchService _searchService;

  @Autowired
  private GeospatialBeanService _whereGeospatialService;

  @Autowired
  private RouteBeanService _routeBeanService;

  @Autowired
  private StopBeanService _stopService;

  @Autowired
  private TransitGraphDao _graphDao;

  private Map<AgencyAndId, STRtree> _stopTreesByRouteId = new HashMap<AgencyAndId, STRtree>();

  @PostConstruct
  public void setup() {

    for (StopEntry stop : _graphDao.getAllStops()) {
      Set<AgencyAndId> routeIds = _routeService.getRouteCollectionIdsForStop(stop.getId());
      for (AgencyAndId routeId : routeIds) {
        STRtree tree = _stopTreesByRouteId.get(routeId);
        if (tree == null) {
          tree = new STRtree();
          _stopTreesByRouteId.put(routeId, tree);
        }
        double x = stop.getStopLon();
        double y = stop.getStopLat();
        Envelope env = new Envelope(x, x, y, y);
        tree.insert(env, routeId);
      }
    }

    for (STRtree tree : _stopTreesByRouteId.values())
      tree.build();
  }

  @Override
  public RoutesBean getRoutesForQuery(SearchQueryBean query)
      throws ServiceException {
    if (query.getQuery() != null)
      return getRoutesWithRouteNameQuery(query);
    else
      return getRoutesWithoutRouteNameQuery(query);
  }

  @Cacheable
  @Override
  public ListBean<String> getRouteIdsForAgencyId(String agencyId) {
    AgencyEntry agency = _graphDao.getAgencyForId(agencyId);
    if (agency == null)
      throw new NoSuchAgencyServiceException(agencyId);
    List<String> ids = new ArrayList<String>();
    for (RouteCollectionEntry routeCollection : agency.getRouteCollections()) {
      AgencyAndId id = routeCollection.getId();
      ids.add(AgencyAndIdLibrary.convertToString(id));
    }
    return new ListBean<String>(ids, false);
  }

  @Cacheable
  @Override
  public ListBean<RouteBean> getRoutesForAgencyId(String agencyId) {
    AgencyEntry agency = _graphDao.getAgencyForId(agencyId);
    if (agency == null)
      throw new NoSuchAgencyServiceException(agencyId);
    List<RouteBean> routes = new ArrayList<RouteBean>();
    for (RouteCollectionEntry routeCollection : agency.getRouteCollections()) {
      AgencyAndId routeId = routeCollection.getId();
      RouteBean route = _routeBeanService.getRouteForId(routeId);
      routes.add(route);
    }
    return new ListBean<RouteBean>(routes, false);
  }

  /****
   * Private Methods
   ****/

  private RoutesBean getRoutesWithoutRouteNameQuery(SearchQueryBean query) {

    CoordinateBounds bounds = query.getBounds();

    List<AgencyAndId> stops = _whereGeospatialService.getStopsByBounds(bounds);

    Set<RouteBean> routes = new HashSet<RouteBean>();
    for (AgencyAndId stopId : stops) {
      StopBean stop = _stopService.getStopForId(stopId);
      routes.addAll(stop.getRoutes());
    }

    List<RouteBean> routeBeans = new ArrayList<RouteBean>(routes);
    boolean limitExceeded = BeanServiceSupport.checkLimitExceeded(routeBeans,
        query.getMaxCount());
    return constructResult(routeBeans, limitExceeded);
  }

  private RoutesBean getRoutesWithRouteNameQuery(SearchQueryBean query)
      throws ServiceException {

    SearchResult<AgencyAndId> result = searchForRoutes(query);

    List<RouteBean> routeBeans = new ArrayList<RouteBean>();
    CoordinateBounds bounds = query.getBounds();

    for (AgencyAndId id : result.getResults()) {
      STRtree tree = _stopTreesByRouteId.get(id);
      if (tree == null) {
        _log.warn("stop tree not found for routeId=" + id);
        continue;
      }
      Envelope env = new Envelope(bounds.getMinLon(), bounds.getMaxLon(),
          bounds.getMinLat(), bounds.getMaxLat());
      HasItemsVisitor v = new HasItemsVisitor();
      tree.query(env, v);

      if (v.hasItems()) {
        RouteBean routeBean = _routeBeanService.getRouteForId(id);
        routeBeans.add(routeBean);
      }
    }

    boolean limitExceeded = BeanServiceSupport.checkLimitExceeded(routeBeans,
        query.getMaxCount());

    return constructResult(routeBeans, limitExceeded);
  }

  private SearchResult<AgencyAndId> searchForRoutes(SearchQueryBean query)
      throws ServiceException, InvalidArgumentServiceException {

    try {
      return _searchService.searchForRoutesByName(query.getQuery(),
          query.getMaxCount() + 1, MIN_SEARCH_SCORE);
    } catch (IOException e) {
      throw new ServiceException();
    } catch (ParseException e) {
      throw new InvalidArgumentServiceException("query", "queryParseError");
    }
  }

  private RoutesBean constructResult(List<RouteBean> routeBeans,
      boolean limitExceeded) {

    Collections.sort(routeBeans, new RouteBeanIdComparator());

    RoutesBean result = new RoutesBean();
    result.setRoutes(routeBeans);
    result.setLimitExceeded(limitExceeded);
    return result;
  }

  private static class HasItemsVisitor implements ItemVisitor {

    private boolean _hasItems = false;

    public boolean hasItems() {
      return _hasItems;
    }

    @Override
    public void visitItem(Object arg0) {
      _hasItems = true;
    }
  }

}
