package br.com.caelum.vraptor.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import br.com.caelum.vraptor.InterceptionException;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.VRaptorException;
import br.com.caelum.vraptor.controller.ControllerInstance;
import br.com.caelum.vraptor.controller.ControllerMethod;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.factory.StepInvokerFactory;
import br.com.caelum.vraptor.interceptor.example.AcceptsInterceptor;
import br.com.caelum.vraptor.interceptor.example.AcceptsInterceptorWithStackAsParameter;
import br.com.caelum.vraptor.interceptor.example.AcceptsWithoutArgsInterceptor;
import br.com.caelum.vraptor.interceptor.example.AlwaysAcceptsAspectInterceptor;
import br.com.caelum.vraptor.interceptor.example.AroundInterceptorWithoutSimpleStackParameter;
import br.com.caelum.vraptor.interceptor.example.BeforeAfterInterceptorWithStackAsParameter;
import br.com.caelum.vraptor.interceptor.example.ExampleOfSimpleStackInterceptor;
import br.com.caelum.vraptor.interceptor.example.InterceptorWithCustomizedAccepts;
import br.com.caelum.vraptor.interceptor.example.InternalAndCustomAcceptsInterceptor;
import br.com.caelum.vraptor.interceptor.example.MethodLevelAcceptsController;
import br.com.caelum.vraptor.interceptor.example.NonBooleanAcceptsInterceptor;
import br.com.caelum.vraptor.interceptor.example.VoidAcceptsInterceptor;
import br.com.caelum.vraptor.interceptor.example.WithoutAroundInterceptor;
import br.com.caelum.vraptor.ioc.Container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AspectStyleInterceptorHandlerTest {

	private static final StepInvoker stepInvoker = StepInvokerFactory.create();
	private @Mock
	InterceptorStack stack;
	private @Mock
	ControllerMethod controllerMethod;
	private @Mock
	Object currentController;
	private @Mock
	WithAnnotationAcceptor withAnnotationAcceptor;
	private @Mock
	ControllerInstance controllerInstance;
	private @Mock
	SimpleInterceptorStack simpleInterceptorStack;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldAlwaysCallAround() {
		AlwaysAcceptsAspectInterceptor interceptor = spy(new AlwaysAcceptsAspectInterceptor());
		AspectStyleInterceptorHandler handler = newAspectStyleInterceptorHandler(
				AlwaysAcceptsAspectInterceptor.class, interceptor);

		handler.execute(stack, controllerMethod, currentController);

		verify(interceptor).intercept(Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test
	public void shouldInvokeUsingBeforeAndAfter() {
		AlwaysAcceptsAspectInterceptor interceptor = spy(new AlwaysAcceptsAspectInterceptor());
		AspectStyleInterceptorHandler handler = newAspectStyleInterceptorHandler(
				AlwaysAcceptsAspectInterceptor.class, interceptor);

		handler.execute(stack, controllerMethod, currentController);

		InOrder order = inOrder(interceptor);
		order.verify(interceptor).begin();
		order.verify(interceptor).intercept(
				Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(interceptor).after();
	}

	@Test
	public void shouldInvokeIfAccepts() {
		AcceptsInterceptor acceptsInterceptor = spy(new AcceptsInterceptor(true));
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				AcceptsInterceptor.class, acceptsInterceptor);

		aspectHandler.execute(stack, controllerMethod, currentController);

		InOrder order = inOrder(acceptsInterceptor);
		order.verify(acceptsInterceptor).accepts(controllerMethod);
		order.verify(acceptsInterceptor).before();
		order.verify(acceptsInterceptor).around(
				Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(acceptsInterceptor).after();
	}

	@Test
	public void shouldNotInvokeIfDoesNotAccept() {
		AcceptsInterceptor acceptsInterceptor = spy(new AcceptsInterceptor(
				false));
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				AcceptsInterceptor.class, acceptsInterceptor);

		aspectHandler.execute(stack, controllerMethod, currentController);

		verify(acceptsInterceptor).accepts(controllerMethod);
		verify(acceptsInterceptor, never()).before();
		verify(acceptsInterceptor, never()).around(Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		verify(acceptsInterceptor, never()).after();
	}

	@Test(expected = VRaptorException.class)
	public void shouldVerifyIfAcceptsMethodReturnsVoid() {
		newInterceptorHandlerFor(new VoidAcceptsInterceptor());
	}

	@Test(expected = VRaptorException.class)
	public void shouldVerifyIfAcceptsMethodReturnsNonBooleanType() {
		newInterceptorHandlerFor(new NonBooleanAcceptsInterceptor());
	}

	private void newInterceptorHandlerFor(Object interceptor) {
		Container container = new InstanceContainer(interceptor,controllerMethod);
		InterceptorMethodParametersResolver parametersResolver = new InterceptorMethodParametersResolver(container);
		new AspectStyleInterceptorHandler(VoidAcceptsInterceptor.class, stepInvoker, container, parametersResolver);
	}

	@Test
	public void shouldInvokeAcceptsWithoutArgs() {
		AcceptsWithoutArgsInterceptor acceptsWithoutArgsInterceptor = spy(new AcceptsWithoutArgsInterceptor());
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				AcceptsWithoutArgsInterceptor.class,
				acceptsWithoutArgsInterceptor);

		aspectHandler.execute(stack, controllerMethod, currentController);

		InOrder order = inOrder(acceptsWithoutArgsInterceptor);
		order.verify(acceptsWithoutArgsInterceptor).accepts();
		order.verify(acceptsWithoutArgsInterceptor).before();
		order.verify(acceptsWithoutArgsInterceptor).around(
				Mockito.same(stack),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		order.verify(acceptsWithoutArgsInterceptor).after();
	}

	@Test
	public void shouldInvokeAroundWithSimpleStack() {
		ExampleOfSimpleStackInterceptor simpleStackInterceptor = spy(new ExampleOfSimpleStackInterceptor());
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				ExampleOfSimpleStackInterceptor.class, simpleStackInterceptor);

		aspectHandler.execute(stack, controllerMethod, currentController);

		verify(simpleStackInterceptor).around(
				Mockito.any(SimpleInterceptorStack.class));
	}

	@Test
	public void shouldInvokeNextIfNotAccepts() throws Exception {
		AcceptsInterceptor interceptor = spy(new AcceptsInterceptor(false));
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				AcceptsInterceptor.class, interceptor);

		aspectHandler.execute(stack, controllerMethod, null);

		verify(interceptor, never()).around(
				Mockito.any(InterceptorStack.class),
				Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
		verify(stack).next(Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test
	public void shouldNotInvokeIfDoesNotHaveAround() throws Exception {
		WithoutAroundInterceptor interceptor = spy(new WithoutAroundInterceptor());
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				WithoutAroundInterceptor.class, interceptor);

		aspectHandler.execute(stack, controllerMethod, null);

		verify(stack).next(Mockito.same(controllerMethod),
				Mockito.any(ControllerInstance.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void mustReceiveStackAsParameterForAroundCall() {
		AroundInterceptorWithoutSimpleStackParameter interceptor = new AroundInterceptorWithoutSimpleStackParameter();
		newAspectStyleInterceptorHandler(
				AroundInterceptorWithoutSimpleStackParameter.class, interceptor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void mustNotReceiveStackAsParameterForBeforeAfterCall() {
		BeforeAfterInterceptorWithStackAsParameter interceptor = new BeforeAfterInterceptorWithStackAsParameter();
		newAspectStyleInterceptorHandler(
				BeforeAfterInterceptorWithStackAsParameter.class, interceptor);

	}

	@Test(expected = VRaptorException.class)
	public void mustNotReceiveStackAsParameterForAcceptsCall() {
		AcceptsInterceptorWithStackAsParameter interceptor = new AcceptsInterceptorWithStackAsParameter();
		newAspectStyleInterceptorHandler(AcceptsInterceptorWithStackAsParameter.class, interceptor);

	}

	@Test(expected = VRaptorException.class)
	public void mustNotUseInternalAcceptsAndCustomAccepts(){
		InternalAndCustomAcceptsInterceptor interceptor = new InternalAndCustomAcceptsInterceptor();
		newAspectStyleInterceptorHandler(
				InternalAndCustomAcceptsInterceptor.class, interceptor);
	}

	@Test
	public void shouldAcceptCustomizedAccepts() throws Exception {
		InterceptorWithCustomizedAccepts interceptor = new InterceptorWithCustomizedAccepts();
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				InterceptorWithCustomizedAccepts.class, interceptor,
				withAnnotationAcceptor);
		when(
				withAnnotationAcceptor.validate(Mockito.same(controllerMethod),
						Mockito.any(ControllerInstance.class)))
				.thenReturn(true);
		aspectHandler.execute(stack, controllerMethod,
				new MethodLevelAcceptsController());

		assertTrue(interceptor.isBeforeCalled());
		assertTrue(interceptor.isInterceptCalled());
		assertTrue(interceptor.isAfterCalled());

	}

	@Test
	public void shouldNotAcceptCustomizedAccepts() throws Exception {
		InterceptorWithCustomizedAccepts interceptor = new InterceptorWithCustomizedAccepts();
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				InterceptorWithCustomizedAccepts.class, interceptor,
				withAnnotationAcceptor);
		when(
				withAnnotationAcceptor.validate(Mockito.same(controllerMethod),
						Mockito.any(ControllerInstance.class))).thenReturn(
				false);
		aspectHandler.execute(stack, controllerMethod,
				new MethodLevelAcceptsController());

		assertFalse(interceptor.isBeforeCalled());
		assertFalse(interceptor.isInterceptCalled());
		assertFalse(interceptor.isAfterCalled());

	}

	@Test
	public void shouldInvokeCustomAcceptsFailCallback() {
		InterceptorWithCustomizedAccepts interceptor = spy(new InterceptorWithCustomizedAccepts());
		AspectStyleInterceptorHandler aspectHandler = newAspectStyleInterceptorHandler(
				InterceptorWithCustomizedAccepts.class, interceptor,
				withAnnotationAcceptor);

		when(
				withAnnotationAcceptor.validate(Mockito.same(controllerMethod),
						Mockito.any(ControllerInstance.class))).thenReturn(
				false);

		aspectHandler.execute(stack, controllerMethod, aspectHandler);

		verify(interceptor).customAcceptsFailCallback();


	}

	@Intercepts
	class SimpleInterceptor {
		public void dummyMethodWithoutInterceptorAnnotations() {}
	}

	@Test(expected=InterceptionException.class)
	public void shoulThrowExceptionIfInterceptorDontHaveAnyCallableMethod() {
		SimpleInterceptor interceptor = new SimpleInterceptor();
		newAspectStyleInterceptorHandler(SimpleInterceptor.class, interceptor);
	}

	@SuppressWarnings("unchecked")
	private AspectStyleInterceptorHandler newAspectStyleInterceptorHandler(
			Class<?> interceptorClass, Object... dependencies) {
		@SuppressWarnings("rawtypes")
		List<Object> deps = new ArrayList(Arrays.asList(dependencies));
		boolean hasControllerInstance = false;
		for (Object object : deps) {
			if(ControllerInstance.class.isAssignableFrom(object.getClass())){
				hasControllerInstance = true;
				break;
			}
		}
		if(!hasControllerInstance){
			deps.add(controllerInstance);
		}
		deps.add(stack);
		deps.add(controllerMethod);
		deps.add(simpleInterceptorStack);
		Container container = new InstanceContainer(deps.toArray());
		InterceptorMethodParametersResolver parametersResolver = new InterceptorMethodParametersResolver(container);
		AspectStyleInterceptorHandler aspectHandler = new AspectStyleInterceptorHandler(
				interceptorClass, stepInvoker, container, parametersResolver);
		return aspectHandler;
	}
}