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
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.BaseModelListener;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.sync.model.SyncConstants;
import com.liferay.sync.model.SyncDLObject;
import com.liferay.sync.service.SyncDLObjectLocalServiceUtil;

import java.util.List;

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
			String type = null;

			if (modelName.equals(DLFileEntry.class.getName())) {
				type = SyncConstants.TYPE_FILE;
			}
			else if (modelName.equals(DLFolder.class.getName())) {
				type = SyncConstants.TYPE_FOLDER;
			}

			SyncDLObject syncDLObject =
				SyncDLObjectLocalServiceUtil.fetchSyncDLObject(
					type, GetterUtil.getLong(resourcePermission.getPrimKey()));

			if (syncDLObject != null) {
				updateSyncDLObject(syncDLObject);
			}
		}
		catch (Exception e) {
			throw new ModelListenerException(e);
		}
	}

	protected void updateSyncDLObject(SyncDLObject syncDLObject)
		throws SystemException {

		syncDLObject.setModifiedTime(System.currentTimeMillis());

		SyncDLObjectLocalServiceUtil.updateSyncDLObject(syncDLObject);

		String type = syncDLObject.getType();

		if (!type.equals(SyncConstants.TYPE_FOLDER)) {
			return;
		}

		List<SyncDLObject> curSyncDLObjects =
			SyncDLObjectLocalServiceUtil.getSyncDLObjects(
				syncDLObject.getTypePK());

		for (SyncDLObject curSyncDLObject : curSyncDLObjects) {
			updateSyncDLObject(curSyncDLObject);
		}
	}

}