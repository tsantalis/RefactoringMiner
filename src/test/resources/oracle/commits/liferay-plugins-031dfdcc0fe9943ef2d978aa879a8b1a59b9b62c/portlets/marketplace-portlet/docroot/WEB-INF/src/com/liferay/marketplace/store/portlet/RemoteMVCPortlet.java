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

package com.liferay.marketplace.store.portlet;

import com.liferay.marketplace.oauth.util.OAuthUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Map;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

/**
 * @author Ryan Park
 */
public class RemoteMVCPortlet extends MVCPortlet {

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		String remoteMVCPath = renderRequest.getParameter("remoteMVCPath");

		if (remoteMVCPath != null) {
			try {
				remoteRender(renderRequest, renderResponse);
			}
			catch (IOException ioe) {
				throw ioe;
			}
			catch (Exception e) {
				throw new PortletException(e);
			}
		}
		else {
			super.render(renderRequest, renderResponse);
		}
	}

	protected String getRemotePortletURL() {
		return StringPool.BLANK;
	}

	protected void remoteRender(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		OAuthRequest oAuthRequest = new OAuthRequest(
			Verb.GET, getRemotePortletURL());

		setRequestParameters(renderRequest, renderResponse, oAuthRequest);

		Token token = OAuthUtil.getAccessToken(themeDisplay.getUser());

		if (token != null) {
			OAuthService oAuthService = OAuthUtil.getOAuthService();

			oAuthService.signRequest(token, oAuthRequest);
		}

		Response response = oAuthRequest.send();

		renderResponse.setContentType(ContentTypes.TEXT_HTML);

		PrintWriter printWriter = renderResponse.getWriter();

		printWriter.write(response.getBody());
	}

	protected void setRequestParameters(
			RenderRequest renderRequest, RenderResponse renderResponse,
			OAuthRequest oAuthRequest)
		throws Exception {

		String currentURL = PortalUtil.getCurrentURL(renderRequest);

		oAuthRequest.addQuerystringParameter("remoteURL", currentURL);

		oAuthRequest.addQuerystringParameter(
			"remotePortletNamespace", renderResponse.getNamespace());

		Map<String, String[]> parameterMap = renderRequest.getParameterMap();

		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();

			if (ArrayUtil.isEmpty(values) || Validator.isNull(values[0])) {
				continue;
			}

			oAuthRequest.addQuerystringParameter(entry.getKey(), values[0]);
		}
	}

}