package com.commercetools.sunrise.common.localization;

import com.commercetools.sunrise.common.contexts.ProjectContext;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.reverserouter.HomeReverseRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Call;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseLocalizationController extends SunriseFrameworkController {
    public static final String SESSION_COUNTRY = "countryCode";

    protected static final Logger logger = LoggerFactory.getLogger(SunriseLocalizationController.class);

    @Inject
    private FormFactory formFactory;

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("localization-controller", "country", "language"));
    }

    public CompletionStage<Result> changeLanguage() {
        final Form<LanguageFormData> languageForm = formFactory.form(LanguageFormData.class).bindFromRequest();
        final String languageTag = languageForm.hasErrors() ? defaultLanguage() : languageForm.get().getLanguage();
        logger.debug("Changed language: " + languageTag);
        return redirectToLanguage(languageTag);
    }

    public CompletionStage<Result> changeCountry(final String languageTag) {
        final Form<CountryFormData> boundForm = formFactory.form(CountryFormData.class).bindFromRequest();
        final String country = boundForm.hasErrors() ? defaultCountry() : boundForm.get().getCountry();
        session(SESSION_COUNTRY, country);
        logger.debug("Changed country: " + country);
        return redirectToHome();
    }

    protected CompletionStage<Result> redirectToLanguage(final String languageTag) {
        final Call call = injector().getInstance(HomeReverseRouter.class).homePageCall(languageTag);
        return completedFuture(redirect(call));
    }

    private String defaultLanguage() {
        final ProjectContext projectContext = injector().getInstance(ProjectContext.class);
        return projectContext.defaultLocale().toLanguageTag();
    }

    private String defaultCountry() {
        final ProjectContext projectContext = injector().getInstance(ProjectContext.class);
        return projectContext.defaultCountry().getAlpha2();
    }

}
