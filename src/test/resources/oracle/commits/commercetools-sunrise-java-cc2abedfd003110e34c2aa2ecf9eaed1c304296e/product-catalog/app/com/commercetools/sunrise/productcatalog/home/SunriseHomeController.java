package com.commercetools.sunrise.productcatalog.home;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.pages.PageContent;
import com.commercetools.sunrise.common.reverserouter.HomeReverseRouter;
import com.commercetools.sunrise.hooks.RequestHook;
import com.commercetools.sunrise.hooks.PageDataHook;
import com.commercetools.sunrise.productcatalog.productsuggestions.ProductSuggestionsControllerComponent;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;

/**
 * Controller for the home page.
 <p>Components that may be a fit</p>
 * <ul>
 *     <li>{@link ProductSuggestionsControllerComponent}</li>
 * </ul>
 * <p id="hooks">supported hooks</p>
 * <ul>
 *     <li>{@link RequestHook}</li>
 *     <li>{@link PageDataHook}</li>
 * </ul>
 * <p>tags</p>
 * <ul>
 *     <li>home</li>
 *     <li>product-catalog</li>
 * </ul>
 */
@RequestScoped
public abstract class SunriseHomeController extends SunriseFrameworkController implements WithOverwriteableTemplateName {

    @Inject
    private HomeReverseRouter homeReverseRouter;

    @Override
    public String getTemplateName() {
        return "home";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("home", "product-catalog"));
    }

    public Result index() {
        return redirect(homeReverseRouter.homePageCall(userContext().languageTag()));
    }

    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(this::showHome);
    }

    protected CompletionStage<Result> showHome() {
        final PageContent pageContent = createPageContent();
        return asyncOk(renderPage(pageContent, getTemplateName()));
    }

    protected PageContent createPageContent() {
        final HomePageContent pageContent = new HomePageContent();
        setI18nTitle(pageContent, "catalog:home.title");
        return pageContent;
    }
}
