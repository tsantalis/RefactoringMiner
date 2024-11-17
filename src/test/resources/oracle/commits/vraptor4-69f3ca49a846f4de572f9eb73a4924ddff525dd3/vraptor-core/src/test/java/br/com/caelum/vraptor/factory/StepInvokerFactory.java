package br.com.caelum.vraptor.factory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import br.com.caelum.vraptor.cache.LRUCache;
import br.com.caelum.vraptor.interceptor.StepInvoker;
import br.com.caelum.vraptor.reflection.DefaultMethodExecutor;
import br.com.caelum.vraptor.reflection.MethodExecutor;
import br.com.caelum.vraptor.reflection.MethodHandleFactory;

public class StepInvokerFactory {

	public static StepInvoker create(){
		MethodHandleFactory methodHandleFactory = new MethodHandleFactory();
		LRUCache<Method, MethodHandle> cache = new LRUCache<Method,MethodHandle>(500);
		MethodExecutor methodExecutor = new DefaultMethodExecutor(cache,methodHandleFactory);
		return new StepInvoker(methodExecutor);		
	}
}
