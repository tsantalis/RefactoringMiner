/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.caelum.vraptor.ioc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import br.com.caelum.vraptor.Accepts;
import br.com.caelum.vraptor.AroundCall;
import br.com.caelum.vraptor.InterceptionException;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.cache.VRaptorDefaultCache;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.controller.DefaultBeanClass;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.interceptor.AllMethodHandles;
import br.com.caelum.vraptor.interceptor.Interceptor;
import br.com.caelum.vraptor.interceptor.InterceptorMethodsCache;
import br.com.caelum.vraptor.interceptor.InterceptorRegistry;
import br.com.caelum.vraptor.interceptor.StepInvoker;

public class InterceptorStereotypeHandlerTest {

	private @Mock InterceptorRegistry interceptorRegistry;
	private InterceptorStereotypeHandler handler;
	private @Mock InterceptorMethodsCache interceptorMethodsCache;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);		
		handler = new InterceptorStereotypeHandler(interceptorRegistry,interceptorMethodsCache);
	}

	@Test
	public void shouldRegisterInterceptorsOnRegistry() throws Exception {
		handler.handle(new DefaultBeanClass(InterceptorA.class));
		verify(interceptorRegistry, times(1)).register(InterceptorA.class);
	}
	
	@Test
	public void shouldRegisterAspectStyleInterceptorAndCacheMethods() throws Exception {
		handler.handle(new DefaultBeanClass(InterceptorB.class));
		verify(interceptorRegistry, times(1)).register(InterceptorB.class);
		verify(interceptorMethodsCache).put(InterceptorB.class);
		
	}

	static class InterceptorA implements Interceptor {

		public boolean accepts(ControllerMethod method) {
			return false;
		}

		public void intercept(InterceptorStack stack, ControllerMethod method,
				Object controllerInstance) throws InterceptionException {
		}
	}

	@Intercepts
	static class InterceptorB{

		@Accepts
		public boolean accepts() {
			return false;
		}

		@AroundCall
		public void intercept(InterceptorStack stack, ControllerMethod method,
				Object controllerInstance) throws InterceptionException {
		}
	}
}
