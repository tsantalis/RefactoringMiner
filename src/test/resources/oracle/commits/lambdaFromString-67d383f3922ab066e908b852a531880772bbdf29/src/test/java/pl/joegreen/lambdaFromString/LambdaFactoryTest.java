package pl.joegreen.lambdaFromString;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pl.joegreen.lambdaFromString.dummy.CustomInterface;
import pl.joegreen.lambdaFromString.dummy.CustomInterfaceUsingInnerClass;

import javax.tools.JavaCompiler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LambdaFactoryTest {

    public static final String INCORRECT_CODE = "()->a";

    @Parameterized.Parameters(name = "{1}")
    public static Iterable<Object[]> factories(){
        JavaCompiler jdkCompiler = JavaCompilerProvider.getJdkJavaCompiler().get();
        JavaCompiler eclipseCompiler = new EclipseCompiler();
        return Arrays.asList(new Object[][]{
                {eclipseCompiler, "Default (Eclipse Compiler)"},
                {jdkCompiler, "JDK Compiler"}
        });
    }

    private LambdaFactory factory;

    public LambdaFactoryTest(JavaCompiler defaultCompiler, String name){
        DefaultCompilerSetter.setDefaultCompiler(defaultCompiler);
        factory = LambdaFactory.get();
    }

    @Test
    public void integerIncrement() {
        Function<Integer, Integer> lambda = factory.createLambdaUnchecked(
                "i -> i+1", new TypeReference<Function<Integer, Integer>>() {});
        assertTrue(1 == lambda.apply(0));
    }

    @Test
    public void integerMultiply(){
        IntBinaryOperator lambda = factory.createLambdaUnchecked(
                "(a,b) -> a*b", new TypeReference<IntBinaryOperator>() {});
        assertEquals(1 * 2 * 3 * 4, IntStream.range(1, 5).reduce(lambda).getAsInt());
    }

    @Test
    public void integerToString() {
        Function<Integer, String> lambda = factory.createLambdaUnchecked(
                "i -> \"ABC\"+i+\"DEF\"", new TypeReference<Function<Integer, String>>() {});
        assertEquals("ABC101DEF", lambda.apply(101));
    }

    @Test
    public void useImportToAddBigDecimals() throws LambdaCreationException {
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get().withImports(BigDecimal.class));
        BiFunction<BigDecimal, BigDecimal, BigDecimal> lambda = factory.createLambda(
                "(a,b) -> a.add(b)", new TypeReference<BiFunction<BigDecimal, BigDecimal, BigDecimal>>() {});
        assertEquals(new BigDecimal("11"), lambda.apply(BigDecimal.ONE, BigDecimal.TEN));
    }

    @Test
    public void useStaticImportToIncrementBigDecimals(){
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                .withImports(BigDecimal.class)
                .withStaticImports("java.math.BigDecimal.ONE"));
        Function<BigDecimal, BigDecimal> lambda = factory.createLambdaUnchecked(
                "a -> a.add(ONE)", new TypeReference< Function<BigDecimal, BigDecimal>>() {});
        assertEquals(new BigDecimal("11"), lambda.apply(BigDecimal.TEN));
    }

    @Test
    public void usingNoArgStringConstructorAsCode(){
        Supplier<String> lambda = factory.createLambdaUnchecked(
                "String::new", new TypeReference<Supplier<String>>() {});
        String string = lambda.get();
        assertEquals("", string);
    }

    @Test
    public void creatingIntegerInsteadOfLambda(){
        Integer result = factory.createLambdaUnchecked("1+2", new TypeReference<Integer>() {});
        assertEquals(3, result.intValue());
    }

    @Test
    public void lambdaCreatingAnonymousClass(){
        String code = "() -> new Object(){ public String toString(){return \"test\";}}";
        Supplier<Object> lambda = factory.createLambdaUnchecked(code, new TypeReference<Supplier<Object>>() {});
        Object object = lambda.get();
        assertEquals("test", object.toString());
    }

    @Test
    public void lambdaCreatingMapEntry() {
        String code = "(str, num) -> new SimpleEntry<String, Long>(str, num)";
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withImports(SimpleEntry.class)
        );
        BiFunction<String, Long, SimpleEntry<String, Long>> lambda
                = factory.createLambdaUnchecked(code, new TypeReference<BiFunction<String, Long, SimpleEntry<String, Long>>>() {});
        SimpleEntry se = lambda.apply("a", 1L);
        assertEquals(new SimpleEntry<>("a", 1L), se);
    }


    @Test
    public void lambdaCreatingComplicatedGenericType() {
        String code = "() -> ( x -> new ArrayList<>())";
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withImports(SimpleEntry.class, ArrayList.class)
        );
        Supplier<Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>>> lambda =
                 factory.createLambdaUnchecked(code, new TypeReference<Supplier<Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>>>>() {});
        Function<CustomInterfaceUsingInnerClass.InnerClass[], List<SimpleEntry<?, ?>>> function = lambda.get();
        assertEquals(new ArrayList<SimpleEntry<?, ?>>(), function.apply(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void integerIncrementWithDynamicTypeReference() {
        Function<Integer, Integer> lambda = (Function<Integer, Integer>) factory.createLambdaUnchecked(
                "i -> i+1", new DynamicTypeReference("Function<Integer,Integer>"));
        assertTrue(1 == lambda.apply(0));
    }

    @Test
    public void integerMultiplyWithDynamicTypeReference(){
        IntBinaryOperator lambda = (IntBinaryOperator) factory.createLambdaUnchecked(
                "(a,b) -> a*b", new DynamicTypeReference("IntBinaryOperator"));
        assertEquals(1 * 2 * 3 * 4, IntStream.range(1, 5).reduce(lambda).getAsInt());
    }

    @Test
    public void lambdaImplementingNonStandardInterface(){
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withImports(CustomInterface.class));
        String code = " x -> 10";
        CustomInterface lambda = factory.createLambdaUnchecked(code, new TypeReference<CustomInterface>() {});
        assertEquals(lambda.customFunction("abc"), 10);
    }


    @Test
    public void lambdaImplementingBinaryClassFileOnCustomClassPath() throws Exception{
        URL classPathDirectory = this.getClass().getClassLoader().getResource("binaryFileTests/");
        URLClassLoader customClassLoader = new URLClassLoader(new URL[]{classPathDirectory});
        String classPathExtractedFromClassLoader = ClassPathExtractor.getUrlClassLoaderClassPath(customClassLoader);
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withCompilationClassPath(classPathExtractedFromClassLoader)
                        .withParentClassLoader(customClassLoader)
        );
        String code = " x -> x.toUpperCase()";
        Object lambda = factory.createLambdaUnchecked(code, new DynamicTypeReference("CustomCompiledStringMapperInterface"));
        Class<?> customInterfaceClass = customClassLoader.loadClass("CustomCompiledStringMapperInterface");
        Object uppercaseMapperImpl = customInterfaceClass.cast(lambda);
        Method mappingMethod = customInterfaceClass.getMethod("map", String.class);
        assertEquals("ALA", mappingMethod.invoke(uppercaseMapperImpl, "ala"));
    }

    @Test
    public void lambdaImplementingBinaryClassFileOnCustomClassPathContainingSpace() throws Exception{
        URL classPathDirectory = this.getClass().getClassLoader().getResource("binaryFileTests/with space/");
        URLClassLoader customClassLoader = new URLClassLoader(new URL[]{classPathDirectory});
        String classPathExtractedFromClassLoader = ClassPathExtractor.getUrlClassLoaderClassPath(customClassLoader);
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withCompilationClassPath(classPathExtractedFromClassLoader)
                        .withParentClassLoader(customClassLoader)
        );
        String code = " x -> x.toUpperCase()";
        Object lambda = factory.createLambdaUnchecked(code, new DynamicTypeReference("CustomCompiledStringMapperInterface"));
        Class<?> customInterfaceClass = customClassLoader.loadClass("CustomCompiledStringMapperInterface");
        Object uppercaseMapperImpl = customInterfaceClass.cast(lambda);
        Method mappingMethod = customInterfaceClass.getMethod("map", String.class);
        assertEquals("ALA", mappingMethod.invoke(uppercaseMapperImpl, "ala"));
    }


    @Test
    public void lambdaImplementingNonStandardInterfaceUsingInnerClass(){
        LambdaFactory factory = LambdaFactory.get(
                LambdaFactoryConfiguration.get()
                        .withImports(CustomInterfaceUsingInnerClass.class,
                                CustomInterfaceUsingInnerClass.InnerClass.class)
        );
        String code = " () -> new InnerClass()";
        CustomInterfaceUsingInnerClass lambda = factory.createLambdaUnchecked(code, new TypeReference<CustomInterfaceUsingInnerClass>() {});
        assertEquals(CustomInterfaceUsingInnerClass.InnerClass.class, lambda.createInnerClass().getClass());
    }

    @Test(expected = LambdaCreationException.class)
    public void exceptionContainsCompilationDetailsWhenCompilationFails() throws LambdaCreationException {
        try {
            factory.createLambda(INCORRECT_CODE, new TypeReference<Supplier<Integer>>() {});
        }catch (LambdaCreationException ex){
            assertTrue(ex.getCompilationDetails().isPresent());
            assertFalse(ex.getCompilationDetails().get().getDiagnostics().isEmpty());
            throw ex;
        }
    }

    @Test(expected = LambdaCreationRuntimeException.class)
    public void runtimeExceptionContainsNestedCheckedException() throws LambdaCreationException {
        try {
            factory.createLambdaUnchecked(INCORRECT_CODE, new TypeReference<Supplier<Integer>>() {});
        }catch (LambdaCreationRuntimeException ex){
            assertNotNull(ex.getNestedCheckedException());
            throw ex;
        }
    }

    @Test(expected = LambdaCreationRuntimeException.class)
    public void emptyCodeFailsWithException(){
        factory.createLambdaUnchecked("", new TypeReference<Supplier<Object>>() {});
    }

}
