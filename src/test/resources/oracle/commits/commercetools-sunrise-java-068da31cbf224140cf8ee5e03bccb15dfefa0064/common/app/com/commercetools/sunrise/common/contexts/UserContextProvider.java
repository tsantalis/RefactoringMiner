package com.commercetools.sunrise.common.contexts;

import com.commercetools.sunrise.common.controllers.SunriseController;
import com.neovisionaries.i18n.CountryCode;
import play.mvc.Http;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.money.CurrencyUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class UserContextProvider implements Provider<UserContext> {

    @Inject
    private Http.Context context;
    @Inject
    private ProjectContext projectContext;

    @Override
    public UserContext get() {
        final Locale locale = getLocaleInPath(context);
        final List<Locale> acceptedLocales = SunriseController.acceptedLocales(locale, context.request(), projectContext);
        final CountryCode currentCountry = SunriseController.currentCountry(context.session(), projectContext);
        final CurrencyUnit currentCurrency = SunriseController.currentCurrency(currentCountry, projectContext);
        return UserContextImpl.of(acceptedLocales, currentCountry, currentCurrency);
    }

    @Nullable
    private Locale getLocaleInPath(final Http.Context context) {
        final int i = indexOfLanguageTagInRoute(context);
        if (i >= 0) {
            final String languageTag = context.request().path().split("/")[i];
            return Locale.forLanguageTag(languageTag);
        }
        return null;
    }

    private int indexOfLanguageTagInRoute(final Http.Context context) {
        final String patternString = context.args.get("ROUTE_PATTERN").toString().replaceAll("<[^>]+>", "");//hack since splitting '$languageTag<[^/]+>' with '/' would create more words
        final List<String> strings = Arrays.asList(patternString.split("/"));
        return strings.indexOf("$languageTag");
    }
}
