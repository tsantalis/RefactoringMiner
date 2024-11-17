package br.com.caelum.vraptor.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import br.com.caelum.vraptor.VRaptorException;
import br.com.caelum.vraptor.cache.LRU;
import br.com.caelum.vraptor.cache.VRaptorCache;

/**
 * This class should decide what kind of dynamic invocation will be used.
 * MethodHandle or pure Reflection.
 * 
 * @author Alberto Souza
 * 
 */
// TODO unit tests
@ApplicationScoped
public class DefaultMethodExecutor implements MethodExecutor {

	private VRaptorCache<Method,MethodHandle> cache;
	private MethodHandleFactory methodHandleFactory;

	@Inject
	public DefaultMethodExecutor(@LRU(capacity=500) VRaptorCache<Method, MethodHandle> cache,
			MethodHandleFactory methodHandleFactory) {
		this.cache = cache;
		this.methodHandleFactory = methodHandleFactory;
	}

	@Deprecated
	public DefaultMethodExecutor() {
	}

	@Override
	public <T> T invoke(Method method, Object instance, Object... args) {
		MethodHandle methodHandle = cache.get(method);
		// can not use put ifAbsent because i don't want to build MH all time
		if (methodHandle == null) {
			cache.put(method,
					methodHandleFactory.create(instance.getClass(), method));
			
		}
		try {
			return (T) cache.get(method)
					.invokeExact(instance, args);
		} catch (Throwable e) {
			throw new MethodExecutorException(e);
		}
	}

}
