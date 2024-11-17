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
package org.onebusaway.transit_data_federation.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExtendedCalendarServiceImpl implements ExtendedCalendarService {

  private CalendarService _calendarService;

  private TransitGraphDao _transitGraphDao;

  private Map<ServiceIdActivation, List<Date>> _serviceDatesByServiceIds = new HashMap<ServiceIdActivation, List<Date>>();

  private double _serviceDateRangeCacheInterval = 4 * 60 * 60;

  private Cache _serviceDateRangeCache;

  private int _serviceDateLowerBoundsInWeeks = -1;

  private int _serviceDateUpperBoundsInWeeks = -1;

  public void setServiceDateLowerBoundsInWeeks(int serviceDateLowerBoundsInWeeks) {
    _serviceDateLowerBoundsInWeeks = serviceDateLowerBoundsInWeeks;
  }

  public void setServiceDateUpperBoundsInWeeks(int serviceDateUpperBoundsInWeeks) {
    _serviceDateUpperBoundsInWeeks = serviceDateUpperBoundsInWeeks;
  }

  @Autowired
  public void setCalendarService(CalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  public void setServiceDateRangeCacheInterval(int hours) {
    _serviceDateRangeCacheInterval = hours * 60 * 60;
  }

  public void setServiceDateRangeCache(Cache serviceDateRangeCache) {
    _serviceDateRangeCache = serviceDateRangeCache;
  }

  @PostConstruct
  @Refreshable(dependsOn = RefreshableResources.CALENDAR_DATA)
  public void start() {
    cacheServiceDatesForServiceIds();
  }

  @Cacheable
  @Override
  public Set<ServiceDate> getServiceDatesForServiceIds(
      ServiceIdActivation serviceIds) {

    Set<ServiceDate> serviceDates = null;

    List<LocalizedServiceId> activeServiceIds = serviceIds.getActiveServiceIds();
    List<LocalizedServiceId> inactiveServiceIds = serviceIds.getInactiveServiceIds();

    for (LocalizedServiceId activeServiceId : activeServiceIds) {

      Set<ServiceDate> dates = _calendarService.getServiceDatesForServiceId(activeServiceId.getId());
      if (dates.isEmpty())
        return Collections.emptySet();
      if (serviceDates == null)
        serviceDates = new HashSet<ServiceDate>(dates);
      else
        serviceDates.retainAll(dates);
      if (serviceDates.isEmpty())
        return Collections.emptySet();
    }

    for (LocalizedServiceId inactiveServiceId : inactiveServiceIds) {
      Set<ServiceDate> dates = _calendarService.getServiceDatesForServiceId(inactiveServiceId.getId());
      serviceDates.removeAll(dates);
    }

    return serviceDates;
  }

  @Cacheable
  @Override
  public Set<Date> getDatesForServiceIds(ServiceIdActivation serviceIds) {

    Set<Date> serviceDates = null;

    List<LocalizedServiceId> activeServiceIds = serviceIds.getActiveServiceIds();
    List<LocalizedServiceId> inactiveServiceIds = serviceIds.getInactiveServiceIds();

    for (LocalizedServiceId activeServiceId : activeServiceIds) {

      List<Date> dates = _calendarService.getDatesForLocalizedServiceId(activeServiceId);
      if (dates.isEmpty())
        return Collections.emptySet();
      if (serviceDates == null)
        serviceDates = new HashSet<Date>(dates);
      else
        serviceDates.retainAll(dates);
      if (serviceDates.isEmpty())
        return Collections.emptySet();
    }

    for (LocalizedServiceId inactiveServiceId : inactiveServiceIds) {
      List<Date> dates = _calendarService.getDatesForLocalizedServiceId(inactiveServiceId);
      serviceDates.removeAll(dates);
    }

    return serviceDates;
  }

  @Cacheable
  public List<Date> getDatesForServiceIdsAsOrderedList(
      ServiceIdActivation serviceIds) {
    Set<Date> dates = getDatesForServiceIds(serviceIds);
    List<Date> list = new ArrayList<Date>(dates);
    Collections.sort(list);
    return list;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<Date> getServiceDatesWithinRange(
      ServiceIdActivation serviceIds, ServiceInterval interval, Date from,
      Date to) {

    if (_serviceDateRangeCache == null)
      return getServiceDatesWithinRangeExact(serviceIds, interval, from, to);

    ServiceDateRangeKey key = getCacheKey(serviceIds, interval, from, to);
    Element element = _serviceDateRangeCache.get(key);

    if (element == null) {

      serviceIds = key.getServiceIds();
      interval = key.getInterval();
      from = key.getFromTime();
      to = key.getToTime();

      Collection<Date> values = getServiceDatesWithinRangeExact(serviceIds,
          interval, from, to);

      element = new Element(key, values);
      _serviceDateRangeCache.put(element);
    }

    return (Collection<Date>) element.getValue();
  }

  @Override
  @Cacheable
  public boolean areServiceIdsActiveOnServiceDate(
      ServiceIdActivation serviceIds, Date serviceDate) {

    List<LocalizedServiceId> activeServiceIds = serviceIds.getActiveServiceIds();
    List<LocalizedServiceId> inactiveServiceIds = serviceIds.getInactiveServiceIds();

    // 95% of configs look like this
    if (activeServiceIds.size() == 1 && inactiveServiceIds.isEmpty()) {
      LocalizedServiceId lsid = activeServiceIds.get(0);
      return _calendarService.isLocalizedServiceIdActiveOnDate(lsid,
          serviceDate);
    }

    for (LocalizedServiceId lsid : activeServiceIds) {
      if (!_calendarService.isLocalizedServiceIdActiveOnDate(lsid, serviceDate))
        return false;
    }

    for (LocalizedServiceId lsid : inactiveServiceIds) {
      if (_calendarService.isLocalizedServiceIdActiveOnDate(lsid, serviceDate))
        return false;
    }

    return true;
  }

  @Override
  public List<Date> getServiceDatesForInterval(ServiceIdActivation serviceIds,
      ServiceInterval serviceInterval, long time, boolean findDepartures) {

    if (findDepartures)
      return getNextServiceDatesForDepartureInterval(serviceIds,
          serviceInterval, time);
    else
      return getPreviousServiceDatesForArrivalInterval(serviceIds,
          serviceInterval, time);
  }

  @Override
  public List<Date> getNextServiceDatesForDepartureInterval(
      ServiceIdActivation serviceIds, ServiceInterval serviceInterval, long time) {

    List<Date> serviceDates = _serviceDatesByServiceIds.get(serviceIds);

    if (CollectionsLibrary.isEmpty(serviceDates))
      return Collections.emptyList();

    int offset = (serviceInterval.getMaxDeparture() - serviceInterval.getMinDeparture()) * 1000;
    Date offsetDate = new Date(time - offset);
    int startIndex = Collections.binarySearch(serviceDates, offsetDate);

    if (startIndex < 0)
      startIndex = -(startIndex + 1);
    startIndex = Math.max(0, startIndex - 1);

    List<Date> serviceDatesToReturn = new ArrayList<Date>();
    boolean directHit = false;

    for (int index = startIndex; index < serviceDates.size(); index++) {

      Date serviceDate = serviceDates.get(index);

      long timeFrom = serviceDate.getTime() + serviceInterval.getMinDeparture()
          * 1000;
      long timeTo = serviceDate.getTime() + serviceInterval.getMaxDeparture()
          * 1000;

      if (time < timeFrom) {

        if (!directHit) {
          serviceDatesToReturn.add(serviceDate);
        }

        return serviceDatesToReturn;
      }

      if (timeFrom <= time && time <= timeTo) {
        serviceDatesToReturn.add(serviceDate);
        directHit = true;
      }
    }

    return serviceDatesToReturn;
  }

  @Override
  public List<Date> getPreviousServiceDatesForArrivalInterval(
      ServiceIdActivation serviceIds, ServiceInterval serviceInterval, long time) {

    List<Date> serviceDates = _serviceDatesByServiceIds.get(serviceIds);

    if (CollectionsLibrary.isEmpty(serviceDates))
      return Collections.emptyList();

    int offset = (serviceInterval.getMaxDeparture() - serviceInterval.getMinDeparture()) * 1000;
    Date offsetDate = new Date(time + offset);
    int endIndex = Collections.binarySearch(serviceDates, offsetDate);

    if (endIndex < 0)
      endIndex = -(endIndex + 1);
    endIndex = Math.min(serviceDates.size() - 1, endIndex + 1);

    List<Date> serviceDatesToReturn = new ArrayList<Date>();
    boolean directHit = false;

    for (int index = endIndex; index >= 0; index--) {

      Date serviceDate = serviceDates.get(index);

      long timeFrom = serviceDate.getTime() + serviceInterval.getMinDeparture()
          * 1000;
      long timeTo = serviceDate.getTime() + serviceInterval.getMaxDeparture()
          * 1000;

      if (time > timeTo) {

        if (!directHit) {
          serviceDatesToReturn.add(serviceDate);
        }

        return serviceDatesToReturn;
      }

      if (timeFrom <= time && time <= timeTo) {
        serviceDatesToReturn.add(serviceDate);
        directHit = true;
      }
    }

    return serviceDatesToReturn;
  }

  /****
   * Private Methods
   ****/

  private ServiceDateRangeKey getCacheKey(ServiceIdActivation serviceIds,
      ServiceInterval interval, Date from, Date to) {

    Serializable serviceIdsKey = getServiceIdsKey(serviceIds);
    int fromStopTime = (int) (Math.floor(interval.getMinArrival()
        / _serviceDateRangeCacheInterval) * _serviceDateRangeCacheInterval);
    int toStopTime = (int) (Math.ceil(interval.getMaxDeparture()
        / _serviceDateRangeCacheInterval) * _serviceDateRangeCacheInterval);
    double m = _serviceDateRangeCacheInterval * 1000;
    long fromTime = (long) (Math.floor(from.getTime() / m) * m);
    long toTime = (long) (Math.ceil(to.getTime() / m) * m);
    return new ServiceDateRangeKey(serviceIdsKey, fromStopTime, toStopTime,
        fromTime, toTime);
  }

  private Serializable getServiceIdsKey(ServiceIdActivation serviceIds) {

    List<LocalizedServiceId> activeServiceIds = serviceIds.getActiveServiceIds();
    List<LocalizedServiceId> inactiveServiceIds = serviceIds.getInactiveServiceIds();

    if (activeServiceIds.size() == 1 && inactiveServiceIds.isEmpty())
      return activeServiceIds.get(0);

    return serviceIds;
  }

  private Collection<Date> getServiceDatesWithinRangeExact(
      ServiceIdActivation serviceIds, ServiceInterval interval, Date from,
      Date to) {
    Set<Date> serviceDates = null;

    List<LocalizedServiceId> activeServiceIds = serviceIds.getActiveServiceIds();
    List<LocalizedServiceId> inactiveServiceIds = serviceIds.getInactiveServiceIds();

    // System.out.println(serviceIds + " " + interval + " " + from + " " + to);

    // 95% of configs look like this
    if (activeServiceIds.size() == 1 && inactiveServiceIds.isEmpty())
      return _calendarService.getServiceDatesWithinRange(
          activeServiceIds.get(0), interval, from, to);

    for (LocalizedServiceId serviceId : activeServiceIds) {
      List<Date> dates = _calendarService.getServiceDatesWithinRange(serviceId,
          interval, from, to);

      // If the dates are ever empty here, we can short circuit to no dates
      if (dates.isEmpty())
        return Collections.emptyList();

      if (serviceDates == null)
        serviceDates = new HashSet<Date>(dates);
      else
        serviceDates.retainAll(serviceDates);

      // If the dates are empty here after the intersection operation, we can
      // short circuit to no dates
      if (serviceDates.isEmpty())
        return Collections.emptyList();
    }

    if (!inactiveServiceIds.isEmpty()) {
      for (LocalizedServiceId serviceId : inactiveServiceIds) {
        List<Date> dates = _calendarService.getServiceDatesWithinRange(
            serviceId, interval, from, to);
        serviceDates.removeAll(dates);
      }
    }

    return serviceDates;
  }

  private void cacheServiceDatesForServiceIds() {

    if(_serviceDateRangeCache != null) {
      _serviceDateRangeCache.removeAll();
    }
    
    _serviceDatesByServiceIds.clear();
    
    Set<ServiceIdActivation> allServiceIds = determineAllServiceIds();

    Date lowerBounds = null;
    if (_serviceDateLowerBoundsInWeeks != -1) {
      Calendar c = Calendar.getInstance();
      c.add(Calendar.WEEK_OF_YEAR, -_serviceDateLowerBoundsInWeeks);
      lowerBounds = c.getTime();
    }

    Date upperBounds = null;
    if (_serviceDateUpperBoundsInWeeks != -1) {
      Calendar c = Calendar.getInstance();
      c.add(Calendar.WEEK_OF_YEAR, _serviceDateUpperBoundsInWeeks);
      upperBounds = c.getTime();
    }

    for (ServiceIdActivation serviceIds : allServiceIds) {

      List<Date> dates = computeServiceDatesForServiceIds(serviceIds,
          lowerBounds, upperBounds);
      _serviceDatesByServiceIds.put(serviceIds, dates);
    }
  }

  private Set<ServiceIdActivation> determineAllServiceIds() {
    Set<ServiceIdActivation> allServiceIds = new HashSet<ServiceIdActivation>();

    for (BlockEntry block : _transitGraphDao.getAllBlocks()) {
      for (BlockConfigurationEntry blockConfig : block.getConfigurations()) {
        ServiceIdActivation serviceIds = blockConfig.getServiceIds();
        allServiceIds.add(serviceIds);
      }
    }
    return allServiceIds;
  }

  private List<Date> computeServiceDatesForServiceIds(
      ServiceIdActivation serviceIds, Date lowerBounds, Date upperBounds) {
    Set<Date> serviceDates = null;

    for (LocalizedServiceId lsid : serviceIds.getActiveServiceIds()) {
      List<Date> dates = _calendarService.getDatesForLocalizedServiceId(lsid);
      if (dates == null)
        dates = Collections.emptyList();
      if (serviceDates == null)
        serviceDates = new HashSet<Date>(dates);
      else
        serviceDates.retainAll(dates);
    }

    for (LocalizedServiceId lsid : serviceIds.getInactiveServiceIds()) {
      List<Date> dates = _calendarService.getDatesForLocalizedServiceId(lsid);
      if (serviceDates != null)
        serviceDates.removeAll(dates);
    }

    List<Date> dates = new ArrayList<Date>();
    if (serviceDates != null) {
      for (Date serviceDate : serviceDates) {
        if ((lowerBounds == null || lowerBounds.before(serviceDate))
            && (upperBounds == null || serviceDate.before(upperBounds)))
          dates.add(serviceDate);
      }
    }

    Collections.sort(dates);
    return dates;
  }

  private class ServiceDateRangeKey {
    private final Serializable _serviceIds;
    private final int _fromStopTime;
    private final int _toStopTime;
    private final long _fromTime;
    private final long _toTime;

    public ServiceDateRangeKey(Serializable serviceIds, int fromStopTime,
        int toStopTime, long fromTime, long toTime) {
      if (serviceIds == null)
        throw new IllegalStateException("serviceIds cannot be null");
      _serviceIds = serviceIds;
      _fromStopTime = fromStopTime;
      _toStopTime = toStopTime;
      _fromTime = fromTime;
      _toTime = toTime;
    }

    public ServiceIdActivation getServiceIds() {
      if (_serviceIds instanceof ServiceIdActivation) {
        return (ServiceIdActivation) _serviceIds;
      } else if (_serviceIds instanceof LocalizedServiceId) {
        return new ServiceIdActivation((LocalizedServiceId) _serviceIds);
      } else {
        throw new IllegalStateException("unknown service id type: "
            + _serviceIds);
      }
    }

    public ServiceInterval getInterval() {
      return new ServiceInterval(_fromStopTime, _toStopTime);
    }

    public Date getFromTime() {
      return new Date(_fromTime);
    }

    public Date getToTime() {
      return new Date(_toTime);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + _fromStopTime;
      result = prime * result + (int) (_fromTime ^ (_fromTime >>> 32));
      result = prime * result + _serviceIds.hashCode();
      result = prime * result + _toStopTime;
      result = prime * result + (int) (_toTime ^ (_toTime >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ServiceDateRangeKey other = (ServiceDateRangeKey) obj;
      if (_fromStopTime != other._fromStopTime)
        return false;
      if (_fromTime != other._fromTime)
        return false;
      if (!_serviceIds.equals(other._serviceIds))
        return false;
      if (_toStopTime != other._toStopTime)
        return false;
      if (_toTime != other._toTime)
        return false;
      return true;
    }
  }
}
