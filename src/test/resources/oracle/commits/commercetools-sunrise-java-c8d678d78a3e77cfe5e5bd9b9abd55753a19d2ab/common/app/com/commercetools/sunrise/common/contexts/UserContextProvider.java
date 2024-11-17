package com.commercetools.sunrise.common.contexts;

import com.neovisionaries.i18n.CountryCode;
import play.mvc.Http;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.*;

import static com.commercetools.sunrise.common.localization.SunriseLocalizationController.SESSION_COUNTRY;
import static java.util.stream.Collectors.toList;

public final class UserContextProvider implements Provider<UserContext> {

    @Inject
    private Http.Context context;
    @Inject
    private ProjectContext projectContext;

    @Override
    public UserContext get() {
        final Locale locale = getLocaleInPath(context);
        final List<Locale> acceptedLocales = acceptedLocales(locale, context.request(), projectContext);
        final CountryCode currentCountry = currentCountry(context.session(), projectContext);
        final CurrencyUnit currentCurrency = currentCurrency(currentCountry, projectContext);
        return UserContextImpl.of(acceptedLocales, currentCountry, currentCurrency);
    }

    private static CountryCode currentCountry(final Http.Session session, final ProjectContext projectContext) {
        final String countryCodeInSession = session.get(SESSION_COUNTRY);
        final CountryCode country = CountryCode.getByCode(countryCodeInSession, false);
        return projectContext.isCountrySupported(country) ? country : projectContext.defaultCountry();
    }

    private static CurrencyUnit currentCurrency(final CountryCode currentCountry, final ProjectContext projectContext) {
        return Optional.ofNullable(currentCountry.getCurrency())
                .map(countryCurrency -> {
                    final CurrencyUnit currency = Monetary.getCurrency(countryCurrency.getCurrencyCode());
                    return projectContext.isCurrencySupported(currency) ? currency : projectContext.defaultCurrency();
                }).orElseGet(projectContext::defaultCurrency);
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

    private static List<Locale> acceptedLocales(@Nullable final Locale locale, final Http.Request request,
                                               final ProjectContext projectContext) {
        final ArrayList<Locale> acceptedLocales = new ArrayList<>();
        if (locale != null) {
            acceptedLocales.add(currentLocale(locale, projectContext));
        }
        acceptedLocales.addAll(request.acceptLanguages().stream()
                .map(lang -> Locale.forLanguageTag(lang.code()))
                .collect(toList()));
        acceptedLocales.addAll(projectContext.locales());
        return acceptedLocales;
    }

    private static Locale currentLocale(final Locale locale, final ProjectContext projectContext) {
        return projectContext.isLocaleSupported(locale) ? locale : projectContext.defaultLocale();
    }
}
