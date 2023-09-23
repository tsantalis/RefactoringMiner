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

package com.liferay.calendar.portlet;

import com.liferay.calendar.CalendarBookingDurationException;
import com.liferay.calendar.CalendarBookingRecurrenceException;
import com.liferay.calendar.CalendarNameException;
import com.liferay.calendar.CalendarResourceCodeException;
import com.liferay.calendar.CalendarResourceNameException;
import com.liferay.calendar.DuplicateCalendarResourceException;
import com.liferay.calendar.NoSuchResourceException;
import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.calendar.model.CalendarBookingConstants;
import com.liferay.calendar.model.CalendarNotificationTemplate;
import com.liferay.calendar.model.CalendarNotificationTemplateConstants;
import com.liferay.calendar.model.CalendarResource;
import com.liferay.calendar.notification.NotificationTemplateContextFactory;
import com.liferay.calendar.notification.NotificationTemplateType;
import com.liferay.calendar.notification.NotificationType;
import com.liferay.calendar.recurrence.Frequency;
import com.liferay.calendar.recurrence.PositionalWeekday;
import com.liferay.calendar.recurrence.Recurrence;
import com.liferay.calendar.recurrence.RecurrenceSerializer;
import com.liferay.calendar.recurrence.Weekday;
import com.liferay.calendar.service.CalendarBookingLocalServiceUtil;
import com.liferay.calendar.service.CalendarBookingServiceUtil;
import com.liferay.calendar.service.CalendarLocalServiceUtil;
import com.liferay.calendar.service.CalendarNotificationTemplateServiceUtil;
import com.liferay.calendar.service.CalendarResourceServiceUtil;
import com.liferay.calendar.service.CalendarServiceUtil;
import com.liferay.calendar.service.permission.CalendarPermission;
import com.liferay.calendar.util.ActionKeys;
import com.liferay.calendar.util.CalendarDataFormat;
import com.liferay.calendar.util.CalendarDataHandler;
import com.liferay.calendar.util.CalendarDataHandlerFactory;
import com.liferay.calendar.util.CalendarResourceUtil;
import com.liferay.calendar.util.CalendarSearcher;
import com.liferay.calendar.util.CalendarUtil;
import com.liferay.calendar.util.JCalendarUtil;
import com.liferay.calendar.util.PortletKeys;
import com.liferay.calendar.util.RSSUtil;
import com.liferay.calendar.util.WebKeys;
import com.liferay.calendar.workflow.CalendarBookingWorkflowConstants;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.service.SubscriptionLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.comparator.UserFirstNameComparator;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.service.MBMessageServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.liferay.util.dao.orm.CustomSQLUtil;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * @author Eduardo Lundgren
 * @author Fabio Pezzutto
 * @author Andrea Di Giorgi
 * @author Marcellus Tavares
 * @author Bruno Basto
 * @author Pier Paolo Ramon
 */
public class CalendarPortlet extends MVCPortlet {

	public void deleteCalendar(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarId = ParamUtil.getLong(actionRequest, "calendarId");

		CalendarServiceUtil.deleteCalendar(calendarId);
	}

	public void deleteCalendarResource(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarResourceId = ParamUtil.getLong(
			actionRequest, "calendarResourceId");

		CalendarResourceServiceUtil.deleteCalendarResource(calendarResourceId);
	}

	@Override
	public void init() throws PortletException {
		super.init();

		NotificationTemplateContextFactory.setPortletConfig(getPortletConfig());
	}

	public void invokeTransition(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarBookingId = ParamUtil.getLong(
			actionRequest, "calendarBookingId");

		int status = ParamUtil.getInteger(actionRequest, "status");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			CalendarBooking.class.getName(), actionRequest);

		CalendarBookingServiceUtil.invokeTransition(
			calendarBookingId, status, serviceContext);
	}

	public void moveCalendarBookingToTrash(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarBookingId = ParamUtil.getLong(
			actionRequest, "calendarBookingId");

		CalendarBookingServiceUtil.moveCalendarBookingToTrash(
			calendarBookingId);
	}

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		try {
			getCalendar(renderRequest);
			getCalendarBooking(renderRequest);
			getCalendarResource(renderRequest);
		}
		catch (Exception e) {
			if (e instanceof NoSuchResourceException ||
				e instanceof PrincipalException) {

				SessionErrors.add(renderRequest, e.getClass());
			}
			else {
				throw new PortletException(e);
			}
		}

		super.render(renderRequest, renderResponse);
	}

	@Override
	public void serveResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws PortletException {

		try {
			String resourceID = resourceRequest.getResourceID();

			if (resourceID.equals("calendarBookingInvitees")) {
				serveCalendarBookingInvitees(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("calendarBookings")) {
				serveCalendarBookings(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("calendarBookingsRSS")) {
				serveCalendarBookingsRSS(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("calendarRenderingRules")) {
				serveCalendarRenderingRules(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("calendarResources")) {
				serveCalendarResources(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("exportCalendar")) {
				serveExportCalendar(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("importCalendar")) {
				serveImportCalendar(resourceRequest, resourceResponse);
			}
			else if (resourceID.equals("resourceCalendars")) {
				serveResourceCalendars(resourceRequest, resourceResponse);
			}
			else {
				super.serveResource(resourceRequest, resourceResponse);
			}
		}
		catch (Exception e) {
			throw new PortletException(e);
		}
	}

	public void updateCalendar(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarId = ParamUtil.getLong(actionRequest, "calendarId");

		long calendarResourceId = ParamUtil.getLong(
			actionRequest, "calendarResourceId");
		Map<Locale, String> nameMap = LocalizationUtil.getLocalizationMap(
			actionRequest, "name");
		Map<Locale, String> descriptionMap =
			LocalizationUtil.getLocalizationMap(actionRequest, "description");
		String timeZoneId = ParamUtil.getString(actionRequest, "timeZoneId");
		int color = ParamUtil.getInteger(actionRequest, "color");
		boolean defaultCalendar = ParamUtil.getBoolean(
			actionRequest, "defaultCalendar");
		boolean enableComments = ParamUtil.getBoolean(
			actionRequest, "enableComments");
		boolean enableRatings = ParamUtil.getBoolean(
			actionRequest, "enableRatings");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			Calendar.class.getName(), actionRequest);

		Calendar calendar = null;

		if (calendarId <= 0) {
			CalendarResource calendarResource =
				CalendarResourceServiceUtil.getCalendarResource(
					calendarResourceId);

			calendar = CalendarServiceUtil.addCalendar(
				calendarResource.getGroupId(), calendarResourceId, nameMap,
				descriptionMap, timeZoneId, color, defaultCalendar,
				enableComments, enableRatings, serviceContext);
		}
		else {
			calendar = CalendarServiceUtil.updateCalendar(
				calendarId, nameMap, descriptionMap, timeZoneId, color,
				defaultCalendar, enableComments, enableRatings, serviceContext);
		}

		String redirect = getEditCalendarURL(
			actionRequest, actionResponse, calendar);

		actionRequest.setAttribute(WebKeys.REDIRECT, redirect);
	}

	public void updateCalendarBooking(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarBookingId = ParamUtil.getLong(
			actionRequest, "calendarBookingId");

		long calendarId = ParamUtil.getLong(actionRequest, "calendarId");
		long[] childCalendarIds = ParamUtil.getLongValues(
			actionRequest, "childCalendarIds");
		Map<Locale, String> titleMap = LocalizationUtil.getLocalizationMap(
			actionRequest, "title");
		Map<Locale, String> descriptionMap =
			LocalizationUtil.getLocalizationMap(actionRequest, "description");
		String location = ParamUtil.getString(actionRequest, "location");
		java.util.Calendar startTimeJCalendar = getJCalendar(
			actionRequest, "startTime");
		java.util.Calendar endTimeJCalendar = getJCalendar(
			actionRequest, "endTime");
		boolean allDay = ParamUtil.getBoolean(actionRequest, "allDay");
		String recurrence = getRecurrence(actionRequest);
		long[] reminders = getReminders(actionRequest);
		String[] remindersType = getRemindersType(actionRequest);
		int status = ParamUtil.getInteger(actionRequest, "status");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			CalendarBooking.class.getName(), actionRequest);

		CalendarBooking calendarBooking = null;

		if (calendarBookingId <= 0) {
			calendarBooking = CalendarBookingServiceUtil.addCalendarBooking(
				calendarId, childCalendarIds,
				CalendarBookingConstants.PARENT_CALENDAR_BOOKING_ID_DEFAULT,
				titleMap, descriptionMap, location,
				startTimeJCalendar.getTimeInMillis(),
				endTimeJCalendar.getTimeInMillis(), allDay, recurrence,
				reminders[0], remindersType[0], reminders[1], remindersType[1],
				serviceContext);
		}
		else {
			int instanceIndex = ParamUtil.getInteger(
				actionRequest, "instanceIndex");

			boolean updateCalendarBookingInstance = ParamUtil.getBoolean(
				actionRequest, "updateCalendarBookingInstance");

			if (updateCalendarBookingInstance) {
				calendarBooking =
					CalendarBookingLocalServiceUtil.getCalendarBooking(
						calendarBookingId);

				boolean allFollowing = ParamUtil.getBoolean(
					actionRequest, "allFollowing");

				calendarBooking =
					CalendarBookingServiceUtil.updateCalendarBookingInstance(
						calendarBookingId, instanceIndex, calendarId,
						childCalendarIds, titleMap, descriptionMap, location,
						startTimeJCalendar.getTimeInMillis(),
						endTimeJCalendar.getTimeInMillis(), allDay, recurrence,
						allFollowing, reminders[0], remindersType[0],
						reminders[1], remindersType[1], status, serviceContext);
			}
			else {
				calendarBooking =
					CalendarBookingServiceUtil.getCalendarBookingInstance(
						calendarBookingId, instanceIndex);

				long duration =
					(endTimeJCalendar.getTimeInMillis() -
						startTimeJCalendar.getTimeInMillis());
				long offset =
					(startTimeJCalendar.getTimeInMillis() -
						calendarBooking.getStartTime());

				calendarBooking =
					CalendarBookingServiceUtil.
						getNewStartTimeAndDurationCalendarBooking(
							calendarBookingId, offset, duration);

				calendarBooking =
					CalendarBookingServiceUtil.updateCalendarBooking(
						calendarBookingId, calendarId, childCalendarIds,
						titleMap, descriptionMap, location,
						calendarBooking.getStartTime(),
						calendarBooking.getEndTime(), allDay, recurrence,
						reminders[0], remindersType[0], reminders[1],
						remindersType[1], status, serviceContext);
			}
		}

		actionRequest.setAttribute(WebKeys.CALENDAR_BOOKING, calendarBooking);

		String redirect = getRedirect(actionRequest, actionResponse);

		redirect = HttpUtil.setParameter(
			redirect, actionResponse.getNamespace() + "calendarBookingId",
			calendarBooking.getCalendarBookingId());

		actionRequest.setAttribute(WebKeys.REDIRECT, redirect);
	}

	public void updateCalendarNotificationTemplate(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarNotificationTemplateId = ParamUtil.getLong(
			actionRequest, "calendarNotificationTemplateId");

		long calendarId = ParamUtil.getLong(actionRequest, "calendarId");
		NotificationType notificationType = NotificationType.parse(
			ParamUtil.getString(actionRequest, "notificationType"));
		NotificationTemplateType notificationTemplateType =
			NotificationTemplateType.parse(
				ParamUtil.getString(actionRequest, "notificationTemplateType"));
		String subject = ParamUtil.getString(actionRequest, "subject");
		String body = ParamUtil.getString(actionRequest, "body");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			CalendarNotificationTemplate.class.getName(), actionRequest);

		if (calendarNotificationTemplateId <= 0) {
			CalendarNotificationTemplateServiceUtil.
				addCalendarNotificationTemplate(
					calendarId, notificationType,
					getNotificationTypeSettings(
						actionRequest, notificationType),
					notificationTemplateType, subject, body, serviceContext);
		}
		else {
			CalendarNotificationTemplateServiceUtil.
				updateCalendarNotificationTemplate(
					calendarNotificationTemplateId,
					getNotificationTypeSettings(
						actionRequest, notificationType),
					subject, body, serviceContext);
		}
	}

	public void updateCalendarResource(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		long calendarResourceId = ParamUtil.getLong(
			actionRequest, "calendarResourceId");

		long defaultCalendarId = ParamUtil.getLong(
			actionRequest, "defaultCalendarId");
		String code = ParamUtil.getString(actionRequest, "code");
		Map<Locale, String> nameMap = LocalizationUtil.getLocalizationMap(
			actionRequest, "name");
		Map<Locale, String> descriptionMap =
			LocalizationUtil.getLocalizationMap(actionRequest, "description");
		boolean active = ParamUtil.getBoolean(actionRequest, "active");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			CalendarResource.class.getName(), actionRequest);

		if (calendarResourceId <= 0) {
			CalendarResourceServiceUtil.addCalendarResource(
				serviceContext.getScopeGroupId(),
				PortalUtil.getClassNameId(CalendarResource.class), 0,
				PortalUUIDUtil.generate(), code, nameMap, descriptionMap,
				active, serviceContext);
		}
		else {
			CalendarResourceServiceUtil.updateCalendarResource(
				calendarResourceId, nameMap, descriptionMap, active,
				serviceContext);

			if (defaultCalendarId > 0) {
				CalendarLocalServiceUtil.updateCalendar(
					defaultCalendarId, true);
			}
		}
	}

	public void updateDiscussion(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (cmd.equals(Constants.ADD) || cmd.equals(Constants.UPDATE)) {
			updateMessage(actionRequest);
		}
		else if (cmd.equals(Constants.DELETE)) {
			deleteMessage(actionRequest);
		}
		else if (cmd.equals(Constants.SUBSCRIBE_TO_COMMENTS)) {
			subscribeToComments(actionRequest, true);
		}
		else if (cmd.equals(Constants.UNSUBSCRIBE_FROM_COMMENTS)) {
			subscribeToComments(actionRequest, false);
		}
	}

	protected void addCalendar(
			PortletRequest portletRequest, Set<Calendar> calendarsSet,
			long classNameId, long classPK)
		throws PortalException, SystemException {

		CalendarResource calendarResource =
			CalendarResourceUtil.getCalendarResource(
				portletRequest, classNameId, classPK);

		if (calendarResource == null) {
			return;
		}

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		PermissionChecker permissionChecker =
			themeDisplay.getPermissionChecker();

		List<Calendar> calendars =
			CalendarLocalServiceUtil.getCalendarResourceCalendars(
				calendarResource.getGroupId(),
				calendarResource.getCalendarResourceId());

		for (Calendar calendar : calendars) {
			if (!CalendarPermission.contains(
					permissionChecker, calendar, ActionKeys.VIEW)) {

				continue;
			}

			calendarsSet.add(calendar);
		}
	}

	protected void deleteMessage(ActionRequest actionRequest) throws Exception {
		long groupId = PortalUtil.getScopeGroupId(actionRequest);

		String className = ParamUtil.getString(actionRequest, "className");
		long classPK = ParamUtil.getLong(actionRequest, "classPK");
		String permissionClassName = ParamUtil.getString(
			actionRequest, "permissionClassName");
		long permissionClassPK = ParamUtil.getLong(
			actionRequest, "permissionClassPK");
		long permissionOwnerId = ParamUtil.getLong(
			actionRequest, "permissionOwnerId");

		long messageId = ParamUtil.getLong(actionRequest, "messageId");

		MBMessageServiceUtil.deleteDiscussionMessage(
			groupId, className, classPK, permissionClassName, permissionClassPK,
			permissionOwnerId, messageId);
	}

	@Override
	protected void doDispatch(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		if (SessionErrors.contains(
				renderRequest, NoSuchResourceException.class.getName()) ||
			SessionErrors.contains(
				renderRequest, PrincipalException.class.getName())) {

			include("/error.jsp", renderRequest, renderResponse);
		}
		else {
			super.doDispatch(renderRequest, renderResponse);
		}
	}

	protected void getCalendar(PortletRequest portletRequest) throws Exception {
		long calendarId = ParamUtil.getLong(portletRequest, "calendarId");

		if (calendarId <= 0) {
			return;
		}

		Calendar calendar = CalendarServiceUtil.getCalendar(calendarId);

		portletRequest.setAttribute(WebKeys.CALENDAR, calendar);
	}

	protected void getCalendarBooking(PortletRequest portletRequest)
		throws Exception {

		if (portletRequest.getAttribute(WebKeys.CALENDAR_BOOKING) != null) {
			return;
		}

		long calendarBookingId = ParamUtil.getLong(
			portletRequest, "calendarBookingId");

		if (calendarBookingId <= 0) {
			return;
		}

		CalendarBooking calendarBooking =
			CalendarBookingServiceUtil.getCalendarBooking(calendarBookingId);

		portletRequest.setAttribute(WebKeys.CALENDAR_BOOKING, calendarBooking);
	}

	protected void getCalendarResource(PortletRequest portletRequest)
		throws Exception {

		long calendarResourceId = ParamUtil.getLong(
			portletRequest, "calendarResourceId");

		long classNameId = ParamUtil.getLong(portletRequest, "classNameId");
		long classPK = ParamUtil.getLong(portletRequest, "classPK");

		CalendarResource calendarResource = null;

		if (calendarResourceId > 0) {
			calendarResource = CalendarResourceServiceUtil.getCalendarResource(
				calendarResourceId);
		}
		else if ((classNameId > 0) && (classPK > 0)) {
			calendarResource = CalendarResourceUtil.getCalendarResource(
				portletRequest, classNameId, classPK);
		}

		portletRequest.setAttribute(
			WebKeys.CALENDAR_RESOURCE, calendarResource);
	}

	protected String getEditCalendarURL(
			ActionRequest actionRequest, ActionResponse actionResponse,
			Calendar calendar)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String editCalendarURL = getRedirect(actionRequest, actionResponse);

		if (Validator.isNull(editCalendarURL)) {
			editCalendarURL = PortalUtil.getLayoutFullURL(themeDisplay);
		}

		String namespace = actionResponse.getNamespace();

		editCalendarURL = HttpUtil.setParameter(
			editCalendarURL, "p_p_id", PortletKeys.CALENDAR);
		editCalendarURL = HttpUtil.setParameter(
			editCalendarURL, namespace + "mvcPath",
			templatePath + "edit_calendar.jsp");
		editCalendarURL = HttpUtil.setParameter(
			editCalendarURL, namespace + "redirect",
			getRedirect(actionRequest, actionResponse));
		editCalendarURL = HttpUtil.setParameter(
			editCalendarURL, namespace + "backURL",
			ParamUtil.getString(actionRequest, "backURL"));
		editCalendarURL = HttpUtil.setParameter(
			editCalendarURL, namespace + "calendarId",
			calendar.getCalendarId());

		return editCalendarURL;
	}

	protected java.util.Calendar getJCalendar(
		PortletRequest portletRequest, String name) {

		int month = ParamUtil.getInteger(portletRequest, name + "Month");
		int day = ParamUtil.getInteger(portletRequest, name + "Day");
		int year = ParamUtil.getInteger(portletRequest, name + "Year");
		int hour = ParamUtil.getInteger(portletRequest, name + "Hour");
		int minute = ParamUtil.getInteger(portletRequest, name + "Minute");

		int amPm = ParamUtil.getInteger(portletRequest, name + "AmPm");

		if (amPm == java.util.Calendar.PM) {
			hour += 12;
		}

		return JCalendarUtil.getJCalendar(
			year, month, day, hour, minute, 0, 0, getTimeZone(portletRequest));
	}

	protected String getNotificationTypeSettings(
		ActionRequest actionRequest, NotificationType notificationType) {

		UnicodeProperties notificationTypeSettingsProperties =
			new UnicodeProperties(true);

		if (notificationType == NotificationType.EMAIL) {
			String fromAddress = ParamUtil.getString(
				actionRequest, "fromAddress");
			String fromName = ParamUtil.getString(actionRequest, "fromName");

			notificationTypeSettingsProperties.put(
				CalendarNotificationTemplateConstants.PROPERTY_FROM_ADDRESS,
				fromAddress);
			notificationTypeSettingsProperties.put(
				CalendarNotificationTemplateConstants.PROPERTY_FROM_NAME,
				fromName);
		}

		return notificationTypeSettingsProperties.toString();
	}

	protected String getRecurrence(ActionRequest actionRequest) {
		boolean repeat = ParamUtil.getBoolean(actionRequest, "repeat");

		if (!repeat) {
			return null;
		}

		Recurrence recurrence = new Recurrence();

		int count = 0;

		String ends = ParamUtil.getString(actionRequest, "ends");

		if (ends.equals("after")) {
			count = ParamUtil.getInteger(actionRequest, "count");
		}

		recurrence.setCount(count);

		Frequency frequency = Frequency.parse(
			ParamUtil.getString(actionRequest, "frequency"));

		recurrence.setFrequency(frequency);

		int interval = ParamUtil.getInteger(actionRequest, "interval");

		recurrence.setInterval(interval);

		java.util.Calendar untilJCalendar = null;

		if (ends.equals("on")) {
			int untilDateDay = ParamUtil.getInteger(
				actionRequest, "untilDateDay");
			int untilDateMonth = ParamUtil.getInteger(
				actionRequest, "untilDateMonth");
			int untilDateYear = ParamUtil.getInteger(
				actionRequest, "untilDateYear");

			untilJCalendar = CalendarFactoryUtil.getCalendar();

			untilJCalendar.set(java.util.Calendar.DATE, untilDateDay);
			untilJCalendar.set(java.util.Calendar.MONTH, untilDateMonth);
			untilJCalendar.set(java.util.Calendar.YEAR, untilDateYear);
		}

		recurrence.setUntilJCalendar(untilJCalendar);

		List<PositionalWeekday> positionalWeekdays =
			new ArrayList<PositionalWeekday>();

		if (frequency == Frequency.WEEKLY) {
			for (Weekday weekday : Weekday.values()) {
				boolean checked = ParamUtil.getBoolean(
					actionRequest, weekday.getValue());

				if (checked) {
					positionalWeekdays.add(new PositionalWeekday(weekday, 0));
				}
			}
		}
		else if ((frequency == Frequency.MONTHLY) ||
				 (frequency == Frequency.YEARLY)) {

			boolean repeatOnWeekday = ParamUtil.getBoolean(
				actionRequest, "repeatOnWeekday");

			if (repeatOnWeekday) {
				int position = ParamUtil.getInteger(actionRequest, "position");

				Weekday weekday = Weekday.parse(
					ParamUtil.getString(actionRequest, "weekday"));

				positionalWeekdays.add(
					new PositionalWeekday(weekday, position));

				if (frequency == Frequency.YEARLY) {
					List<Integer> months = Arrays.asList(
						ParamUtil.getInteger(actionRequest, "startTimeMonth"));

					recurrence.setMonths(months);
				}
			}
		}

		recurrence.setPositionalWeekdays(positionalWeekdays);

		String[] exceptionDates = StringUtil.split(
			ParamUtil.getString(actionRequest, "exceptionDates"));

		for (String exceptionDate : exceptionDates) {
			recurrence.addExceptionDate(
				JCalendarUtil.getJCalendar(Long.valueOf(exceptionDate)));
		}

		return RecurrenceSerializer.serialize(recurrence);
	}

	protected long[] getReminders(PortletRequest portletRequest) {
		long firstReminder = ParamUtil.getInteger(
			portletRequest, "reminderValue0");
		long firstReminderDuration = ParamUtil.getInteger(
			portletRequest, "reminderDuration0");
		long secondReminder = ParamUtil.getInteger(
			portletRequest, "reminderValue1");
		long secondReminderDuration = ParamUtil.getInteger(
			portletRequest, "reminderDuration1");

		return new long[] {
			firstReminder * firstReminderDuration * Time.SECOND,
			secondReminder * secondReminderDuration * Time.SECOND
		};
	}

	protected String[] getRemindersType(PortletRequest portletRequest) {
		String firstReminderType = ParamUtil.getString(
			portletRequest, "reminderType0");
		String secondReminderType = ParamUtil.getString(
			portletRequest, "reminderType1");

		return new String[] {
			firstReminderType, secondReminderType
		};
	}

	protected TimeZone getTimeZone(PortletRequest portletRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		boolean allDay = ParamUtil.getBoolean(portletRequest, "allDay");

		if (allDay) {
			return TimeZoneUtil.getTimeZone(StringPool.UTC);
		}

		PortletPreferences preferences = portletRequest.getPreferences();

		User user = themeDisplay.getUser();

		String timeZoneId = preferences.getValue(
			"timeZoneId", user.getTimeZoneId());

		if (Validator.isNull(timeZoneId)) {
			timeZoneId = user.getTimeZoneId();
		}

		return TimeZone.getTimeZone(timeZoneId);
	}

	@Override
	protected boolean isSessionErrorException(Throwable cause) {
		if (cause instanceof CalendarBookingDurationException ||
			cause instanceof CalendarBookingRecurrenceException ||
			cause instanceof CalendarNameException ||
			cause instanceof CalendarResourceCodeException ||
			cause instanceof CalendarResourceNameException ||
			cause instanceof DuplicateCalendarResourceException ||
			cause instanceof PrincipalException) {

			return true;
		}

		return false;
	}

	protected Hits search(long companyId, long userId, String keywords)
		throws Exception {

		SearchContext searchContext = new SearchContext();

		searchContext.setAttribute(Field.NAME, keywords);
		searchContext.setAttribute("resourceName", keywords);
		searchContext.setCompanyId(companyId);
		searchContext.setEnd(SearchContainer.DEFAULT_DELTA);
		searchContext.setGroupIds(new long[0]);
		searchContext.setStart(0);
		searchContext.setUserId(userId);

		Indexer indexer = CalendarSearcher.getInstance();

		return indexer.search(searchContext);
	}

	protected void serveCalendarBookingInvitees(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long parentCalendarBookingId = ParamUtil.getLong(
			resourceRequest, "parentCalendarBookingId");

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<CalendarBooking> childCalendarBookings =
			CalendarBookingServiceUtil.getChildCalendarBookings(
				parentCalendarBookingId);

		Collection<CalendarResource> calendarResources =
			CalendarUtil.getCalendarResources(childCalendarBookings);

		for (CalendarResource calendarResource : calendarResources) {
			JSONObject jsonObject = CalendarUtil.toCalendarResourceJSONObject(
				themeDisplay, calendarResource);

			jsonArray.put(jsonObject);
		}

		writeJSON(resourceRequest, resourceResponse, jsonArray);
	}

	protected void serveCalendarBookings(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws IOException, PortalException, SystemException {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long[] calendarIds = ParamUtil.getLongValues(
			resourceRequest, "calendarIds");
		long endTime = ParamUtil.getLong(resourceRequest, "endTime");
		long startTime = ParamUtil.getLong(resourceRequest, "startTime");
		int[] statuses = ParamUtil.getIntegerValues(
			resourceRequest, "statuses");

		List<CalendarBooking> calendarBookings =
			CalendarBookingServiceUtil.search(
				themeDisplay.getCompanyId(), new long[0], calendarIds,
				new long[0], -1, null, startTime, endTime, true, statuses,
				QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

		JSONArray jsonArray = CalendarUtil.toCalendarBookingsJSONArray(
			themeDisplay, calendarBookings, getTimeZone(resourceRequest));

		writeJSON(resourceRequest, resourceResponse, jsonArray);
	}

	protected void serveCalendarBookingsRSS(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		PortletPreferences portletPreferences =
			resourceRequest.getPreferences();

		boolean enableRss = GetterUtil.getBoolean(
			portletPreferences.getValue("enableRss", null), true);

		if (!PortalUtil.isRSSFeedsEnabled() || !enableRss) {
			PortalUtil.sendRSSFeedsDisabledError(
					resourceRequest, resourceResponse);

			return;
		}

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long calendarId = ParamUtil.getLong(resourceRequest, "calendarId");

		long timeInterval = GetterUtil.getLong(
			portletPreferences.getValue("rssTimeInterval", StringPool.BLANK),
			RSSUtil.TIME_INTERVAL_DEFAULT);

		long startTime = System.currentTimeMillis();

		long endTime = startTime + timeInterval;

		int max = GetterUtil.getInteger(
			portletPreferences.getValue("rssDelta", StringPool.BLANK),
			SearchContainer.DEFAULT_DELTA);
		String rssFeedType = portletPreferences.getValue(
			"rssFeedType", RSSUtil.FORMAT_DEFAULT);
		String type = RSSUtil.getFormatType(rssFeedType);
		double version = RSSUtil.getFeedTypeVersion(rssFeedType);
		String displayStyle = portletPreferences.getValue(
			"rssDisplayStyle", RSSUtil.DISPLAY_STYLE_DEFAULT);

		String rss = CalendarBookingServiceUtil.getCalendarBookingsRSS(
			calendarId, startTime, endTime, max, type, version, displayStyle,
			themeDisplay);

		PortletResponseUtil.sendFile(
			resourceRequest, resourceResponse, null, rss.getBytes(),
			ContentTypes.TEXT_XML_UTF8);
	}

	protected void serveCalendarRenderingRules(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long[] calendarIds = ParamUtil.getLongValues(
			resourceRequest, "calendarIds");
		int[] statuses = {
			CalendarBookingWorkflowConstants.STATUS_APPROVED,
			CalendarBookingWorkflowConstants.STATUS_MAYBE,
			CalendarBookingWorkflowConstants.STATUS_PENDING
		};
		long startTime = ParamUtil.getLong(resourceRequest, "startTime");
		long endTime = ParamUtil.getLong(resourceRequest, "endTime");
		String ruleName = ParamUtil.getString(resourceRequest, "ruleName");

		if (calendarIds.length > 0) {
			JSONObject jsonObject = CalendarUtil.getCalendarRenderingRules(
				themeDisplay, calendarIds, statuses, startTime, endTime,
				ruleName, getTimeZone(resourceRequest));

			writeJSON(resourceRequest, resourceResponse, jsonObject);
		}
	}

	protected void serveCalendarResources(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String keywords = ParamUtil.getString(resourceRequest, "keywords");

		Set<Calendar> calendarsSet = new LinkedHashSet<Calendar>();

		Hits hits = search(
			themeDisplay.getCompanyId(), themeDisplay.getUserId(), keywords);

		for (Document document : hits.getDocs()) {
			long calendarId = GetterUtil.getLong(
				document.get(Field.ENTRY_CLASS_PK));
			Calendar calendar = CalendarServiceUtil.getCalendar(calendarId);

			calendarsSet.add(calendar);
		}

		long groupClassNameId = PortalUtil.getClassNameId(Group.class);

		String name = StringUtil.merge(
			CustomSQLUtil.keywords(keywords), StringPool.BLANK);

		LinkedHashMap<String, Object> params =
			new LinkedHashMap<String, Object>();

		params.put("usersGroups", themeDisplay.getUserId());

		List<Group> groups = GroupLocalServiceUtil.search(
			themeDisplay.getCompanyId(), name, null, params, true, 0,
			SearchContainer.DEFAULT_DELTA);

		for (Group group : groups) {
			addCalendar(
				resourceRequest, calendarsSet, groupClassNameId,
				group.getGroupId());
		}

		long userClassNameId = PortalUtil.getClassNameId(User.class);

		List<User> users = UserLocalServiceUtil.search(
			themeDisplay.getCompanyId(), keywords, 0, null, 0,
			SearchContainer.DEFAULT_DELTA, new UserFirstNameComparator());

		for (User user : users) {
			addCalendar(
				resourceRequest, calendarsSet, userClassNameId,
				user.getUserId());
		}

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		for (Calendar calendar : calendarsSet) {
			JSONObject jsonObject = CalendarUtil.toCalendarJSONObject(
				themeDisplay, calendar);

			jsonArray.put(jsonObject);
		}

		writeJSON(resourceRequest, resourceResponse, jsonArray);
	}

	protected void serveExportCalendar(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long calendarId = ParamUtil.getLong(resourceRequest, "calendarId");

		Calendar calendar = CalendarLocalServiceUtil.getCalendar(calendarId);

		String fileName =
			calendar.getName(themeDisplay.getLocale()) + CharPool.PERIOD +
				String.valueOf(CalendarDataFormat.ICAL);

		CalendarDataHandler calendarDataHandler =
			CalendarDataHandlerFactory.getCalendarDataHandler(
				CalendarDataFormat.ICAL);

		String data = calendarDataHandler.exportCalendar(calendarId);

		String contentType = MimeTypesUtil.getContentType(fileName);

		PortletResponseUtil.sendFile(
			resourceRequest, resourceResponse, fileName, data.getBytes(),
			contentType);
	}

	protected void serveImportCalendar(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		UploadPortletRequest uploadPortletRequest =
			PortalUtil.getUploadPortletRequest(resourceRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long calendarId = ParamUtil.getLong(uploadPortletRequest, "calendarId");

		File file = uploadPortletRequest.getFile("file");

		String data = FileUtil.read(file);

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		if (Validator.isNotNull(data)) {
			try {
				CalendarDataHandler calendarDataHandler =
					CalendarDataHandlerFactory.getCalendarDataHandler(
						CalendarDataFormat.ICAL);

				calendarDataHandler.importCalendar(calendarId, data);

				jsonObject.put("success", true);
			}
			catch (Exception e) {
				String message = themeDisplay.translate(
						"an-unexpected-error-occurred-while-importing-your-" +
						"file");

				jsonObject.put("error", message);
			}
		}
		else {
			String message = themeDisplay.translate(
				"failed-to-import-empty-file");

			jsonObject.put("error", message);
		}

		writeJSON(resourceRequest, resourceResponse, jsonObject);
	}

	protected void serveResourceCalendars(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long calendarResourceId = ParamUtil.getLong(
			resourceRequest, "calendarResourceId");

		List<Calendar> calendars = CalendarServiceUtil.search(
			themeDisplay.getCompanyId(), null, new long[] {calendarResourceId},
			null, true, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

		JSONArray jsonObject = CalendarUtil.toCalendarsJSONArray(
			themeDisplay, calendars);

		writeJSON(resourceRequest, resourceResponse, jsonObject);
	}

	protected void subscribeToComments(
			ActionRequest actionRequest, boolean subscribe)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String className = ParamUtil.getString(actionRequest, "className");
		long classPK = ParamUtil.getLong(actionRequest, "classPK");

		if (subscribe) {
			SubscriptionLocalServiceUtil.addSubscription(
				themeDisplay.getUserId(), themeDisplay.getScopeGroupId(),
				className, classPK);
		}
		else {
			SubscriptionLocalServiceUtil.deleteSubscription(
				themeDisplay.getUserId(), className, classPK);
		}
	}

	protected MBMessage updateMessage(ActionRequest actionRequest)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String className = ParamUtil.getString(actionRequest, "className");
		long classPK = ParamUtil.getLong(actionRequest, "classPK");
		String permissionClassName = ParamUtil.getString(
			actionRequest, "permissionClassName");
		long permissionClassPK = ParamUtil.getLong(
			actionRequest, "permissionClassPK");
		long permissionOwnerId = ParamUtil.getLong(
			actionRequest, "permissionOwnerId");

		long messageId = ParamUtil.getLong(actionRequest, "messageId");

		long threadId = ParamUtil.getLong(actionRequest, "threadId");
		long parentMessageId = ParamUtil.getLong(
			actionRequest, "parentMessageId");
		String subject = ParamUtil.getString(actionRequest, "subject");
		String body = ParamUtil.getString(actionRequest, "body");

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			MBMessage.class.getName(), actionRequest);

		MBMessage message = null;

		if (messageId <= 0) {
			message = MBMessageServiceUtil.addDiscussionMessage(
				serviceContext.getScopeGroupId(), className, classPK,
				permissionClassName, permissionClassPK, permissionOwnerId,
				threadId, parentMessageId, subject, body, serviceContext);
		}
		else {
			message = MBMessageServiceUtil.updateDiscussionMessage(
				className, classPK, permissionClassName, permissionClassPK,
				permissionOwnerId, messageId, subject, body, serviceContext);
		}

		// Subscription

		boolean subscribe = ParamUtil.getBoolean(actionRequest, "subscribe");

		if (subscribe) {
			SubscriptionLocalServiceUtil.addSubscription(
				themeDisplay.getUserId(), themeDisplay.getScopeGroupId(),
				className, classPK);
		}

		return message;
	}

}