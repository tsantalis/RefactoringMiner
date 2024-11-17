package common;

import basicauth.BasicAuthTestModule;
import categorytree.CategoryTreeTestModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import common.controllers.TestableReverseRouter;
import common.controllers.TestableSphereClient;
import ctpclient.CtpClientTestModule;
import template.TemplateTestModule;
import ctpmodels.CtpModelsTestModule;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQuery;
import org.apache.commons.lang3.RandomStringUtils;
import play.Application;
import play.Configuration;
import play.Environment;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.mvc.Controller;
import play.mvc.Http;
import productcatalog.productoverview.search.SearchConfig;
import reverserouter.ReverseRouterTestModule;
import search.SearchTestModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static common.utils.JsonUtils.readCtpObject;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

public abstract class WithSunriseApplication {
    public static final int ALLOWED_TIMEOUT = 100000;

    @FunctionalInterface
    public interface CheckedConsumer<T> {

        void apply(T t) throws Exception;
    }

    protected static void run(final Application app, final String url, final CheckedConsumer<WSRequest> test) {
        final int port = 3333;
        running(testServer(port, app), () -> {
            try {
                test.apply(WS.url("http://localhost:" + port + url));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected static <C extends Controller> void run(final Application app, final Class<C> controllerClass,
                                                    final CheckedConsumer<C> test) {
        running(app, () -> {
            try {
                test.apply(app.injector().instanceOf(controllerClass));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected static void setContext(final Http.Request request) {
        Http.Context.current.set(new Http.Context(request));
    }

    protected static Http.RequestBuilder requestBuilder() {
        return new Http.RequestBuilder().id(1L);
    }

    protected static Application app(final Module ... modules) {
        return appBuilder(modules).build();
    }

    protected static GuiceApplicationBuilder appBuilder(final Module ... modules) {
        final Module testModule = Modules.override(testModule()).with(modules);
        return new GuiceApplicationBuilder()
                .in(Environment.simple())
                .loadConfig(testConfiguration())
                .overrides(testModule);
    }

    protected static Module testModule() {
        final List<Category> categories = readCtpObject("data/categories.json", CategoryQuery.resultTypeReference()).getResults();
        return Modules.combine(
                new CtpClientTestModule(TestableSphereClient.ofEmptyResponse()),
                new TemplateTestModule(),
                new CtpModelsTestModule(),
                new BasicAuthTestModule(null),
                new ReverseRouterTestModule(new TestableReverseRouter()),
                new CategoryTreeTestModule(categories),
                new SearchTestModule(SearchConfig.of(null, null, null, null, null)));
    }

    protected static Configuration testConfiguration() {
        final Configuration configuration = Configuration.load(new Environment(Mode.TEST));
        final Map<String, Object> additionalSettings = new HashMap<>();
        additionalSettings.put("application.metrics.enabled", false);
        additionalSettings.put("application.settingsWidget.enabled", false);
        additionalSettings.put("application.auth.credentials", null);
        additionalSettings.put("play.crypto.secret", RandomStringUtils.randomAlphanumeric(5));
        return new Configuration(additionalSettings).withFallback(configuration);
    }
}