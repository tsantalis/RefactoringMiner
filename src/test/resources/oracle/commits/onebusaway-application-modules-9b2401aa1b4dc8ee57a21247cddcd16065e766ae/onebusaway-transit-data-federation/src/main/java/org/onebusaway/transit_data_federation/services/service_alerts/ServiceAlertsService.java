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
package org.onebusaway.transit_data_federation.services.service_alerts;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Affects;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;

public interface ServiceAlertsService {

  /**
   * Create a service alert. To assist with data federation in the
   * {@link TransitDataService}, each service alert is assigned to an agency, as
   * determined by the 'agencyId' parameter. This doesn't mean that the service
   * alert is 'active' for the agency (as returned by
   * {@link #getServiceAlertsForAgencyId(long, String)}) but it will be returned
   * in a call to {@link #getServiceAlertsForFederatedAgencyId(String)}. This
   * also determines the agency id used in the service alerts id (
   * {@link ServiceAlert#getId()}).
   * 
   * @param builder the filled-in service alert builder
   * @param defaultAgencyId the agency to assign the service alert to
   * 
   * @return the built service alert
   */

  public ServiceAlert createOrUpdateServiceAlert(ServiceAlert.Builder builder,
      String defaultAgencyId);

  public void removeServiceAlert(AgencyAndId serviceAlertId);

  public void removeServiceAlerts(List<AgencyAndId> serviceAlertIds);

  /**
   * Remove all service alerts with the specified agency id. This would remove
   * all the service alerts returned by a call to
   * {@link #getServiceAlertsForFederatedAgencyId(String)}.
   * 
   * @param agencyId
   */
  public void removeAllServiceAlertsForFederatedAgencyId(String agencyId);

  public ServiceAlert getServiceAlertForId(AgencyAndId serviceAlertId);

  public List<ServiceAlert> getAllServiceAlerts();

  /**
   * This returns all the service alerts with the specified federated agency id,
   * as set in {@link ServiceAlert#getId()} and in the call to
   * {@link #createServiceAlert(String, org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert.Builder)}
   * . Contrast this with {@link #getServiceAlertsForAgencyId(long, String)},
   * which find service alerts affecting a particular agency.
   * 
   * @param agencyId
   * @return
   */
  public List<ServiceAlert> getServiceAlertsForFederatedAgencyId(String agencyId);

  /**
   * This returns the set of service alerts affecting a particular agency, as
   * determined by {@link Affects#getAgencyId()}.
   * 
   * @param time
   * @param agencyId
   * @return the set of service alerts affecting the specified agency
   */
  public List<ServiceAlert> getServiceAlertsForAgencyId(long time,
      String agencyId);

  public List<ServiceAlert> getServiceAlertsForStopId(long time,
      AgencyAndId stopId);
  
  public List<ServiceAlert> getServiceAlertsForStopIds(long time,
      Iterable<AgencyAndId> stopIds);

  public List<ServiceAlert> getServiceAlertsForStopCall(long time,
      BlockInstance blockInstance, BlockStopTimeEntry blockStopTime,
      AgencyAndId vehicleId);

  public List<ServiceAlert> getServiceAlertsForVehicleJourney(long time,
      BlockTripInstance blockTripInstance,
      AgencyAndId vehicleId);

  public List<ServiceAlert> getServiceAlerts(SituationQueryBean query);

  /**
   * Set whether the ServiceAlerts service is responsible for persisting service alerts
   * across application restarts, or whether an external service will handle that.
   * 
   * @param persist
   */
  public void doPersistence(boolean persist);

}
