/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.transit_data.model.service_alerts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.QueryBean;

@QueryBean
public class SituationQueryBean implements Serializable {

  private static final long serialVersionUID = 1L;

  // Not clear whether this is service alert agency ID or affects agency ID.
  // Treating it as latter for now.
  private String agencyId;

  private List<String> stopIds;

  // Asked Brian about time in developer list, he said:
  // "Generally, I'd set it to now(). You can use it to query what service
  // ids were active at some point in the past or future by changing the
  // time value."
  // http://groups.google.com/group/onebusaway-developers/msg/fb9b44cd9ba7bef4?hl=en
  private long time;

  private List<RouteIdAndDirection> routes = new ArrayList<RouteIdAndDirection>();

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public List<String> getStopIds() {
    return stopIds;
  }

  public void setStopIds(List<String> stopIds) {
    this.stopIds = stopIds;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public List<RouteIdAndDirection> getRoutes() {
    return this.routes;
  }

  public void setRoutes(List<RouteIdAndDirection> routes) {
    this.routes = routes;
  }

  public void addRoute(String id, String direction) {
    routes.add(new RouteIdAndDirection(id, direction));
  }
  
  public class RouteIdAndDirection implements Serializable {
    private static final long serialVersionUID = 1L;
    public RouteIdAndDirection(String routeId, String direction) {
      this.routeId = routeId;
      this.direction = direction;
    }
    public String routeId;
    public String direction;
  }

}
