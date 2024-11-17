package com.commercetools.sunrise.common.contexts;

import com.commercetools.sunrise.hooks.HookContext;
import com.commercetools.sunrise.hooks.HookContextImpl;
import com.google.inject.AbstractModule;
import play.mvc.Http;

import javax.inject.Singleton;

/**
 * Module to enable request scoped dependency injection of the HTTP context of play.
 */
public class ContextDataModule extends AbstractModule {

    @Override
    protected void configure() {
        final RequestScope requestScope = new RequestScope();
        bindScope(RequestScoped.class, requestScope);
        bind(Http.Context.class).toProvider(HttpContextProvider.class).in(requestScope);
        bind(UserContext.class).toProvider(UserContextProvider.class).in(requestScope);
        bind(RequestContext.class).toProvider(RequestContextProvider.class).in(requestScope);
        bind(ProjectContext.class).toProvider(ProjectContextProvider.class).in(Singleton.class);
        bind(HookContext.class).to(HookContextImpl.class).in(requestScope);
    }
}
