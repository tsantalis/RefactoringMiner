/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.portlet.bind;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletRequest;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
public class PortletRequestParameterPropertyValuesTests {

	private final MockPortletRequest request = new MockPortletRequest();

	@Test
	public void withNoParams() {
		PortletRequestParameterPropertyValues pvs = new PortletRequestParameterPropertyValues(request);
		assertTrue("Should not have any property values", pvs.getPropertyValues().length == 0);
	}

	@Test
	public void withNoPrefix() {
		request.addParameter("param", "value");
		PortletRequestParameterPropertyValues pvs = new PortletRequestParameterPropertyValues(request);
		assertEquals("value", pvs.getPropertyValue("param").getValue());
	}

	@Test
	public void withPrefix() {
		request.addParameter("test_param", "value");
		PortletRequestParameterPropertyValues pvs = new PortletRequestParameterPropertyValues(request, "test");
		assertTrue(pvs.contains("param"));
		assertFalse(pvs.contains("test_param"));
		assertEquals("value", pvs.getPropertyValue("param").getValue());
	}

	@Test
	public void withPrefixAndOverridingSeparator() {
		request.addParameter("test.param", "value");
		request.addParameter("test_another", "anotherValue");
		request.addParameter("some.other", "someValue");
		PortletRequestParameterPropertyValues pvs = new PortletRequestParameterPropertyValues(request, "test", ".");
		assertFalse(pvs.contains("test.param"));
		assertFalse(pvs.contains("test_another"));
		assertFalse(pvs.contains("some.other"));
		assertFalse(pvs.contains("another"));
		assertFalse(pvs.contains("other"));
		assertTrue(pvs.contains("param"));
		assertEquals("value", pvs.getPropertyValue("param").getValue());
	}

}
