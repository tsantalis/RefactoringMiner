package pl.joegreen.lambdaFromString.classFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * <strong>This class may change between versions</strong>.
 * If you use it your code may not work with the next version of the library.
 */
public class DefaultClassFactory implements ClassFactory {

    @Override
    public Class<?> createClass(String fullClassName, String sourceCode, JavaCompiler compiler, List<String> additionalCompilerOptions, ClassLoader parentClassLoader) throws ClassCompilationException {
        try {
            Map<String, CompiledClassJavaObject> compiledClassesBytes = compileClasses(fullClassName, sourceCode, compiler, additionalCompilerOptions);
            return loadClass(fullClassName, compiledClassesBytes, parentClassLoader);
        } catch (ClassNotFoundException | RuntimeException e) {
            throw new ClassCompilationException(e);
        }
    }

    protected Class<?> loadClass(String fullClassName, Map<String, CompiledClassJavaObject> compiledClassesBytes, ClassLoader parentClassLoader) throws ClassNotFoundException {
        return (new InMemoryClassLoader(compiledClassesBytes, parentClassLoader)).loadClass(fullClassName);
    }

    protected Map<String, CompiledClassJavaObject> compileClasses(
            String fullClassName, String sourceCode, JavaCompiler compiler, List<String> additionalCompilerOptions) throws ClassCompilationException {

        ClassSourceJavaObject classSourceObject = new ClassSourceJavaObject(fullClassName, sourceCode);
        /*
         * diagnosticListener = null -> compiler's default reporting
		 * diagnostics; locale = null -> default locale to format diagnostics;
		 * charset = null -> uses platform default charset
		 */
        try (InMemoryFileManager stdFileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null))) {
            StringWriter stdErrWriter = new StringWriter();
            DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
            List<String> finalCompilerOptions = mergeStringLists(getDefaultCompilerOptions(), additionalCompilerOptions);
            JavaCompiler.CompilationTask compilationTask = compiler.getTask(stdErrWriter,
                    stdFileManager, diagnosticsCollector,
                    finalCompilerOptions, null, Collections.singletonList(classSourceObject));

            boolean status = compilationTask.call();
            if (!status) {
                throw new ClassCompilationException(
                        new CompilationDetails(fullClassName, sourceCode,
                                diagnosticsCollector.getDiagnostics(), stdErrWriter.toString()));
            }
            return stdFileManager.getClasses();
        }
    }

    protected List<String> getDefaultCompilerOptions() {
        return Arrays.asList("-target", "1.8", "-source", "1.8");
    }

    private List<String> mergeStringLists(List<String> firstList, List<String> sendList) {
        return Stream.concat(firstList.stream(), sendList.stream()).collect(Collectors.toList());
    }

}


