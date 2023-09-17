/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.calendar.service.impl;

import com.liferay.calendar.CalendarBookingDurationException;
import com.liferay.calendar.CalendarBookingRecurrenceException;
import com.liferay.calendar.CalendarBookingTitleException;
import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.calendar.model.CalendarBookingConstants;
import com.liferay.calendar.notification.NotificationTemplateType;
import com.liferay.calendar.notification.NotificationType;
import com.liferay.calendar.recurrence.Recurrence;
import com.liferay.calendar.recurrence.RecurrenceSerializer;
import com.liferay.calendar.service.base.CalendarBookingLocalServiceBaseImpl;
import com.liferay.calendar.social.CalendarActivityKeys;
import com.liferay.calendar.util.CalendarDataFormat;
import com.liferay.calendar.util.CalendarDataHandler;
import com.liferay.calendar.util.CalendarDataHandlerFactory;
import com.liferay.calendar.util.JCalendarUtil;
import com.liferay.calendar.util.NotificationUtil;
import com.liferay.calendar.util.PortletPropsValues;
import com.liferay.calendar.util.RecurrenceUtil;
import com.liferay.calendar.workflow.CalendarBookingApprovalWorkflow;
import com.liferay.calendar.workflow.CalendarBookingWorkflowConstants;
import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.sanitizer.Sanitizer;
import com.liferay.portal.kernel.sanitizer.SanitizerUtil;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.systemevent.SystemEvent;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.SystemEventConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.model.AssetLinkConstants;
import com.liferay.portlet.social.model.SocialActivityConstants;
import com.liferay.portlet.trash.model.TrashEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Lundgren
 * @author Fabio Pezzutto
 * @author Marcellus Tavares
 * @author Pier Paolo Ramon
 */
public class CalendarBookingLocalServiceImpl
	extends CalendarBookingLocalServiceBaseImpl {

	@Override
	public CalendarBooking addCalendarBooking(
			long userId, long calendarId, long[] childCalendarIds,
			long parentCalendarBookingId, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, String location, long startTime,
			long endTime, boolean allDay, String recurrence, long firstReminder,
			String firstReminderType, long secondReminder,
			String secondReminderType, ServiceContext serviceContext)
		throws PortalException, SystemException {

		// Calendar booking

		User user = userPersistence.findByPrimaryKey(userId);
		Calendar calendar = calendarPersistence.findByPrimaryKey(calendarId);

		long calendarBookingId = counterLocalService.increment();

		for (Locale locale : descriptionMap.keySet()) {
			String sanitizedDescription = SanitizerUtil.sanitize(
				calendar.getCompanyId(), calendar.getGroupId(), userId,
				CalendarBooking.class.getName(), calendarBookingId,
				ContentTypes.TEXT_HTML, Sanitizer.MODE_ALL,
				descriptionMap.get(locale), null);

			descriptionMap.put(locale, sanitizedDescription);
		}

		java.util.Calendar startTimeJCalendar = JCalendarUtil.getJCalendar(
			startTime);
		java.util.Calendar endTimeJCalendar = JCalendarUtil.getJCalendar(
			endTime);

		if (allDay) {
			startTimeJCalendar = JCalendarUtil.toMidnightJCalendar(
				startTimeJCalendar);
			endTimeJCalendar = JCalendarUtil.toLastHourJCalendar(
				endTimeJCalendar);
		}

		if (firstReminder < secondReminder) {
			long originalSecondReminder = secondReminder;

			secondReminder = firstReminder;
			firstReminder = originalSecondReminder;
		}

		Date now = new Date();

		if (Validator.isNull(recurrence)) {
			validate(titleMap, startTimeJCalendar, endTimeJCalendar);
		}
		else {
			java.util.Calendar untilJCalendar =
				RecurrenceSerializer.deserialize(recurrence).getUntilJCalendar();

			validate(
				titleMap, startTimeJCalendar, endTimeJCalendar, untilJCalendar);
		}

		CalendarBooking calendarBooking = calendarBookingPersistence.create(
			calendarBookingId);

		calendarBooking.setGroupId(calendar.getGroupId());
		calendarBooking.setCompanyId(user.getCompanyId());
		calendarBooking.setUserId(user.getUserId());
		calendarBooking.setUserName(user.getFullName());
		calendarBooking.setCreateDate(serviceContext.getCreateDate(now));
		calendarBooking.setModifiedDate(serviceContext.getModifiedDate(now));
		calendarBooking.setCalendarId(calendarId);
		calendarBooking.setCalendarResourceId(calendar.getCalendarResourceId());

		if (parentCalendarBookingId > 0) {
			calendarBooking.setParentCalendarBookingId(parentCalendarBookingId);
		}
		else {
			calendarBooking.setParentCalendarBookingId(calendarBookingId);
		}

		String vEventUid = (String)serviceContext.getAttribute("vEventUid");

		if (vEventUid == null) {
			vEventUid = PortalUUIDUtil.generate();
		}

		calendarBooking.setVEventUid(vEventUid);
		calendarBooking.setTitleMap(titleMap);
		calendarBooking.setDescriptionMap(descriptionMap);
		calendarBooking.setLocation(location);
		calendarBooking.setStartTime(startTimeJCalendar.getTimeInMillis());
		calendarBooking.setEndTime(endTimeJCalendar.getTimeInMillis());
		calendarBooking.setAllDay(allDay);
		calendarBooking.setRecurrence(recurrence);
		calendarBooking.setFirstReminder(firstReminder);
		calendarBooking.setFirstReminderType(firstReminderType);
		calendarBooking.setSecondReminder(secondReminder);
		calendarBooking.setSecondReminderType(secondReminderType);
		calendarBooking.setExpandoBridgeAttributes(serviceContext);
		calendarBooking.setStatus(
			CalendarBookingWorkflowConstants.STATUS_PENDING);
		calendarBooking.setStatusDate(serviceContext.getModifiedDate(now));

		calendarBookingPersistence.update(calendarBooking);

		addChildCalendarBookings(
			calendarBooking, childCalendarIds, serviceContext);

		// Resources

		resourceLocalService.addModelResources(calendarBooking, serviceContext);

		// Asset

		updateAsset(
			userId, calendarBooking, serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames(),
			serviceContext.getAssetLinkEntryIds());

		// Social

		socialActivityLocalService.addActivity(
			userId, calendarBooking.getGroupId(),
			CalendarBooking.class.getName(), calendarBookingId,
			CalendarActivityKeys.ADD_CALENDAR_BOOKING,
			getExtraDataJSON(calendarBooking), 0);

		// Notifications

		sendNotification(
			calendarBooking, NotificationTemplateType.INVITE, serviceContext);

		// Workflow

		calendarBookingApprovalWorkflow.startWorkflow(
			userId, calendarBooking, serviceContext);

		return calendarBooking;
	}

	@Override
	public void checkCalendarBookings()
		throws PortalException, SystemException {

		Date now = new Date();

		List<CalendarBooking> calendarBookings =
			calendarBookingFinder.findByFutureReminders(now.getTime());

		long endTime = now.getTime() + Time.MONTH;

		calendarBookings = RecurrenceUtil.expandCalendarBookings(
			calendarBookings, now.getTime(), endTime, 1);

		for (CalendarBooking calendarBooking : calendarBookings) {
			try {
				Company company = companyPersistence.findByPrimaryKey(
					calendarBooking.getCompanyId());

				if (company.isActive()) {
					NotificationUtil.notifyCalendarBookingReminders(
						calendarBooking, now.getTime());
				}
			}
			catch (PortalException pe) {
				throw pe;
			}
			catch (SystemException se) {
				throw se;
			}
			catch (Exception e) {
				throw new SystemException(e);
			}
		}
	}

	@Indexable(type = IndexableType.DELETE)
	@Override
	@SystemEvent(
		action = SystemEventConstants.ACTION_SKIP, send = false,
		type = SystemEventConstants.TYPE_DELETE)
	public CalendarBooking deleteCalendarBooking(
			CalendarBooking calendarBooking)
		throws PortalException, SystemException {

		// Calendar booking

		calendarBookingPersistence.remove(calendarBooking);

		// Calendar bookings

		List<CalendarBooking> childCalendarBookings = getChildCalendarBookings(
			calendarBooking.getCalendarBookingId());

		for (CalendarBooking childCalendarBooking : childCalendarBookings) {
			calendarBookingLocalService.deleteCalendarBooking(
				childCalendarBooking);
		}

		// Resources

		resourceLocalService.deleteResource(
			calendarBooking, ResourceConstants.SCOPE_INDIVIDUAL);

		// Subscriptions

		subscriptionLocalService.deleteSubscriptions(
			calendarBooking.getCompanyId(), CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		// Asset

		assetEntryLocalService.deleteEntry(
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		// Message boards

		mbMessageLocalService.deleteDiscussionMessages(
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		// Ratings

		ratingsStatsLocalService.deleteStats(
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		// Trash

		trashEntryLocalService.deleteEntry(
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		return calendarBooking;
	}

	@Override
	public CalendarBooking deleteCalendarBooking(long calendarBookingId)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		calendarBookingLocalService.deleteCalendarBooking(calendarBooking);

		return calendarBooking;
	}

	@Override
	public void deleteCalendarBookingInstance(
			CalendarBooking calendarBooking, int instanceIndex,
			boolean allFollowing)
		throws PortalException, SystemException {

		CalendarBooking calendarBookingInstance =
			RecurrenceUtil.getCalendarBookingInstance(
				calendarBooking, instanceIndex);

		deleteCalendarBookingInstance(
			calendarBooking, calendarBookingInstance.getStartTime(),
			allFollowing);
	}

	@Override
	public void deleteCalendarBookingInstance(
			CalendarBooking calendarBooking, long startTime,
			boolean allFollowing)
		throws PortalException, SystemException {

		Date now = new Date();

		java.util.Calendar startTimeJCalendar = JCalendarUtil.getJCalendar(
			startTime);

		Recurrence recurrenceObj = calendarBooking.getRecurrenceObj();

		if (allFollowing) {
			if (startTime == calendarBooking.getStartTime()) {
				calendarBookingLocalService.deleteCalendarBooking(
					calendarBooking);

				return;
			}

			if (recurrenceObj.getCount() > 0) {
				recurrenceObj.setCount(0);
			}

			startTimeJCalendar.add(java.util.Calendar.DATE, -1);

			recurrenceObj.setUntilJCalendar(startTimeJCalendar);
		}
		else {
			recurrenceObj.addExceptionDate(startTimeJCalendar);
		}

		String recurrence = RecurrenceSerializer.serialize(recurrenceObj);

		updateChildCalendarBookings(calendarBooking, now, recurrence);
	}

	@Override
	public void deleteCalendarBookingInstance(
			long calendarBookingId, long startTime, boolean allFollowing)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		deleteCalendarBookingInstance(calendarBooking, startTime, allFollowing);
	}

	@Override
	public void deleteCalendarBookings(long calendarId)
		throws PortalException, SystemException {

		List<CalendarBooking> calendarBookings =
			calendarBookingPersistence.findByCalendarId(calendarId);

		for (CalendarBooking calendarBooking : calendarBookings) {
			calendarBookingLocalService.deleteCalendarBooking(calendarBooking);
		}
	}

	@Override
	public String exportCalendarBooking(long calendarBookingId, String type)
		throws Exception {

		CalendarDataFormat calendarDataFormat = CalendarDataFormat.parse(type);

		CalendarDataHandler calendarDataHandler =
			CalendarDataHandlerFactory.getCalendarDataHandler(
				calendarDataFormat);

		return calendarDataHandler.exportCalendarBooking(calendarBookingId);
	}

	@Override
	public CalendarBooking fetchCalendarBooking(
			long calendarId, String vEventUid)
		throws SystemException {

		return calendarBookingPersistence.fetchByC_V(calendarId, vEventUid);
	}

	@Override
	public CalendarBooking fetchCalendarBooking(String uuid, long groupId)
		throws SystemException {

		return calendarBookingPersistence.fetchByUUID_G(uuid, groupId);
	}

	@Override
	public CalendarBooking getCalendarBooking(long calendarBookingId)
		throws PortalException, SystemException {

		return calendarBookingPersistence.findByPrimaryKey(calendarBookingId);
	}

	@Override
	public CalendarBooking getCalendarBooking(
			long calendarId, long parentCalendarBookingId)
		throws PortalException, SystemException {

		return calendarBookingPersistence.findByC_P(
			calendarId, parentCalendarBookingId);
	}

	@Override
	public CalendarBooking getCalendarBookingInstance(
			long calendarBookingId, int instanceIndex)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking = getCalendarBooking(calendarBookingId);

		return RecurrenceUtil.getCalendarBookingInstance(
			calendarBooking, instanceIndex);
	}

	@Override
	public List<CalendarBooking> getCalendarBookings(long calendarId)
		throws SystemException {

		return calendarBookingPersistence.findByCalendarId(calendarId);
	}

	@Override
	public List<CalendarBooking> getCalendarBookings(
			long calendarId, int[] statuses)
		throws SystemException {

		return calendarBookingPersistence.findByC_S(calendarId, statuses);
	}

	@Override
	public List<CalendarBooking> getCalendarBookings(
			long calendarId, long startTime, long endTime)
		throws SystemException {

		return getCalendarBookings(
			calendarId, startTime, endTime, QueryUtil.ALL_POS);
	}

	@Override
	public List<CalendarBooking> getCalendarBookings(
			long calendarId, long startTime, long endTime, int max)
		throws SystemException {

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			CalendarBooking.class, getClassLoader());

		Property property = PropertyFactoryUtil.forName("calendarId");

		dynamicQuery.add(property.eq(calendarId));

		if (startTime >= 0) {
			Property propertyStartTime = PropertyFactoryUtil.forName(
				"startTime");

			dynamicQuery.add(propertyStartTime.gt(startTime));
		}

		if (endTime >= 0) {
			Property propertyEndTime = PropertyFactoryUtil.forName("endTime");

			dynamicQuery.add(propertyEndTime.lt(endTime));
		}

		if (max > 0) {
			dynamicQuery.setLimit(0, max);
		}

		return dynamicQuery(dynamicQuery);
	}

	@Override
	public int getCalendarBookingsCount(
			long calendarId, long parentCalendarBookingId)
		throws SystemException {

		return calendarBookingPersistence.countByC_P(
			calendarId, parentCalendarBookingId);
	}

	@Override
	public List<CalendarBooking> getChildCalendarBookings(
			long calendarBookingId)
		throws SystemException {

		return calendarBookingPersistence.findByParentCalendarBookingId(
			calendarBookingId);
	}

	@Override
	public List<CalendarBooking> getChildCalendarBookings(
			long parentCalendarBookingId, int status)
		throws SystemException {

		return calendarBookingPersistence.findByP_S(
			parentCalendarBookingId, status);
	}

	@Override
	public long[] getChildCalendarIds(long calendarBookingId, long calendarId)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		List<CalendarBooking> childCalendarBookings =
			calendarBookingPersistence.findByParentCalendarBookingId(
				calendarBookingId);

		long[] childCalendarIds = new long[childCalendarBookings.size()];

		for (int i = 0; i < childCalendarIds.length; i++) {
			CalendarBooking childCalendarBooking = childCalendarBookings.get(i);

			if (childCalendarBooking.getCalendarId() ==
					calendarBooking.getCalendarId()) {

				childCalendarIds[i] = calendarId;
			}
			else {
				childCalendarIds[i] = childCalendarBooking.getCalendarId();
			}
		}

		return childCalendarIds;
	}

	@Override
	public CalendarBooking moveCalendarBookingToTrash(
			long userId, CalendarBooking calendarBooking)
		throws PortalException, SystemException {

		if (!calendarBooking.isMasterBooking()) {
			return calendarBooking;
		}

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setUserId(userId);

		calendarBookingLocalService.updateStatus(
			userId, calendarBooking,
			CalendarBookingWorkflowConstants.STATUS_IN_TRASH, serviceContext);

		socialActivityCounterLocalService.disableActivityCounters(
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId());

		socialActivityLocalService.addActivity(
			userId, calendarBooking.getGroupId(),
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId(),
			SocialActivityConstants.TYPE_MOVE_TO_TRASH,
			getExtraDataJSON(calendarBooking), 0);

		return calendarBooking;
	}

	@Override
	public CalendarBooking moveCalendarBookingToTrash(
			long userId, long calendarBookingId)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		return moveCalendarBookingToTrash(userId, calendarBooking);
	}

	@Override
	public CalendarBooking restoreCalendarBookingFromTrash(
			long userId, long calendarBookingId)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking = getCalendarBooking(calendarBookingId);

		if (!calendarBooking.isMasterBooking()) {
			return calendarBooking;
		}

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setUserId(userId);

		TrashEntry trashEntry = trashEntryLocalService.getEntry(
			CalendarBooking.class.getName(), calendarBookingId);

		calendarBookingLocalService.updateStatus(
			userId, calendarBookingId, trashEntry.getStatus(), serviceContext);

		socialActivityCounterLocalService.enableActivityCounters(
			CalendarBooking.class.getName(), calendarBookingId);

		socialActivityLocalService.addActivity(
			userId, calendarBooking.getGroupId(),
			CalendarBooking.class.getName(), calendarBookingId,
			SocialActivityConstants.TYPE_RESTORE_FROM_TRASH,
			getExtraDataJSON(calendarBooking), 0);

		return calendarBooking;
	}

	@Override
	public List<CalendarBooking> search(
			long companyId, long[] groupIds, long[] calendarIds,
			long[] calendarResourceIds, long parentCalendarBookingId,
			String keywords, long startTime, long endTime, boolean recurring,
			int[] statuses, int start, int end,
			OrderByComparator orderByComparator)
		throws SystemException {

		List<CalendarBooking> calendarBookings =
			calendarBookingFinder.findByKeywords(
				companyId, groupIds, calendarIds, calendarResourceIds,
				parentCalendarBookingId, keywords, startTime, endTime,
				recurring, statuses, start, end, orderByComparator);

		if (recurring) {
			calendarBookings = RecurrenceUtil.expandCalendarBookings(
				calendarBookings, startTime, endTime);
		}

		return calendarBookings;
	}

	@Override
	public List<CalendarBooking> search(
			long companyId, long[] groupIds, long[] calendarIds,
			long[] calendarResourceIds, long parentCalendarBookingId,
			String title, String description, String location, long startTime,
			long endTime, boolean recurring, int[] statuses,
			boolean andOperator, int start, int end,
			OrderByComparator orderByComparator)
		throws SystemException {

		List<CalendarBooking> calendarBookings =
			calendarBookingFinder.findByC_G_C_C_P_T_D_L_S_E_S(
				companyId, groupIds, calendarIds, calendarResourceIds,
				parentCalendarBookingId, title, description, location,
				startTime, endTime, recurring, statuses, andOperator, start,
				end, orderByComparator);

		if (recurring) {
			calendarBookings = RecurrenceUtil.expandCalendarBookings(
				calendarBookings, startTime, endTime);
		}

		return calendarBookings;
	}

	@Override
	public int searchCount(
			long companyId, long[] groupIds, long[] calendarIds,
			long[] calendarResourceIds, long parentCalendarBookingId,
			String keywords, long startTime, long endTime, int[] statuses)
		throws SystemException {

		return calendarBookingFinder.countByKeywords(
			companyId, groupIds, calendarIds, calendarResourceIds,
			parentCalendarBookingId, keywords, startTime, endTime, statuses);
	}

	@Override
	public int searchCount(
			long companyId, long[] groupIds, long[] calendarIds,
			long[] calendarResourceIds, long parentCalendarBookingId,
			String title, String description, String location, long startTime,
			long endTime, int[] statuses, boolean andOperator)
		throws SystemException {

		return calendarBookingFinder.countByC_G_C_C_P_T_D_L_S_E_S(
			companyId, groupIds, calendarIds, calendarResourceIds,
			parentCalendarBookingId, title, description, location, startTime,
			endTime, statuses, andOperator);
	}

	@Override
	public void updateAsset(
			long userId, CalendarBooking calendarBooking,
			long[] assetCategoryIds, String[] assetTagNames,
			long[] assetLinkEntryIds)
		throws PortalException, SystemException {

		boolean visible = false;

		if (calendarBooking.isApproved()) {
			visible = true;
		}

		String summary = HtmlUtil.extractText(
			StringUtil.shorten(calendarBooking.getDescription(), 500));

		AssetEntry assetEntry = assetEntryLocalService.updateEntry(
			userId, calendarBooking.getGroupId(),
			calendarBooking.getCreateDate(), calendarBooking.getModifiedDate(),
			CalendarBooking.class.getName(),
			calendarBooking.getCalendarBookingId(), calendarBooking.getUuid(),
			0, assetCategoryIds, assetTagNames, visible, null, null, null,
			ContentTypes.TEXT_HTML, calendarBooking.getTitle(),
			calendarBooking.getDescription(), summary, null, null, 0, 0, null,
			false);

		assetLinkLocalService.updateLinks(
			userId, assetEntry.getEntryId(), assetLinkEntryIds,
			AssetLinkConstants.TYPE_RELATED);
	}

	@Override
	public CalendarBooking updateCalendarBooking(
			long userId, long calendarBookingId, long calendarId,
			long[] childCalendarIds, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, String location, long startTime,
			long endTime, boolean allDay, String recurrence, long firstReminder,
			String firstReminderType, long secondReminder,
			String secondReminderType, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		// Calendar booking

		Calendar calendar = calendarPersistence.findByPrimaryKey(calendarId);
		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		for (Locale locale : descriptionMap.keySet()) {
			String sanitizedDescription = SanitizerUtil.sanitize(
				calendar.getCompanyId(), calendar.getGroupId(), userId,
				CalendarBooking.class.getName(), calendarBookingId,
				ContentTypes.TEXT_HTML, Sanitizer.MODE_ALL,
				descriptionMap.get(locale), null);

			descriptionMap.put(locale, sanitizedDescription);
		}

		java.util.Calendar startTimeJCalendar = JCalendarUtil.getJCalendar(
			startTime);
		java.util.Calendar endTimeJCalendar = JCalendarUtil.getJCalendar(
			endTime);

		if (allDay) {
			startTimeJCalendar = JCalendarUtil.toMidnightJCalendar(
				startTimeJCalendar);
			endTimeJCalendar = JCalendarUtil.toLastHourJCalendar(
				endTimeJCalendar);
		}

		if (firstReminder < secondReminder) {
			long originalSecondReminder = secondReminder;

			secondReminder = firstReminder;
			firstReminder = originalSecondReminder;
		}

		if (Validator.isNull(recurrence)) {
			validate(titleMap, startTimeJCalendar, endTimeJCalendar);
		}
		else {
			java.util.Calendar untilJCalendar =
				RecurrenceSerializer.deserialize(recurrence).getUntilJCalendar();

			validate(
				titleMap, startTimeJCalendar, endTimeJCalendar, untilJCalendar);
		}

		calendarBooking.setGroupId(calendar.getGroupId());
		calendarBooking.setModifiedDate(serviceContext.getModifiedDate(null));
		calendarBooking.setCalendarId(calendarId);
		calendarBooking.setTitleMap(titleMap);
		calendarBooking.setDescriptionMap(descriptionMap);
		calendarBooking.setLocation(location);
		calendarBooking.setStartTime(startTimeJCalendar.getTimeInMillis());
		calendarBooking.setEndTime(endTimeJCalendar.getTimeInMillis());
		calendarBooking.setAllDay(allDay);
		calendarBooking.setRecurrence(recurrence);
		calendarBooking.setFirstReminder(firstReminder);
		calendarBooking.setFirstReminderType(firstReminderType);
		calendarBooking.setSecondReminder(secondReminder);
		calendarBooking.setSecondReminderType(secondReminderType);
		calendarBooking.setExpandoBridgeAttributes(serviceContext);

		calendarBookingPersistence.update(calendarBooking);

		addChildCalendarBookings(
			calendarBooking, childCalendarIds, serviceContext);

		// Asset

		updateAsset(
			userId, calendarBooking, serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames(),
			serviceContext.getAssetLinkEntryIds());

		// Social

		socialActivityLocalService.addActivity(
			userId, calendarBooking.getGroupId(),
			CalendarBooking.class.getName(), calendarBookingId,
			CalendarActivityKeys.UPDATE_CALENDAR_BOOKING,
			getExtraDataJSON(calendarBooking), 0);

		// Workflow

		calendarBookingApprovalWorkflow.invokeTransition(
			userId, calendarBooking, status, serviceContext);

		// Notifications

		sendNotification(
			calendarBooking, NotificationTemplateType.UPDATE, serviceContext);

		return calendarBooking;
	}

	@Override
	public CalendarBooking updateCalendarBooking(
			long userId, long calendarBookingId, long calendarId,
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			String location, long startTime, long endTime, boolean allDay,
			String recurrence, long firstReminder, String firstReminderType,
			long secondReminder, String secondReminderType, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		long[] childCalendarIds = getChildCalendarIds(
			calendarBookingId, calendarId);

		return updateCalendarBooking(
			userId, calendarBookingId, calendarId, childCalendarIds, titleMap,
			descriptionMap, location, startTime, endTime, allDay, recurrence,
			firstReminder, firstReminderType, secondReminder,
			secondReminderType, status, serviceContext);
	}

	@Override
	public CalendarBooking updateCalendarBookingInstance(
			long userId, long calendarBookingId, int instanceIndex,
			long calendarId, long[] childCalendarIds,
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			String location, long startTime, long endTime, boolean allDay,
			String recurrence, boolean allFollowing, long firstReminder,
			String firstReminderType, long secondReminder,
			String secondReminderType, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		if ((instanceIndex == 0) && allFollowing) {
			return updateCalendarBooking(
				userId, calendarBookingId, calendarId, childCalendarIds,
				titleMap, descriptionMap, location, startTime, endTime, allDay,
				recurrence, firstReminder, firstReminderType, secondReminder,
				secondReminderType, status, serviceContext);
		}

		String oldRecurrence = calendarBooking.getRecurrence();

		deleteCalendarBookingInstance(
			calendarBooking, instanceIndex, allFollowing);

		if (allFollowing) {
			Recurrence recurrenceObj = RecurrenceSerializer.deserialize(
				recurrence);

			if (oldRecurrence.equals(recurrence) &&
				(recurrenceObj.getCount() > 0)) {

				recurrenceObj.setCount(
					recurrenceObj.getCount() - instanceIndex);

				recurrence = RecurrenceSerializer.serialize(recurrenceObj);
			}
		}
		else {
			recurrence = StringPool.BLANK;
		}

		return addCalendarBooking(
			userId, calendarId, childCalendarIds,
			CalendarBookingConstants.PARENT_CALENDAR_BOOKING_ID_DEFAULT,
			titleMap, descriptionMap, location, startTime, endTime, allDay,
			recurrence, firstReminder, firstReminderType, secondReminder,
			secondReminderType, serviceContext);
	}

	@Override
	public CalendarBooking updateCalendarBookingInstance(
			long userId, long calendarBookingId, int instanceIndex,
			long calendarId, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, String location, long startTime,
			long endTime, boolean allDay, String recurrence,
			boolean allFollowing, long firstReminder, String firstReminderType,
			long secondReminder, String secondReminderType, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		long[] childCalendarIds = getChildCalendarIds(
			calendarBookingId, calendarId);

		return updateCalendarBookingInstance(
			userId, calendarBookingId, instanceIndex, calendarId,
			childCalendarIds, titleMap, descriptionMap, location, startTime,
			endTime, allDay, recurrence, allFollowing, firstReminder,
			firstReminderType, secondReminder, secondReminderType, status,
			serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CalendarBooking updateStatus(
			long userId, CalendarBooking calendarBooking, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		// Calendar booking

		User user = userPersistence.findByPrimaryKey(userId);
		Date now = new Date();

		int oldStatus = calendarBooking.getStatus();

		calendarBooking.setModifiedDate(serviceContext.getModifiedDate(now));
		calendarBooking.setStatus(status);
		calendarBooking.setStatusByUserId(user.getUserId());
		calendarBooking.setStatusByUserName(user.getFullName());
		calendarBooking.setStatusDate(serviceContext.getModifiedDate(now));

		calendarBookingPersistence.update(calendarBooking);

		// Child calendar bookings

		if (status == CalendarBookingWorkflowConstants.STATUS_IN_TRASH) {
			List<CalendarBooking> childCalendarBookings =
				calendarBooking.getChildCalendarBookings();

			for (CalendarBooking childCalendarBooking : childCalendarBookings) {
				if (childCalendarBooking.equals(calendarBooking)) {
					continue;
				}

				updateStatus(
					userId, childCalendarBooking,
					CalendarBookingWorkflowConstants.STATUS_IN_TRASH,
					serviceContext);
			}
		}
		else if (oldStatus ==
					CalendarBookingWorkflowConstants.STATUS_IN_TRASH) {

			List<CalendarBooking> childCalendarBookings =
				calendarBooking.getChildCalendarBookings();

			for (CalendarBooking childCalendarBooking : childCalendarBookings) {
				if (childCalendarBooking.equals(calendarBooking)) {
					continue;
				}

				updateStatus(
					userId, childCalendarBooking,
					CalendarBookingWorkflowConstants.STATUS_PENDING,
					serviceContext);
			}
		}

		// Asset

		if (status == CalendarBookingWorkflowConstants.STATUS_APPROVED) {
			assetEntryLocalService.updateVisible(
				CalendarBooking.class.getName(),
				calendarBooking.getCalendarBookingId(), true);
		}
		else if (status == CalendarBookingWorkflowConstants.STATUS_IN_TRASH) {
			assetEntryLocalService.updateVisible(
				CalendarBooking.class.getName(),
				calendarBooking.getCalendarBookingId(), false);
		}

		// Trash

		if (oldStatus == WorkflowConstants.STATUS_IN_TRASH) {
			trashEntryLocalService.deleteEntry(
				CalendarBooking.class.getName(),
				calendarBooking.getCalendarBookingId());
		}

		if (status == CalendarBookingWorkflowConstants.STATUS_IN_TRASH) {
			if (calendarBooking.isMasterBooking()) {
				trashEntryLocalService.addTrashEntry(
					userId, calendarBooking.getGroupId(),
					CalendarBooking.class.getName(),
					calendarBooking.getCalendarBookingId(),
					calendarBooking.getUuid(), null, oldStatus, null, null);
			}
			else {
				trashEntryLocalService.addTrashEntry(
					userId, calendarBooking.getGroupId(),
					CalendarBooking.class.getName(),
					calendarBooking.getCalendarBookingId(),
					calendarBooking.getUuid(), null,
					CalendarBookingWorkflowConstants.STATUS_PENDING, null,
					null);
			}

			sendNotification(
				calendarBooking, NotificationTemplateType.MOVED_TO_TRASH,
				serviceContext);
		}

		return calendarBooking;
	}

	@Override
	public CalendarBooking updateStatus(
			long userId, long calendarBookingId, int status,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		CalendarBooking calendarBooking =
			calendarBookingPersistence.findByPrimaryKey(calendarBookingId);

		return calendarBookingLocalService.updateStatus(
			userId, calendarBooking, status, serviceContext);
	}

	protected void addChildCalendarBookings(
			CalendarBooking calendarBooking, long[] childCalendarIds,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		if (!calendarBooking.isMasterBooking()) {
			return;
		}

		List<CalendarBooking> childCalendarBookings =
			calendarBookingPersistence.findByParentCalendarBookingId(
				calendarBooking.getCalendarBookingId());

		Set<Long> existingCalendarBookingIds = new HashSet<Long>(
			childCalendarIds.length);

		for (CalendarBooking childCalendarBooking : childCalendarBookings) {
			if (childCalendarBooking.isMasterBooking() ||
				(childCalendarBooking.isDenied() &&
				 ArrayUtil.contains(
					 childCalendarIds, childCalendarBooking.getCalendarId()))) {

				continue;
			}

			deleteCalendarBooking(childCalendarBooking.getCalendarBookingId());

			existingCalendarBookingIds.add(
				childCalendarBooking.getCalendarId());
		}

		for (long calendarId : childCalendarIds) {
			int count = calendarBookingPersistence.countByC_P(
				calendarId, calendarBooking.getCalendarBookingId());

			if (count > 0) {
				continue;
			}

			serviceContext.setAttribute("sendNotification", false);

			CalendarBooking childCalendarBooking = addCalendarBooking(
				calendarBooking.getUserId(), calendarId, new long[0],
				calendarBooking.getCalendarBookingId(),
				calendarBooking.getTitleMap(),
				calendarBooking.getDescriptionMap(),
				calendarBooking.getLocation(), calendarBooking.getStartTime(),
				calendarBooking.getEndTime(), calendarBooking.getAllDay(),
				calendarBooking.getRecurrence(),
				calendarBooking.getFirstReminder(),
				calendarBooking.getFirstReminderType(),
				calendarBooking.getSecondReminder(),
				calendarBooking.getSecondReminderType(), serviceContext);

			serviceContext.setAttribute("sendNotification", true);

			NotificationTemplateType notificationTemplateType =
				NotificationTemplateType.INVITE;

			if (existingCalendarBookingIds.contains(
					childCalendarBooking.getCalendarId())) {

				notificationTemplateType = NotificationTemplateType.UPDATE;
			}

			sendNotification(
				childCalendarBooking, notificationTemplateType, serviceContext);
		}
	}

	protected String getExtraDataJSON(CalendarBooking calendarBooking) {
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("title", calendarBooking.getTitle());

		return jsonObject.toString();
	}

	protected void sendNotification(
		CalendarBooking calendarBooking,
		NotificationTemplateType notificationTemplateType,
		ServiceContext serviceContext) {

		boolean sendNotification = ParamUtil.getBoolean(
			serviceContext, "sendNotification", true);

		if (!sendNotification) {
			return;
		}

		try {
			User sender = userLocalService.fetchUser(
				serviceContext.getUserId());

			NotificationType notificationType = NotificationType.parse(
				PortletPropsValues.CALENDAR_NOTIFICATION_DEFAULT_TYPE);

			NotificationUtil.notifyCalendarBookingRecipients(
				calendarBooking, notificationType, notificationTemplateType,
				sender);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(e, e);
			}
		}
	}

	protected void updateChildCalendarBookings(
			CalendarBooking calendarBooking, Date modifiedDate,
			String recurrence)
		throws SystemException {

		List<CalendarBooking> childCalendarBookings =
			new ArrayList<CalendarBooking>();

		if (calendarBooking.isMasterBooking()) {
			childCalendarBookings = getChildCalendarBookings(
				calendarBooking.getCalendarBookingId());
		}
		else {
			childCalendarBookings.add(calendarBooking);
		}

		for (CalendarBooking childCalendarBooking : childCalendarBookings) {
			childCalendarBooking.setModifiedDate(modifiedDate);
			childCalendarBooking.setRecurrence(recurrence);

			calendarBookingPersistence.update(childCalendarBooking);
		}
	}

	protected void validate(
			Map<Locale, String> titleMap, java.util.Calendar startTimeJCalendar,
			java.util.Calendar endTimeJCalendar)
		throws PortalException {

		validate(titleMap, startTimeJCalendar, endTimeJCalendar, null);
	}

	protected void validate(
			Map<Locale, String> titleMap, java.util.Calendar startTimeJCalendar,
			java.util.Calendar endTimeJCalendar,
			java.util.Calendar untilJCalendar)
		throws PortalException {

		if (Validator.isNull(titleMap) || titleMap.isEmpty()) {
			throw new CalendarBookingTitleException();
		}

		if (startTimeJCalendar.after(endTimeJCalendar)) {
			throw new CalendarBookingDurationException();
		}

		if (Validator.isNotNull(untilJCalendar) &&
			startTimeJCalendar.after(untilJCalendar)) {
				throw new CalendarBookingRecurrenceException();
		}
	}

	@BeanReference(type = CalendarBookingApprovalWorkflow.class)
	protected CalendarBookingApprovalWorkflow calendarBookingApprovalWorkflow;

	private static Log _log = LogFactoryUtil.getLog(
		CalendarBookingLocalServiceImpl.class);

}