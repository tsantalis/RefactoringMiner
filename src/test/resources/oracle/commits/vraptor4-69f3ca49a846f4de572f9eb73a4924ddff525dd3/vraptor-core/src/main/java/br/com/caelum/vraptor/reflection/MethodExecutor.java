package br.com.caelum.vraptor.reflection;

import java.lang.reflect.Method;

/**
 * Interface to abstract how we invoke methods. With reflection
 * API or Method Handle
 * @author Alberto Souza and Rodrigo Ferreira
 *
 */
public interface MethodExecutor {

	public <T> T invoke(Method method,Object instance,Object...args);
}
