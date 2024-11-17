package br.com.caelum.vraptor.core;

import java.lang.invoke.MethodHandle;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class MethodHandleExecutor implements MethodExecutor {
	private MethodHandle methodHandle;

	public MethodHandleExecutor(MethodHandle methodHandle) {
		super();
		this.methodHandle = methodHandle;
	}


	@Override
	public <T> T invoke(Object instance, Object... args) {
		try {				
			return (T)methodHandle.invokeExact(instance,args);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
