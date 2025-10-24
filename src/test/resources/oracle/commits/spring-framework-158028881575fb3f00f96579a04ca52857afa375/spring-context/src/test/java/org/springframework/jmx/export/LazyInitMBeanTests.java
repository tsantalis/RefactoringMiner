/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class LazyInitMBeanTests extends TestCase {

	public void testLazyInit() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getApplicationContextPath());
		ctx.close();
	}

	public void testInvokeOnLazyInitBean() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getApplicationContextPath());
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean"));
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean2"));
		try {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean2");
			String name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "foo", name);
		}
		finally {
			ctx.close();
		}
	}

	private String getApplicationContextPath() {
		return "org/springframework/jmx/export/lazyInit.xml";
	}

}
