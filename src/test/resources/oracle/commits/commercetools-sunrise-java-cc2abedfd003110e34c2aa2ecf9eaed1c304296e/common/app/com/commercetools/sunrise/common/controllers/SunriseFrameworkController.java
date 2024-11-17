package com.commercetools.sunrise.common.controllers;

import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.ctp.MetricAction;
import com.commercetools.sunrise.common.pages.*;
import com.commercetools.sunrise.common.reverserouter.HomeReverseRouter;
import com.commercetools.sunrise.common.template.engine.TemplateEngine;
import com.commercetools.sunrise.common.template.i18n.I18nIdentifier;
import com.commercetools.sunrise.common.template.i18n.I18nResolver;
import com.commercetools.sunrise.framework.ControllerComponent;
import com.commercetools.sunrise.framework.MultiControllerComponentResolver;
import com.commercetools.sunrise.hooks.Hook;
import com.commercetools.sunrise.hooks.PageDataHook;
import com.commercetools.sunrise.hooks.RequestHook;
import com.commercetools.sunrise.hooks.RequestHookContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.google.inject.Injector;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Base;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecution;
import play.mvc.*;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseFrameworkController extends Controller {
    private static final Logger pageDataLoggerAsJson = LoggerFactory.getLogger(SunrisePageData.class.getName() + "Json");
    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        final SimpleModule mod = new SimpleModule("form module");
        mod.addSerializer(Form.class, new StdScalarSerializer<Form>(Form.class){
            @Override
            public void serialize(final Form value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
                gen.writeObject(value.data());
            }
        });
        mapper.registerModule(mod);
        return mapper;
    }

    private SphereClient sphere;

    @Inject
    private RequestHookContext hookContext;

    @Inject
    private void setSphereClient(final SphereClient sphereClient) {
        sphere = new HookedSphereClient(sphereClient, this);
    }

    @Inject
    private Injector injector;
    @Inject
    private UserContext userContext;
    @Inject
    private TemplateEngine templateEngine;
    @Inject
    private PageMetaFactory pageMetaFactory;
    @Inject
    private I18nResolver i18nResolver;
    private final Deque<ErrorHandler> errorHandlers = new LinkedList<>();

    protected SunriseFrameworkController() {
    }

    public abstract Set<String> getFrameworkTags();

    @Inject
    public void setMultiControllerComponents(final MultiControllerComponentResolver multiComponent, final Injector injector) {
        final List<Class<? extends ControllerComponent>> components = multiComponent.findMatchingComponents(this);
        components.forEach(clazz -> {
            final ControllerComponent instance = injector.getInstance(clazz);
            hooks().add(instance);
        });
    }

    protected final void registerControllerComponent(final ControllerComponent controllerComponent) {
        hooks().add(controllerComponent);
    }

    public SphereClient sphere() {
        return sphere;
    }

    public UserContext userContext() {
        return userContext;
    }

    public TemplateEngine templateEngine() {
        return templateEngine;
    }

    public I18nResolver i18nResolver() {
        return i18nResolver;
    }

    public FormFactory formFactory() {
        return injector.getInstance(FormFactory.class);
    }

    public Injector injector() {
        return injector;
    }

    @Nullable
    public static String getCsrfToken(final Http.Session session) {
        return session.get("csrfToken");
    }

    protected CompletionStage<Html> renderPage(final PageContent pageContent, final String templateName) {
        final SunrisePageData pageData = createPageData(pageContent);
        return hooks().allAsyncHooksCompletionStage().thenApply(unused -> {
            hooks().runVoidHook(PageDataHook.class, hook -> hook.acceptPageData(pageData));
            logFinalPageData(pageData);
            final String html = templateEngine().render(templateName, pageData, userContext.locales());
            return new Html(html);
        });
    }

    private static void logFinalPageData(final SunrisePageData pageData) {
        if (pageDataLoggerAsJson.isDebugEnabled()) {
            try {
                final ObjectWriter objectWriter = objectMapper.writer()
                        .withDefaultPrettyPrinter();
                final String formatted = objectWriter.writeValueAsString(pageData);
                pageDataLoggerAsJson.debug(formatted);
            } catch (final Exception e) {
                pageDataLoggerAsJson.error("serialization of " + pageData + " failed.", e);
            }
        }
    }

    protected final SunrisePageData createPageData(final PageContent pageContent) {
        final SunrisePageData pageData = new SunrisePageData();
        pageData.setHeader(new PageHeader(pageContent.getTitle()));
        pageData.setContent(pageContent);
        pageData.setFooter(new PageFooter());
        pageData.setMeta(pageMetaFactory.create());
        return pageData;
    }

    protected CompletionStage<Result> doRequest(final Supplier<CompletionStage<Result>> nextSupplier) {



        final Function<RequestHook, CompletionStage<?>> f = hook -> hook.onRequest(ctx());
        hooks().runAsyncHook(RequestHook.class, f);
        final CompletionStage<Result> resultCompletionStage = nextSupplier.get();
        return CompletableFutureUtils.recoverWith(resultCompletionStage, e -> {
            final Throwable usefulException = e instanceof CompletionException && e.getCause() != null
                    ? e.getCause()
                    : e;
            return errorHandlers.stream().filter(h -> h.canHandle.test(usefulException)).findFirst()
                    .map(h -> h.f.apply(usefulException))
                    .orElse(resultCompletionStage);
        }, HttpExecution.defaultContext())
                .thenApplyAsync(res -> {
                    MetricAction.logRequestData(ctx());
                    return res;
                }, HttpExecution.defaultContext());
    }

    protected void prependErrorHandler(final Predicate<Throwable> canHandle, final Function<? super Throwable, CompletionStage<Result>> f) {
        errorHandlers.addFirst(new ErrorHandler(canHandle, f));
    }

    private static final class ErrorHandler extends Base {
        final Predicate<Throwable> canHandle;
        final Function<? super Throwable, CompletionStage<Result>> f;

        public ErrorHandler(final Predicate<Throwable> canHandle, final Function<? super Throwable, CompletionStage<Result>> f) {
            this.canHandle = canHandle;
            this.f = f;
        }
    }

    protected CompletionStage<Result> asyncOk(final CompletionStage<Html> htmlCompletionStage) {
        return htmlCompletionStage.thenApplyAsync(Results::ok, HttpExecution.defaultContext());
    }

    protected CompletionStage<Result> asyncBadRequest(final CompletionStage<Html> htmlCompletionStage) {
        return htmlCompletionStage.thenApplyAsync(Results::badRequest, HttpExecution.defaultContext());
    }

    protected CompletionStage<Result> asyncInternalServerError(final CompletionStage<Html> htmlCompletionStage) {
        return htmlCompletionStage.thenApplyAsync(Results::internalServerError, HttpExecution.defaultContext());
    }

    protected void setI18nTitle(final PageContent pageContent, final String bundleWithKey) {
        pageContent.setTitle(i18nResolver.getOrEmpty(userContext().locales(), I18nIdentifier.of(bundleWithKey)));
    }

    protected final CompletionStage<Result> redirectToHome() {
        final Call call = injector().getInstance(HomeReverseRouter.class).homePageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected final RequestHookContext hooks() {
        return hookContext;
    }

    protected <R, C extends SphereRequest<R>, F extends Hook, U extends Hook> CompletionStage<R>
    executeSphereRequestWithHooks(final C baseCmd,
                                  final Class<F> filterHookClass, final BiFunction<F, C, C> fh,
                                  final Class<U> updatedHookClass, final BiFunction<U, R, CompletionStage<?>> fu) {
        final C command = hooks().runFilterHook(filterHookClass, fh, baseCmd);
        return sphere().execute(command)
                .thenApplyAsync(res -> {
                    hooks().runAsyncHook(updatedHookClass, hook -> fu.apply(hook, res));
                    return res;
                });
    }

    protected final void saveFormError(final Form<?> form, final String message) {
        form.reject(message);
    }

    protected final void saveUnexpectedFormError(final Form<?> form, final Throwable throwable, final Logger logger) {
        form.reject("Something went wrong, please try again"); // TODO i18n
        logger.error("The CTP request raised an unexpected exception", throwable);
    }
}
