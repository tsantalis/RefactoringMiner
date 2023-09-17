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

package com.liferay.sync.hook.listeners;

import com.liferay.portal.ModelListenerException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.BaseModelListener;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.sync.model.SyncConstants;
import com.liferay.sync.model.SyncDLObject;
import com.liferay.sync.service.SyncDLObjectLocalServiceUtil;

/**
 * @author Shinn Lok
 */
public class ResourcePermissionModelListener
	extends BaseModelListener<ResourcePermission> {

	@Override
	public void onAfterUpdate(ResourcePermission resourcePermission)
		throws ModelListenerException {

		String modelName = resourcePermission.getName();

		try {
			if (modelName.equals(DLFileEntry.class.getName())) {
				SyncDLObject syncDLObject =
					SyncDLObjectLocalServiceUtil.fetchSyncDLObject(
						SyncConstants.TYPE_FILE,
						GetterUtil.getLong(resourcePermission.getPrimKey()));

				if (syncDLObject == null) {
					return;
				}

				syncDLObject.setModifiedTime(System.currentTimeMillis());

				SyncDLObjectLocalServiceUtil.updateSyncDLObject(syncDLObject);
			}
			else if (modelName.equals(DLFolder.class.getName())) {
				SyncDLObject syncDLObject =
					SyncDLObjectLocalServiceUtil.fetchSyncDLObject(
						SyncConstants.TYPE_FOLDER,
						GetterUtil.getLong(resourcePermission.getPrimKey()));

				if (syncDLObject == null) {
					return;
				}

				syncDLObject.setModifiedTime(System.currentTimeMillis());

				SyncDLObjectLocalServiceUtil.updateSyncDLObject(syncDLObject);
			}
		}
		catch (Exception e) {
			throw new ModelListenerException(e);
		}
	}

}