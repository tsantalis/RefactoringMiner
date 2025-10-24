/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;
import java.rmi.UnmarshalException;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.support.RemoteInvocation;

/**
 * @author Juergen Hoeller
 * @since 16.05.2003
 */
public class RmiSupportTests extends TestCase {

	public void testRmiProxyFactoryBean() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertEquals(RemoteBean.name, "myName");
		assertEquals(1, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithRemoteException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(RemoteException.class);
	}

	public void testRmiProxyFactoryBeanWithConnectException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(ConnectException.class);
	}

	public void testRmiProxyFactoryBeanWithConnectIOException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(ConnectIOException.class);
	}

	public void testRmiProxyFactoryBeanWithUnknownHostException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(UnknownHostException.class);
	}

	public void testRmiProxyFactoryBeanWithNoSuchObjectException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(NoSuchObjectException.class);
	}

	public void testRmiProxyFactoryBeanWithStubNotFoundException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(StubNotFoundException.class);
	}

	public void testRmiProxyFactoryBeanWithMarshalException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(MarshalException.class);
	}

	public void testRmiProxyFactoryBeanWithUnmarshalException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(UnmarshalException.class);
	}

	private void doTestRmiProxyFactoryBeanWithException(Class<?> exceptionClass) throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		try {
			proxy.setName(exceptionClass.getName());
			fail("Should have thrown " + exceptionClass.getName());
		}
		catch (Exception ex) {
			if (exceptionClass.isInstance(ex)) {
				// expected
			}
			else {
				throw ex;
			}
		}
		assertEquals(1, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithConnectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(ConnectException.class);
	}

	public void testRmiProxyFactoryBeanWithConnectIOExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(ConnectIOException.class);
	}

	public void testRmiProxyFactoryBeanWithUnknownHostExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(UnknownHostException.class);
	}

	public void testRmiProxyFactoryBeanWithNoSuchObjectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(NoSuchObjectException.class);
	}

	public void testRmiProxyFactoryBeanWithStubNotFoundExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(StubNotFoundException.class);
	}

	private void doTestRmiProxyFactoryBeanWithExceptionAndRefresh(Class<?> exceptionClass) throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.setRefreshStubOnConnectFailure(true);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		try {
			proxy.setName(exceptionClass.getName());
			fail("Should have thrown " + exceptionClass.getName());
		}
		catch (Exception ex) {
			if (exceptionClass.isInstance(ex)) {
				// expected
			}
			else {
				throw ex;
			}
		}
		assertEquals(2, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterface() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IBusinessBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		assertFalse(proxy instanceof IRemoteBean);
		proxy.setName("myName");
		assertEquals(RemoteBean.name, "myName");
		assertEquals(1, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithWrongBusinessInterface() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IWrongBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IWrongBusinessBean);
		IWrongBusinessBean proxy = (IWrongBusinessBean) factory.getObject();
		assertFalse(proxy instanceof IRemoteBean);
		try {
			proxy.setOtherName("name");
			fail("Should have thrown RemoteProxyFailureException");
		}
		catch (RemoteProxyFailureException ex) {
			assertTrue(ex.getCause() instanceof NoSuchMethodException);
			assertTrue(ex.getMessage().contains("setOtherName"));
			assertTrue(ex.getMessage().contains("IWrongBusinessBean"));
		}
		assertEquals(1, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndRemoteException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				RemoteException.class, RemoteAccessException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndConnectException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				ConnectException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndConnectIOException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				ConnectIOException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndUnknownHostException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				UnknownHostException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndNoSuchObjectExceptionException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				NoSuchObjectException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndStubNotFoundException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				StubNotFoundException.class, RemoteConnectFailureException.class);
	}

	private void doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
			Class<?> rmiExceptionClass, Class<?> springExceptionClass) throws Exception {

		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IBusinessBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		assertFalse(proxy instanceof IRemoteBean);
		try {
			proxy.setName(rmiExceptionClass.getName());
			fail("Should have thrown " + rmiExceptionClass.getName());
		}
		catch (Exception ex) {
			if (springExceptionClass.isInstance(ex)) {
				// expected
			}
			else {
				throw ex;
			}
		}
		assertEquals(1, factory.counter);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndRemoteExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				RemoteException.class, RemoteAccessException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndConnectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				ConnectException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndConnectIOExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				ConnectIOException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndUnknownHostExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				UnknownHostException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndNoSuchObjectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				NoSuchObjectException.class, RemoteConnectFailureException.class);
	}

	public void testRmiProxyFactoryBeanWithBusinessInterfaceAndStubNotFoundExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				StubNotFoundException.class, RemoteConnectFailureException.class);
	}

	private void doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
			Class<?> rmiExceptionClass, Class<?> springExceptionClass) throws Exception {

		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.setRefreshStubOnConnectFailure(true);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IBusinessBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		assertFalse(proxy instanceof IRemoteBean);
		try {
			proxy.setName(rmiExceptionClass.getName());
			fail("Should have thrown " + rmiExceptionClass.getName());
		}
		catch (Exception ex) {
			if (springExceptionClass.isInstance(ex)) {
				// expected
			}
			else {
				throw ex;
			}
		}
		if (RemoteConnectFailureException.class.isAssignableFrom(springExceptionClass)) {
			assertEquals(2, factory.counter);
		}
		else {
			assertEquals(1, factory.counter);
		}
	}

	public void testRmiClientInterceptorRequiresUrl() throws Exception{
		RmiClientInterceptor client = new RmiClientInterceptor();
		client.setServiceInterface(IRemoteBean.class);

		try {
			client.afterPropertiesSet();
			fail("url isn't set, expected IllegalArgumentException");
		}
		catch(IllegalArgumentException e){
			// expected
		}
	}

	public void testRemoteInvocation() throws NoSuchMethodException {
		// let's see if the remote invocation object works

		final RemoteBean rb = new RemoteBean();
		final Method setNameMethod = rb.getClass().getDeclaredMethod("setName", String.class);

		MethodInvocation mi = new MethodInvocation() {
			@Override
			public Method getMethod() {
				return setNameMethod;
			}
			@Override
			public Object[] getArguments() {
				return new Object[] {"bla"};
			}
			@Override
			public Object proceed() throws Throwable {
				throw new UnsupportedOperationException();
			}
			@Override
			public Object getThis() {
				return rb;
			}
			@Override
			public AccessibleObject getStaticPart() {
				return setNameMethod;
			}
		};

		RemoteInvocation inv = new RemoteInvocation(mi);

		assertEquals("setName", inv.getMethodName());
		assertEquals("bla", inv.getArguments()[0]);
		assertEquals(String.class, inv.getParameterTypes()[0]);

		// this is a bit BS, but we need to test it
		inv = new RemoteInvocation();
		inv.setArguments(new Object[] { "bla" });
		assertEquals("bla", inv.getArguments()[0]);
		inv.setMethodName("setName");
		assertEquals("setName", inv.getMethodName());
		inv.setParameterTypes(new Class<?>[] {String.class});
		assertEquals(String.class, inv.getParameterTypes()[0]);

		inv = new RemoteInvocation("setName", new Class<?>[] {String.class}, new Object[] {"bla"});
		assertEquals("bla", inv.getArguments()[0]);
		assertEquals("setName", inv.getMethodName());
		assertEquals(String.class, inv.getParameterTypes()[0]);
	}

	public void testRmiInvokerWithSpecialLocalMethods() throws Exception {
		String serviceUrl = "rmi://localhost:1090/test";
		RmiProxyFactoryBean factory = new RmiProxyFactoryBean() {
			@Override
			protected Remote lookupStub() {
				return new RmiInvocationHandler() {
					@Override
					public String getTargetInterfaceName() {
						return null;
					}
					@Override
					public Object invoke(RemoteInvocation invocation) throws RemoteException {
						throw new RemoteException();
					}
				};
			}
		};
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl(serviceUrl);
		factory.afterPropertiesSet();
		IBusinessBean proxy = (IBusinessBean) factory.getObject();

		// shouldn't go through to remote service
		assertTrue(proxy.toString().contains("RMI invoker"));
		assertTrue(proxy.toString().contains(serviceUrl));
		assertEquals(proxy.hashCode(), proxy.hashCode());
		assertTrue(proxy.equals(proxy));

		// should go through
		try {
			proxy.setName("test");
			fail("Should have thrown RemoteAccessException");
		}
		catch (RemoteAccessException ex) {
			// expected
		}
	}


	private static class CountingRmiProxyFactoryBean extends RmiProxyFactoryBean {

		private int counter = 0;

		@Override
		protected Remote lookupStub() {
			counter++;
			return new RemoteBean();
		}
	}


	public static interface IBusinessBean {

		public void setName(String name);

	}


	public static interface IWrongBusinessBean {

		public void setOtherName(String name);

	}


	public static interface IRemoteBean extends Remote {

		public void setName(String name) throws RemoteException;

	}


	public static class RemoteBean implements IRemoteBean {

		private static String name;

		@Override
		public void setName(String nam) throws RemoteException {
			if (nam != null && nam.endsWith("Exception")) {
				RemoteException rex;
				try {
					Class<?> exClass = Class.forName(nam);
					Constructor<?> ctor = exClass.getConstructor(String.class);
					rex = (RemoteException) ctor.newInstance("myMessage");
				}
				catch (Exception ex) {
					throw new RemoteException("Illegal exception class name: " + nam, ex);
				}
				throw rex;
			}
			name = nam;
		}
	}

}
