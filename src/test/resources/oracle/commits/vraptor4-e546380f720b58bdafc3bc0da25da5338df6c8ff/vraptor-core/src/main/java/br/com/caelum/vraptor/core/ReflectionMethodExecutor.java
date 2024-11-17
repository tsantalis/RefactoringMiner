package br.com.caelum.vraptor.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class ReflectionMethodExecutor implements MethodExecutor{
	
	private Method method;	

	public ReflectionMethodExecutor(Method method) {
		super();
		this.method = method;
	}



	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(Object instance,Object... args) {
		try {
			return (T) method.invoke(instance, args);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
