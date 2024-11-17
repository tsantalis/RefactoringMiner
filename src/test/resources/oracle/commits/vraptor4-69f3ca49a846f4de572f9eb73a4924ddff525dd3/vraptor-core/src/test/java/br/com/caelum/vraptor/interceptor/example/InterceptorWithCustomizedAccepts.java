package br.com.caelum.vraptor.interceptor.example;

import java.io.File;
import java.sql.DriverManager;

import br.com.caelum.vraptor.AfterCall;
import br.com.caelum.vraptor.AroundCall;
import br.com.caelum.vraptor.BeforeCall;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.interceptor.AcceptsWithAnnotations;
import br.com.caelum.vraptor.interceptor.CustomAcceptsFailCallback;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;

@Intercepts
@AcceptsWithAnnotations(NotLogged.class)
public class InterceptorWithCustomizedAccepts {
	
	private boolean interceptCalled;
	private boolean beforeCalled;
	private boolean afterCalled;

	@AroundCall
	public void intercept(SimpleInterceptorStack stack) {
		this.interceptCalled = true;
	}		

	@BeforeCall
	public void before() {
		this.beforeCalled = true;
	}

	@AfterCall
	public void after() {
		this.afterCalled = true;
	}

	public boolean isInterceptCalled() {
		return interceptCalled;
	}

	public boolean isBeforeCalled() {
		return beforeCalled;
	}

	public boolean isAfterCalled() {
		return afterCalled;
	}

	@CustomAcceptsFailCallback
	public void customAcceptsFailCallback() {
	}
	
	
}
