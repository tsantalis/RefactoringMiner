package br.com.caelum.vraptor.interceptor;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;

import javax.enterprise.context.ApplicationScoped;

import br.com.caelum.vraptor.cache.VRaptorCache;
import br.com.caelum.vraptor.cache.VRaptorDefaultCache;

@ApplicationScoped
public class AllMethodHandles {

	private VRaptorCache<Class<? extends Annotation>,MethodHandle> cache = new VRaptorDefaultCache<>();
	
	public void add(Class<? extends Annotation> stepAnnotation,MethodHandle handle) {
		if(handle!=null){
			cache.put(stepAnnotation,handle);
		}
	}
	
	public MethodHandle get(Class<? extends Annotation> stepAnnotation){
		return cache.get(stepAnnotation);
	}

}
