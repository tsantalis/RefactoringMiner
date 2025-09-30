/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.descriptor;

import static org.junit.gen5.commons.meta.API.Usage.Internal;
import static org.junit.gen5.commons.util.AnnotationUtils.findAnnotatedMethods;
import static org.junit.gen5.engine.junit5.execution.MethodInvocationContextFactory.methodInvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.junit.gen5.api.AfterAll;
import org.junit.gen5.api.AfterEach;
import org.junit.gen5.api.BeforeAll;
import org.junit.gen5.api.BeforeEach;
import org.junit.gen5.api.extension.AfterAllCallback;
import org.junit.gen5.api.extension.BeforeAllCallback;
import org.junit.gen5.api.extension.ConditionEvaluationResult;
import org.junit.gen5.api.extension.ContainerExtensionContext;
import org.junit.gen5.api.extension.Extension;
import org.junit.gen5.api.extension.ExtensionConfigurationException;
import org.junit.gen5.api.extension.TestExtensionContext;
import org.junit.gen5.commons.JUnitException;
import org.junit.gen5.commons.meta.API;
import org.junit.gen5.commons.util.Preconditions;
import org.junit.gen5.commons.util.ReflectionUtils;
import org.junit.gen5.commons.util.ReflectionUtils.MethodSortOrder;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestTag;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.junit5.execution.AfterAllMethodAdapter;
import org.junit.gen5.engine.junit5.execution.AfterEachMethodAdapter;
import org.junit.gen5.engine.junit5.execution.BeforeAllMethodAdapter;
import org.junit.gen5.engine.junit5.execution.BeforeEachMethodAdapter;
import org.junit.gen5.engine.junit5.execution.ConditionEvaluator;
import org.junit.gen5.engine.junit5.execution.JUnit5EngineExecutionContext;
import org.junit.gen5.engine.junit5.execution.MethodInvoker;
import org.junit.gen5.engine.junit5.execution.TestInstanceProvider;
import org.junit.gen5.engine.junit5.execution.ThrowableCollector;
import org.junit.gen5.engine.junit5.extension.ExtensionRegistry;
import org.junit.gen5.engine.support.descriptor.JavaSource;
import org.junit.gen5.engine.support.hierarchical.Container;

/**
 * {@link TestDescriptor} for tests based on Java classes.
 *
 * @since 5.0
 */
@API(Internal)
public class ClassTestDescriptor extends JUnit5TestDescriptor implements Container<JUnit5EngineExecutionContext> {

	private static final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();

	private final String displayName;

	private final Class<?> testClass;

	public ClassTestDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId);

		this.testClass = Preconditions.notNull(testClass, "Class must not be null");
		this.displayName = determineDisplayName(testClass, testClass.getName());

		setSource(new JavaSource(testClass));
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public final String getName() {
		return getTestClass().getName();
	}

	@Override
	public final String getDisplayName() {
		return this.displayName;
	}

	@Override
	public Set<TestTag> getTags() {
		return getTags(this.testClass);
	}

	@Override
	public final boolean isTest() {
		return false;
	}

	@Override
	public final boolean isContainer() {
		return true;
	}

	@Override
	public JUnit5EngineExecutionContext prepare(JUnit5EngineExecutionContext context) {
		ExtensionRegistry registry = populateNewExtensionRegistryFromExtendWith(testClass,
			context.getExtensionRegistry());

		registerBeforeAllMethodAdapters(registry);
		registerAfterAllMethodAdapters(registry);
		registerBeforeEachMethodAdapters(registry);
		registerAfterEachMethodAdapters(registry);

		context = context.extend().withExtensionRegistry(registry).build();

		ContainerExtensionContext containerExtensionContext = new ClassBasedContainerExtensionContext(
			context.getExtensionContext(), context.getExecutionListener(), this);

		// @formatter:off
		return context.extend()
				.withTestInstanceProvider(testInstanceProvider(context))
				.withExtensionContext(containerExtensionContext)
				.build();
		// @formatter:on
	}

	@Override
	public SkipResult shouldBeSkipped(JUnit5EngineExecutionContext context) throws Exception {
		ConditionEvaluationResult evaluationResult = conditionEvaluator.evaluateForContainer(
			context.getExtensionRegistry(), (ContainerExtensionContext) context.getExtensionContext());
		if (evaluationResult.isDisabled()) {
			return SkipResult.skip(evaluationResult.getReason().orElse("<unknown>"));
		}
		return SkipResult.dontSkip();
	}

	@Override
	public JUnit5EngineExecutionContext beforeAll(JUnit5EngineExecutionContext context) throws Exception {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ContainerExtensionContext extensionContext = (ContainerExtensionContext) context.getExtensionContext();

		invokeBeforeAllCallbacks(registry, extensionContext);
		invokeBeforeAllMethods(registry, extensionContext);

		return context;
	}

	@Override
	public JUnit5EngineExecutionContext afterAll(JUnit5EngineExecutionContext context) throws Exception {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ContainerExtensionContext extensionContext = (ContainerExtensionContext) context.getExtensionContext();
		ThrowableCollector throwableCollector = new ThrowableCollector();

		throwableCollector.execute(() -> invokeAfterAllMethods(registry, extensionContext, throwableCollector));
		throwableCollector.execute(() -> invokeAfterAllCallbacks(registry, extensionContext, throwableCollector));
		throwableCollector.assertEmpty();

		return context;
	}

	protected TestInstanceProvider testInstanceProvider(JUnit5EngineExecutionContext context) {
		return () -> ReflectionUtils.newInstance(testClass);
	}

	private void invokeBeforeAllCallbacks(ExtensionRegistry registry, ContainerExtensionContext context) {
		registry.stream(BeforeAllCallback.class)//
				.forEach(extension -> executeAndMaskThrowable(() -> extension.beforeAll(context)));
	}

	private void invokeBeforeAllMethods(ExtensionRegistry registry, ContainerExtensionContext context) {
		registry.stream(BeforeAllMethodAdapter.class)//
				.forEach(adapter -> executeAndMaskThrowable(() -> adapter.invoke(context)));
	}

	private void invokeAfterAllMethods(ExtensionRegistry registry, ContainerExtensionContext context,
			ThrowableCollector throwableCollector) {

		registry.reverseStream(AfterAllMethodAdapter.class)//
				.forEach(adapter -> throwableCollector.execute(() -> adapter.invoke(context)));
	}

	private void invokeAfterAllCallbacks(ExtensionRegistry registry, ContainerExtensionContext context,
			ThrowableCollector throwableCollector) {

		registry.reverseStream(AfterAllCallback.class)//
				.forEach(extension -> throwableCollector.execute(() -> extension.afterAll(context)));
	}

	private void registerBeforeAllMethodAdapters(ExtensionRegistry extensionRegistry) {
		registerAnnotatedMethodsAsExtensions(extensionRegistry, BeforeAll.class, BeforeAllMethodAdapter.class,
			this::assertStatic, this::synthesizeBeforeAllMethodAdapter);
	}

	private void registerAfterAllMethodAdapters(ExtensionRegistry extensionRegistry) {
		registerAnnotatedMethodsAsExtensions(extensionRegistry, AfterAll.class, AfterAllMethodAdapter.class,
			this::assertStatic, this::synthesizeAfterAllMethodAdapter);
	}

	private void registerBeforeEachMethodAdapters(ExtensionRegistry extensionRegistry) {
		registerAnnotatedMethodsAsExtensions(extensionRegistry, BeforeEach.class, BeforeEachMethodAdapter.class,
			this::assertNonStatic, this::synthesizeBeforeEachMethodAdapter);
	}

	private void registerAfterEachMethodAdapters(ExtensionRegistry extensionRegistry) {
		registerAnnotatedMethodsAsExtensions(extensionRegistry, AfterEach.class, AfterEachMethodAdapter.class,
			this::assertNonStatic, this::synthesizeAfterEachMethodAdapter);
	}

	private void registerAnnotatedMethodsAsExtensions(ExtensionRegistry extensionRegistry,
			Class<? extends Annotation> annotationType, Class<?> extensionType,
			BiConsumer<Class<?>, Method> methodValidator,
			BiFunction<ExtensionRegistry, Method, Extension> extensionSynthesizer) {

		// @formatter:off
		findAnnotatedMethods(testClass, annotationType, MethodSortOrder.HierarchyDown).stream()
			.peek(method -> methodValidator.accept(extensionType, method))
			.forEach(method ->
				extensionRegistry.registerExtension(extensionSynthesizer.apply(extensionRegistry, method), method));
		// @formatter:on
	}

	private BeforeAllMethodAdapter synthesizeBeforeAllMethodAdapter(ExtensionRegistry registry, Method method) {
		return extensionContext -> new MethodInvoker(extensionContext, registry).invoke(
			methodInvocationContext(null, method));
	}

	private AfterAllMethodAdapter synthesizeAfterAllMethodAdapter(ExtensionRegistry registry, Method method) {
		return extensionContext -> new MethodInvoker(extensionContext, registry).invoke(
			methodInvocationContext(null, method));
	}

	private BeforeEachMethodAdapter synthesizeBeforeEachMethodAdapter(ExtensionRegistry registry, Method method) {
		return extensionContext -> invokeMethodInTestExtensionContext(method, extensionContext, registry);
	}

	private AfterEachMethodAdapter synthesizeAfterEachMethodAdapter(ExtensionRegistry registry, Method method) {
		return extensionContext -> invokeMethodInTestExtensionContext(method, extensionContext, registry);
	}

	private void invokeMethodInTestExtensionContext(Method method, TestExtensionContext context,
			ExtensionRegistry registry) {

		Object instance = ReflectionUtils.getOuterInstance(context.getTestInstance(),
			method.getDeclaringClass()).orElseThrow(
				() -> new JUnitException("Failed to find instance for method: " + method.toGenericString()));

		new MethodInvoker(context, registry).invoke(methodInvocationContext(instance, method));
	}

	private void assertStatic(Class<?> extensionType, Method method) {
		if (!ReflectionUtils.isStatic(method)) {
			String message = String.format("Cannot register method '%s' as a(n) %s since it is not static.",
				method.getName(), extensionType.getSimpleName());
			throw new ExtensionConfigurationException(message);
		}
	}

	private void assertNonStatic(Class<?> extensionType, Method method) {
		if (ReflectionUtils.isStatic(method)) {
			String message = String.format("Cannot register method '%s' as a(n) %s since it is static.",
				method.getName(), extensionType.getSimpleName());
			throw new ExtensionConfigurationException(message);
		}
	}

}
