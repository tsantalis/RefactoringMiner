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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.onebusaway.transit_data_federation.impl.service_alerts.ServiceAlertLibrary;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.beans.ServiceAlertsBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Affects;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Consequence;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Id;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert.Cause;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TimeRange;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString.Translation;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ServiceAlertsBeanServiceImpl implements ServiceAlertsBeanService {

  private ServiceAlertsService _serviceAlertsService;

  @Autowired
  public void setServiceAlertsService(ServiceAlertsService serviceAlertsService) {
    _serviceAlertsService = serviceAlertsService;
  }

  @Override
  public ServiceAlertBean createServiceAlert(String agencyId,
      ServiceAlertBean situationBean) {
    ServiceAlert.Builder serviceAlertBuilder = getBeanAsServiceAlertBuilder(situationBean);
    ServiceAlert serviceAlert = _serviceAlertsService.createOrUpdateServiceAlert(
        serviceAlertBuilder, agencyId);
    return getServiceAlertAsBean(serviceAlert);
  }

  @Override
  public void updateServiceAlert(ServiceAlertBean situationBean) {
    ServiceAlert.Builder serviceAlertBuilder = getBeanAsServiceAlertBuilder(situationBean);
    _serviceAlertsService.createOrUpdateServiceAlert(serviceAlertBuilder, null);
  }

  @Override
  public void removeServiceAlert(AgencyAndId situationId) {
    _serviceAlertsService.removeServiceAlert(situationId);
  }

  @Override
  public ServiceAlertBean getServiceAlertForId(AgencyAndId situationId) {
    ServiceAlert serviceAlert = _serviceAlertsService.getServiceAlertForId(situationId);
    if (serviceAlert == null)
      return null;
    return getServiceAlertAsBean(serviceAlert);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForFederatedAgencyId(
      String agencyId) {
    List<ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForFederatedAgencyId(agencyId);
    return list(serviceAlerts);
  }

  @Override
  public void removeAllServiceAlertsForFederatedAgencyId(String agencyId) {
    _serviceAlertsService.removeAllServiceAlertsForFederatedAgencyId(agencyId);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForStopId(long time,
      AgencyAndId stopId) {
    List<ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopId(
        time, stopId);
    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForStopIds(long time,
      Iterable<AgencyAndId> stopIds) {
    List<ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopIds(
        time, stopIds);
    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForStopCall(long time,
      BlockInstance blockInstance, BlockStopTimeEntry blockStopTime,
      AgencyAndId vehicleId) {

    List<ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopCall(
        time, blockInstance, blockStopTime, vehicleId);

    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForVehicleJourney(long time,
      BlockTripInstance blockTripInstance, AgencyAndId vehicleId) {

    List<ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForVehicleJourney(
        time, blockTripInstance, vehicleId);

    return list(serviceAlerts);
  }

  /****
   * Private Methods
   ****/

  private List<ServiceAlertBean> list(List<ServiceAlert> serviceAlerts) {
    List<ServiceAlertBean> beans = new ArrayList<ServiceAlertBean>();
    for (ServiceAlert serviceAlert : serviceAlerts)
      beans.add(getServiceAlertAsBean(serviceAlert));
    return beans;
  }

  private ServiceAlertBean getServiceAlertAsBean(ServiceAlert serviceAlert) {

    ServiceAlertBean bean = new ServiceAlertBean();

    AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getId());
    bean.setId(AgencyAndIdLibrary.convertToString(id));
    bean.setCreationTime(serviceAlert.getCreationTime());

    bean.setActiveWindows(getRangesAsBeans(serviceAlert.getActiveWindowList()));
    bean.setPublicationWindows(getRangesAsBeans(serviceAlert.getPublicationWindowList()));

    /**
     * Reasons
     */
    if (serviceAlert.hasCause())
      bean.setReason(getCauseAsReason(serviceAlert.getCause()));

    /**
     * Text descriptions
     */
    bean.setSummaries(getTranslatedStringsAsNLSBeans(serviceAlert.getSummary()));
    bean.setDescriptions(getTranslatedStringsAsNLSBeans(serviceAlert.getDescription()));
    bean.setUrls(getTranslatedStringsAsNLSBeans(serviceAlert.getUrl()));

    if (serviceAlert.hasSeverity())
      bean.setSeverity(ServiceAlertLibrary.convertSeverity(serviceAlert.getSeverity()));

    bean.setAllAffects(getAffectsAsBeans(serviceAlert));
    bean.setConsequences(getConsequencesAsBeans(serviceAlert));

    return bean;
  }

  private ServiceAlert.Builder getBeanAsServiceAlertBuilder(
      ServiceAlertBean bean) {

    ServiceAlert.Builder situation = ServiceAlert.newBuilder();

    if (bean.getId() != null && !bean.getId().isEmpty()) {
      AgencyAndId id = AgencyAndIdLibrary.convertFromString(bean.getId());
      situation.setId(ServiceAlertLibrary.id(id));
    }
    situation.setCreationTime(bean.getCreationTime());

    situation.addAllActiveWindow(getBeansAsRanges(bean.getActiveWindows()));
    situation.addAllPublicationWindow(getBeansAsRanges(bean.getPublicationWindows()));

    /**
     * Reasons
     */
    situation.setCause(getReasonAsCause(bean.getReason()));

    /**
     * Text descriptions
     */
    situation.setSummary(getNLSBeansAsTranslatedString(bean.getSummaries()));
    situation.setDescription(getNLSBeansAsTranslatedString(bean.getDescriptions()));
    situation.setUrl(getNLSBeansAsTranslatedString(bean.getUrls()));

    if (bean.getSeverity() != null)
      situation.setSeverity(ServiceAlertLibrary.convertSeverity(bean.getSeverity()));

    situation.addAllAffects(getBeanAsAffects(bean));
    situation.addAllConsequence(getBeanAsConsequences(bean));

    return situation;
  }

  /****
   * Situations Affects
   ****/

  private List<SituationAffectsBean> getAffectsAsBeans(ServiceAlert serviceAlert) {

    if (serviceAlert.getAffectsCount() == 0)
      return null;

    List<SituationAffectsBean> beans = new ArrayList<SituationAffectsBean>();

    for (Affects affects : serviceAlert.getAffectsList()) {
      SituationAffectsBean bean = new SituationAffectsBean();
      if (affects.hasAgencyId())
        bean.setAgencyId(affects.getAgencyId());
      if (affects.hasApplicationId())
        bean.setApplicationId(affects.getApplicationId());
      if (affects.hasRouteId()) {
        AgencyAndId routeId = ServiceAlertLibrary.agencyAndId(affects.getRouteId());
        bean.setRouteId(AgencyAndId.convertToString(routeId));
      }
      if (affects.hasDirectionId())
        bean.setDirectionId(affects.getDirectionId());
      if (affects.hasTripId()) {
        AgencyAndId tripId = ServiceAlertLibrary.agencyAndId(affects.getTripId());
        bean.setTripId(AgencyAndId.convertToString(tripId));
      }
      if (affects.hasStopId()) {
        AgencyAndId stopId = ServiceAlertLibrary.agencyAndId(affects.getStopId());
        bean.setStopId(AgencyAndId.convertToString(stopId));
      }
      if (affects.hasApplicationId())
        bean.setApplicationId(affects.getApplicationId());
      beans.add(bean);
    }
    return beans;
  }

  private List<Affects> getBeanAsAffects(ServiceAlertBean bean) {

    List<Affects> affects = new ArrayList<ServiceAlerts.Affects>();

    if (!CollectionsLibrary.isEmpty(bean.getAllAffects())) {
      for (SituationAffectsBean affectsBean : bean.getAllAffects()) {
        Affects.Builder builder = Affects.newBuilder();
        if (affectsBean.getAgencyId() != null)
          builder.setAgencyId(affectsBean.getAgencyId());
        if (affectsBean.getApplicationId() != null)
          builder.setApplicationId(affectsBean.getApplicationId());
        if (affectsBean.getRouteId() != null) {
          AgencyAndId routeId = AgencyAndId.convertFromString(affectsBean.getRouteId());
          builder.setRouteId(ServiceAlertLibrary.id(routeId));
        }
        if (affectsBean.getDirectionId() != null)
          builder.setDirectionId(affectsBean.getDirectionId());
        if (affectsBean.getTripId() != null) {
          AgencyAndId tripId = AgencyAndId.convertFromString(affectsBean.getTripId());
          builder.setTripId(ServiceAlertLibrary.id(tripId));
        }
        if (affectsBean.getStopId() != null) {
          AgencyAndId stopId = AgencyAndId.convertFromString(affectsBean.getStopId());
          builder.setStopId(ServiceAlertLibrary.id(stopId));
        }
        affects.add(builder.build());
      }
    }

    return affects;
  }

  /****
   * Consequence
   ****/

  private List<SituationConsequenceBean> getConsequencesAsBeans(
      ServiceAlert serviceAlert) {
    if (serviceAlert.getConsequenceCount() == 0)
      return null;
    List<SituationConsequenceBean> beans = new ArrayList<SituationConsequenceBean>();
    for (Consequence consequence : serviceAlert.getConsequenceList()) {
      SituationConsequenceBean bean = new SituationConsequenceBean();
      if (consequence.hasEffect())
        bean.setEffect(ServiceAlertLibrary.convertEffect(consequence.getEffect()));
      if (consequence.hasDetourPath())
        bean.setDetourPath(consequence.getDetourPath());
      if (consequence.getDetourStopIdsCount() != 0) {
        List<String> stopIds = new ArrayList<String>();
        for (Id stopId : consequence.getDetourStopIdsList()) {
          AgencyAndId id = ServiceAlertLibrary.agencyAndId(stopId);
          stopIds.add(AgencyAndId.convertToString(id));
        }
        bean.setDetourStopIds(stopIds);
      }
      beans.add(bean);
    }
    return beans;
  }

  private List<Consequence> getBeanAsConsequences(ServiceAlertBean bean) {

    List<Consequence> consequences = new ArrayList<Consequence>();

    if (!CollectionsLibrary.isEmpty(bean.getConsequences())) {
      for (SituationConsequenceBean consequence : bean.getConsequences()) {
        Consequence.Builder builder = Consequence.newBuilder();
        if (consequence.getEffect() != null)
          builder.setEffect(ServiceAlertLibrary.convertEffect(consequence.getEffect()));
        if (consequence.getDetourPath() != null)
          builder.setDetourPath(consequence.getDetourPath());
        if (!CollectionsLibrary.isEmpty(consequence.getDetourStopIds())) {
          List<Id> detourStopIds = new ArrayList<Id>();
          for (String detourStopId : consequence.getDetourStopIds()) {
            Id id = ServiceAlertLibrary.id(AgencyAndId.convertFromString(detourStopId));
            detourStopIds.add(id);
          }
          builder.addAllDetourStopIds(detourStopIds);
        }
        consequences.add(builder.build());
      }
    }

    return consequences;
  }

  /****
   * 
   ****/

  private Cause getReasonAsCause(String reason) {
    if (reason == null)
      return Cause.UNKNOWN_CAUSE;
    return Cause.valueOf(reason);
  }

  private String getCauseAsReason(Cause cause) {
    return cause.toString();
  }

  /****
   * 
   ****/

  private List<TimeRangeBean> getRangesAsBeans(List<TimeRange> ranges) {
    if (ranges == null || ranges.isEmpty())
      return null;
    List<TimeRangeBean> beans = new ArrayList<TimeRangeBean>();
    for (TimeRange range : ranges) {
      TimeRangeBean bean = new TimeRangeBean();
      if (range.hasStart())
        bean.setFrom(range.getStart());
      if (range.hasEnd())
        bean.setTo(range.getEnd());
      beans.add(bean);
    }
    return beans;
  }

  private List<TimeRange> getBeansAsRanges(List<TimeRangeBean> beans) {
    if (beans == null)
      return Collections.emptyList();
    List<TimeRange> ranges = new ArrayList<TimeRange>();
    for (TimeRangeBean bean : beans) {
      TimeRange.Builder range = TimeRange.newBuilder();
      if (bean.getFrom() > 0)
        range.setStart(bean.getFrom());
      if (bean.getTo() > 0)
        range.setEnd(bean.getTo());
      if (range.hasStart() || range.hasEnd())
        ranges.add(range.build());
    }
    return ranges;
  }

  private TranslatedString getNLSBeansAsTranslatedString(
      List<NaturalLanguageStringBean> nlsBeans) {
    TranslatedString.Builder builder = TranslatedString.newBuilder();
    if (!CollectionsLibrary.isEmpty(nlsBeans)) {
      for (NaturalLanguageStringBean nlsBean : nlsBeans) {
        if (nlsBean.getValue() == null)
          continue;
        Translation.Builder translation = Translation.newBuilder();
        translation.setText(nlsBean.getValue());
        translation.setLanguage(nlsBean.getLang());
        builder.addTranslation(translation);
      }
    }
    return builder.build();
  }

  private List<NaturalLanguageStringBean> getTranslatedStringsAsNLSBeans(
      TranslatedString strings) {

    if (strings == null || strings.getTranslationCount() == 0)
      return null;

    List<NaturalLanguageStringBean> nlsBeans = new ArrayList<NaturalLanguageStringBean>();
    for (Translation translation : strings.getTranslationList()) {
      NaturalLanguageStringBean nls = new NaturalLanguageStringBean();
      nls.setValue(translation.getText());
      nls.setLang(translation.getLanguage());
      nlsBeans.add(nls);
    }

    return nlsBeans;
  }
}
