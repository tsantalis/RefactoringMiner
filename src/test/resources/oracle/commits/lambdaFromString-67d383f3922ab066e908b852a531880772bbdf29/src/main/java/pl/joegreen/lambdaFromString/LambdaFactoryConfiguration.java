package pl.joegreen.lambdaFromString;

import pl.joegreen.lambdaFromString.classFactory.ClassFactory;
import pl.joegreen.lambdaFromString.classFactory.DefaultClassFactory;

import javax.tools.JavaCompiler;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

public class LambdaFactoryConfiguration {
    protected static Optional<JavaCompiler> DEFAULT_COMPILER = JavaCompilerProvider.findDefaultJavaCompiler();

    private HelperClassSourceProvider helperClassSourceProvider;
    private ClassFactory classFactory;
    private List<String> staticImports;
    private List<String> imports;
    private String compilationClassPath;
    private ClassLoader parentClassLoader;
    private JavaCompiler javaCompiler;

    public static LambdaFactoryConfiguration get() {
        return new LambdaFactoryConfiguration();
    }

    private LambdaFactoryConfiguration() {
        helperClassSourceProvider = new DefaultHelperClassSourceProvider();
        classFactory = new DefaultClassFactory();
        staticImports = Collections.unmodifiableList(new ArrayList<>());
        imports = Collections.unmodifiableList(new ArrayList<>());
        compilationClassPath = ClassPathExtractor.getJavaPropertyClassPath();
        parentClassLoader = this.getClass().getClassLoader();
        javaCompiler = DEFAULT_COMPILER.orElse(null);
    }

    private LambdaFactoryConfiguration copy() {
        return new LambdaFactoryConfiguration()
                .setDefaultHelperClassSourceProvider(helperClassSourceProvider)
                .setClassFactory(classFactory)
                .setStaticImports(staticImports)
                .setImports(imports)
                .setCompilationClassPath(compilationClassPath)
                .setParentClassLoader(parentClassLoader)
                .setJavaCompiler(javaCompiler);
    }


    public HelperClassSourceProvider getDefaultHelperClassSourceProvider() {
        return helperClassSourceProvider;
    }

    public ClassFactory getClassFactory() {
        return classFactory;
    }

    public List<String> getStaticImports() {
        return staticImports;
    }

    public List<String> getImports() {
        return imports;
    }

    public String getCompilationClassPath() {
        return compilationClassPath;
    }

    public ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public JavaCompiler getJavaCompiler() {
        return javaCompiler;
    }

    /**
     * Changes helperClassSourceProvider which provides a code template for the class to be compiled. <br>
     * Should be used only in rare cases when you cannot get the exact functionality
     * you want from the library and want to change some of its inner behavior. The interface accepted by this method
     * can change between library versions and cause compilation errors if you decide to use it.
     */
    public LambdaFactoryConfiguration withHelperClassSourceProvider(HelperClassSourceProvider helperSourceProvider) {
        return copy().setDefaultHelperClassSourceProvider(helperSourceProvider);
    }

    /**
     * Changes classFactory which is responsible for compiling the helper class and loading it into the memory. <br>
     * Should be used only in rare cases when you cannot get the exact functionality
     * you want from the library and want to change some of its inner behavior. The interface accepted by this method
     * can change between library versions and cause compilation errors if you decide to use it.
     */
    public LambdaFactoryConfiguration withClassFactory(ClassFactory classFactory) {
        return copy().setClassFactory(classFactory);
    }

    /**
     * Adds static imports that will be visible in the lambda code. Each string should consist of what is
     * normally written between "import static " and ";" in the import statement. Imports can contain * wildcards.
     */
    public LambdaFactoryConfiguration withStaticImports(String... newStaticImports) {
        return copy().setStaticImports(listWithNewElements(staticImports, newStaticImports));
    }

    /**
     * Adds imports that will be visible in the lambda code. Each string should consist of what is
     * normally written between "import " and ";" in the import statement. Imports can contain * wildcards.
     */
    public LambdaFactoryConfiguration withImports(String... newImports) {
        return copy().setImports(listWithNewElements(imports, newImports));
    }

    /**
     * Adds imports that will be visible in the lambda code. To add an import with * wildcard
     * please use{@link #withImports(String...)}.
     */
    public LambdaFactoryConfiguration withImports(Class<?>... newImports) {
        String[] stringImports = Arrays.stream(newImports).map(Class::getCanonicalName).toArray(String[]::new);
        return withImports(stringImports);
    }

    /**
     * Overrides default compilation classpath (default is taken from the java.class.path system property).
     * Changing the classpath may sometimes result in a need to provide a custom parent class loader {@link #withParentClassLoader(ClassLoader)}
     * so that it can load classes provided in the given class path.
     */
    public LambdaFactoryConfiguration withCompilationClassPath(String compilationClassPath) {
        return copy().setCompilationClassPath(compilationClassPath);
    }

    /**
     * Overrides default parent class loader (by default a class loader loading this class is used).
     * Provided class loader has to be able to load all the classes used by compiled lambda expressions
     * (either itself or by calling its parent class loader).
     */
    public LambdaFactoryConfiguration withParentClassLoader(ClassLoader parentClassLoader) {
        return copy().setParentClassLoader(parentClassLoader);
    }

    /**
     * Overrides default JavaCompiler instance. <br>It can be used to force using JDK compiler when the Eclipse Compiler
     * is also available. Completely different compiler instance can also be passed but in that case it is possible
     * that some changes will have to be made in the class factory ({@link #withClassFactory(ClassFactory)}).
     */
    public LambdaFactoryConfiguration withJavaCompiler(JavaCompiler javaCompiler) {
        return copy().setJavaCompiler(javaCompiler);
    }


    private LambdaFactoryConfiguration setDefaultHelperClassSourceProvider(HelperClassSourceProvider helperClassSourceProvider) {
        this.helperClassSourceProvider = helperClassSourceProvider;
        return this;
    }

    private LambdaFactoryConfiguration setClassFactory(ClassFactory classFactory) {
        this.classFactory = classFactory;
        return this;
    }

    private LambdaFactoryConfiguration setStaticImports(List<String> staticImports) {
        this.staticImports = staticImports;
        return this;
    }

    private LambdaFactoryConfiguration setImports(List<String> imports) {
        this.imports = imports;
        return this;
    }

    private LambdaFactoryConfiguration setCompilationClassPath(String classPath) {
        this.compilationClassPath = classPath;
        return this;
    }

    private LambdaFactoryConfiguration setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
        return this;
    }

    private LambdaFactoryConfiguration setJavaCompiler(JavaCompiler javaCompiler) {
        this.javaCompiler = javaCompiler;
        return this;
    }

    private static <T> List<T> listWithNewElements(List<T> oldList, T... newElements) {
        return Collections.unmodifiableList(concat(oldList.stream(), Arrays.stream(newElements)).collect(toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaFactoryConfiguration that = (LambdaFactoryConfiguration) o;

        if (!helperClassSourceProvider.equals(that.helperClassSourceProvider)) return false;
        if (!classFactory.equals(that.classFactory)) return false;
        if (!staticImports.equals(that.staticImports)) return false;
        if (!imports.equals(that.imports)) return false;
        if (!compilationClassPath.equals(that.compilationClassPath)) return false;
        if (!parentClassLoader.equals(that.parentClassLoader)) return false;
        return javaCompiler.equals(that.javaCompiler);

    }

    @Override
    public int hashCode() {
        int result = helperClassSourceProvider.hashCode();
        result = 31 * result + classFactory.hashCode();
        result = 31 * result + staticImports.hashCode();
        result = 31 * result + imports.hashCode();
        result = 31 * result + compilationClassPath.hashCode();
        result = 31 * result + parentClassLoader.hashCode();
        result = 31 * result + javaCompiler.hashCode();
        return result;
    }

}
