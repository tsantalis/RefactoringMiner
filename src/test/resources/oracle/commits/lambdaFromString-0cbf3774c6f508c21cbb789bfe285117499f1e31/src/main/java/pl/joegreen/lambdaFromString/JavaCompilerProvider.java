package pl.joegreen.lambdaFromString;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.Optional;

class JavaCompilerProvider {

	/**
	 * Obtain (if available) the default JDK compiler or, if running on JRE, the
	 * Eclipse compiler. Prioritize the JDK compiler since it works for higher Java
	 * versions as well.
	 * 
	 * @return a Java compiler
	 */
	static Optional<JavaCompiler> findDefaultJavaCompiler() {
		Optional<JavaCompiler> jdkJavaCompiler = getJdkJavaCompiler();
		Optional<JavaCompiler> eclipseJavaCompiler = getEclipseJavaCompiler();
		if (jdkJavaCompiler.isPresent()) {
			return jdkJavaCompiler;
		} else if (eclipseJavaCompiler.isPresent()) {
			return eclipseJavaCompiler;
		} else {
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
