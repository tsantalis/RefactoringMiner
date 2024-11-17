package br.com.caelum.vraptor.core;

/**
 * Interface to abstract how we invoke methods. With reflection
 * API or Method Handle
 * @author Alberto Souza and Rodrigo Ferreira
 *
 */
public interface MethodExecutor {

	public <T> T invoke(Object instance,Object...args);
}
