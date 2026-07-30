package pl.joegreen.lambdaFromString;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.Optional;

class JavaCompilerProvider {

    static Optional<JavaCompiler> findDefaultJavaCompiler() {
        Optional<JavaCompiler> eclipseJavaCompiler = getEclipseJavaCompiler();
        Optional<JavaCompiler> jdkJavaCompiler = getJdkJavaCompiler();
        if(eclipseJavaCompiler.isPresent()){
            return eclipseJavaCompiler;
        }else if(jdkJavaCompiler.isPresent()){
            return jdkJavaCompiler;
        }else{
            return Optional.empty();
        }
    }

    static Optional<JavaCompiler> getEclipseJavaCompiler() {
        try {
            return Optional.of(new EclipseCompiler());
        } catch (NoClassDefFoundError err) {
            return Optional.empty();
        }
    }

    static Optional<JavaCompiler> getJdkJavaCompiler() {
        return Optional.ofNullable(ToolProvider.getSystemJavaCompiler());
    }
}
