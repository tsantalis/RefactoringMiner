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

package org.springframework.web.servlet.mvc.mapping;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class ControllerBeanNameHandlerMappingTests {

	private static final String LOCATION = "/org/springframework/web/servlet/mvc/mapping/name-mapping.xml";

	private final XmlWebApplicationContext wac = new XmlWebApplicationContext();

	private HandlerMapping hm;


	@Before
	public void setUp() throws Exception {
		this.wac.setServletContext(new MockServletContext(""));
		this.wac.setConfigLocations(LOCATION);
		this.wac.refresh();
		this.hm = this.wac.getBean(HandlerMapping.class);
	}

	@After
	public void closeWac() {
		this.wac.close();
	}

	@Test
	public void indexUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("index"), chain.getHandler());
	}

	@Test
	public void mapSimpleUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/welcome");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withContextPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/welcome");
		request.setContextPath("/myapp");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withMultiActionControllerMapping() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("admin"), chain.getHandler());
	}

	@Test
	public void withoutControllerSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buy");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("buy"), chain.getHandler());
	}

}
