package pl.joegreen.lambdaFromString;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pl.joegreen.lambdaFromString.classFactory.DefaultClassFactory;
import pl.joegreen.lambdaFromString.dummy.CustomInterface;
import pl.joegreen.lambdaFromString.dummy.CustomInterfaceUsingInnerClass;

import javax.tools.JavaCompiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class LambdaFactoryTest {

	public static final String INCORRECT_CODE = "()->a";

	static Stream<Arguments> jdkAndEclipse() {
		int javaVersion = DefaultClassFactory.getJavaVersion();

		if (javaVersion <= 8) {
			return Stream.of(Arguments.of(JavaCompilerProvider.getJdkJavaCompiler().get()),
					Arguments.of(new EclipseCompiler()));
		}

		return Stream.of(Arguments.of(JavaCompilerProvider.getJdkJavaCompiler().get()));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void integerIncrement(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		Function<Integer, Integer> lambda = factory.createLambdaUnchecked("i -> i+1",
				new TypeReference<Function<Integer, Integer>>() {
				});
		assertTrue(1 == lambda.apply(0));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void integerMultiply(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		IntBinaryOperator lambda = factory.createLambdaUnchecked("(a,b) -> a*b",
				new TypeReference<IntBinaryOperator>() {
				});
		assertEquals(1 * 2 * 3 * 4, IntStream.range(1, 5).reduce(lambda).getAsInt());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void integerToString(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		Function<Integer, String> lambda = factory.createLambdaUnchecked("i -> \"ABC\"+i+\"DEF\"",
				new TypeReference<Function<Integer, String>>() {
				});
		assertEquals("ABC101DEF", lambda.apply(101));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void useImportToAddBigDecimals(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory
				.get(LambdaFactoryConfiguration.get().withImports(BigDecimal.class).withJavaCompiler(jc));
		assertDoesNotThrow(() -> {
			BiFunction<BigDecimal, BigDecimal, BigDecimal> lambda = factory.createLambda("(a,b) -> a.add(b)",
					new TypeReference<BiFunction<BigDecimal, BigDecimal, BigDecimal>>() {
					});
			assertEquals(new BigDecimal("11"), lambda.apply(BigDecimal.ONE, BigDecimal.TEN));
		});
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void useStaticImportToIncrementBigDecimals(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withImports(BigDecimal.class)
				.withStaticImports("java.math.BigDecimal.ONE").withJavaCompiler(jc));
		Function<BigDecimal, BigDecimal> lambda = factory.createLambdaUnchecked("a -> a.add(ONE)",
				new TypeReference<Function<BigDecimal, BigDecimal>>() {
				});
		assertEquals(new BigDecimal("11"), lambda.apply(BigDecimal.TEN));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void usingNoArgStringConstructorAsCode(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		Supplier<String> lambda = factory.createLambdaUnchecked("String::new", new TypeReference<Supplier<String>>() {
		});
		String string = lambda.get();
		assertEquals("", string);
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void creatingIntegerInsteadOfLambda(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		Integer result = factory.createLambdaUnchecked("1+2", new TypeReference<Integer>() {
		});
		assertEquals(3, result.intValue());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaCreatingAnonymousClass(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		String code = "() -> new Object(){ public String toString(){return \"test\";}}";
		Supplier<Object> lambda = factory.createLambdaUnchecked(code, new TypeReference<Supplier<Object>>() {
		});
		Object object = lambda.get();
		assertEquals("test", object.toString());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaCreatingMapEntry(JavaCompiler jc) {
		String code = "(str, num) -> new SimpleEntry<String, Long>(str, num)";
		LambdaFactory factory = LambdaFactory
				.get(LambdaFactoryConfiguration.get().withImports(SimpleEntry.class).withJavaCompiler(jc));
		BiFunction<String, Long, SimpleEntry<String, Long>> lambda = factory.createLambdaUnchecked(code,
				new TypeReference<BiFunction<String, Long, SimpleEntry<String, Long>>>() {
				});
		SimpleEntry<?, ?> se = lambda.apply("a", 1L);
		assertEquals(new SimpleEntry<>("a", 1L), se);
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaCreatingComplicatedGenericType(JavaCompiler jc) {
		String code = "() -> ( x -> new ArrayList<>())";
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get()
				.withImports(SimpleEntry.class, ArrayList.class).withJavaCompiler(jc).withEnablePreview(true));
		Supplier<Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>>> lambda = factory
				.createLambdaUnchecked(code,
						new TypeReference<Supplier<Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>>>>() {
						});
		Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>> function = lambda.get();
		assertEquals(new ArrayList<SimpleEntry<?, ?>>(), function.apply(null));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void integerIncrementWithDynamicTypeReference(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		Object uncheckedLambda = factory.createLambdaUnchecked("i -> i+1",
				new DynamicTypeReference("Function<Integer,Integer>"));
		@SuppressWarnings("unchecked")
		Function<Integer, Integer> lambda = (Function<Integer, Integer>) uncheckedLambda;
		assertTrue(1 == lambda.apply(0));
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void integerMultiplyWithDynamicTypeReference(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		IntBinaryOperator lambda = (IntBinaryOperator) factory.createLambdaUnchecked("(a,b) -> a*b",
				new DynamicTypeReference("IntBinaryOperator"));
		assertEquals(1 * 2 * 3 * 4, IntStream.range(1, 5).reduce(lambda).getAsInt());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaImplementingNonStandardInterface(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withImports(CustomInterface.class)
				.withJavaCompiler(jc).withEnablePreview(true));
		String code = " x -> 10";
		CustomInterface lambda = factory.createLambdaUnchecked(code, new TypeReference<CustomInterface>() {
		});
		assertEquals(lambda.customFunction("abc"), 10);
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaImplementingBinaryClassFileOnCustomClassPath(JavaCompiler jc) {
		URL classPathDirectory = this.getClass().getClassLoader().getResource("binaryFileTests/");
		URLClassLoader customClassLoader = new URLClassLoader(new URL[] { classPathDirectory });
		String classPathExtractedFromClassLoader = ClassPathExtractor.getUrlClassLoaderClassPath(customClassLoader);
		LambdaFactory factory = LambdaFactory
				.get(LambdaFactoryConfiguration.get().withCompilationClassPath(classPathExtractedFromClassLoader)
						.withParentClassLoader(customClassLoader).withJavaCompiler(jc));
		String code = " x -> x.toUpperCase()";
		Object lambda = factory.createLambdaUnchecked(code,
				new DynamicTypeReference("CustomCompiledStringMapperInterface"));
		assertDoesNotThrow(() -> {
			Class<?> customInterfaceClass = customClassLoader.loadClass("CustomCompiledStringMapperInterface");
			Object uppercaseMapperImpl = customInterfaceClass.cast(lambda);
			Method mappingMethod = customInterfaceClass.getMethod("map", String.class);
			assertEquals("ALA", mappingMethod.invoke(uppercaseMapperImpl, "ala"));
		});
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaImplementingBinaryClassFileOnCustomClassPathContainingSpace(JavaCompiler jc) {
		URL classPathDirectory = this.getClass().getClassLoader().getResource("binaryFileTests/with space/");
		URLClassLoader customClassLoader = new URLClassLoader(new URL[] { classPathDirectory });
		String classPathExtractedFromClassLoader = ClassPathExtractor.getUrlClassLoaderClassPath(customClassLoader);
		LambdaFactory factory = LambdaFactory
				.get(LambdaFactoryConfiguration.get().withCompilationClassPath(classPathExtractedFromClassLoader)
						.withParentClassLoader(customClassLoader).withJavaCompiler(jc));
		String code = " x -> x.toUpperCase()";
		Object lambda = factory.createLambdaUnchecked(code,
				new DynamicTypeReference("CustomCompiledStringMapperInterface"));
		assertDoesNotThrow(() -> {
			Class<?> customInterfaceClass = customClassLoader.loadClass("CustomCompiledStringMapperInterface");
			Object uppercaseMapperImpl = customInterfaceClass.cast(lambda);
			Method mappingMethod = customInterfaceClass.getMethod("map", String.class);
			assertEquals("ALA", mappingMethod.invoke(uppercaseMapperImpl, "ala"));
		});
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void lambdaImplementingNonStandardInterfaceUsingInnerClass(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get()
				.withImports(CustomInterfaceUsingInnerClass.class, CustomInterfaceUsingInnerClass.InnerClass.class)
				.withJavaCompiler(jc).withEnablePreview(true));
		String code = " () -> new InnerClass()";
		CustomInterfaceUsingInnerClass lambda = factory.createLambdaUnchecked(code,
				new TypeReference<CustomInterfaceUsingInnerClass>() {
				});
		assertEquals(CustomInterfaceUsingInnerClass.InnerClass.class, lambda.createInnerClass().getClass());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void exceptionContainsCompilationDetailsWhenCompilationFails(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));

		LambdaCreationException ex = assertThrows(LambdaCreationException.class,
				() -> factory.createLambda(INCORRECT_CODE, new TypeReference<Supplier<Integer>>() {
				}));

		assertTrue(ex.getCompilationDetails().isPresent());
		assertFalse(ex.getCompilationDetails().get().getDiagnostics().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void runtimeExceptionContainsNestedCheckedException(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));

		LambdaCreationRuntimeException ex = assertThrows(LambdaCreationRuntimeException.class,
				() -> factory.createLambdaUnchecked(INCORRECT_CODE, new TypeReference<Supplier<Integer>>() {
				}));

		assertNotNull(ex.getNestedCheckedException());
	}

	@ParameterizedTest
	@MethodSource("jdkAndEclipse")
	void emptyCodeFailsWithException(JavaCompiler jc) {
		LambdaFactory factory = LambdaFactory.get(LambdaFactoryConfiguration.get().withJavaCompiler(jc));
		assertThrows(LambdaCreationRuntimeException.class,
				() -> factory.createLambdaUnchecked("", new TypeReference<Supplier<Object>>() {
				}));
	}
}
