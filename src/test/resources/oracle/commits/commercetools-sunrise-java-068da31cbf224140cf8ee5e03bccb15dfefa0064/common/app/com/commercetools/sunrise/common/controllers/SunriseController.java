package com.commercetools.sunrise.common.controllers;

import com.commercetools.sunrise.common.cache.NoCache;
import com.commercetools.sunrise.common.contexts.ProjectContext;
import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.contexts.UserContextImpl;
import com.commercetools.sunrise.common.localization.LocalizationSelectorBean;
import com.commercetools.sunrise.common.models.FormSelectableOptionBean;
import com.commercetools.sunrise.common.pages.*;
import com.commercetools.sunrise.common.reverserouter.*;
import com.commercetools.sunrise.common.template.cms.CmsService;
import com.commercetools.sunrise.common.template.engine.TemplateEngine;
import com.commercetools.sunrise.common.template.i18n.I18nResolver;
import com.commercetools.sunrise.common.tobedeleted.ControllerDependency;
import com.commercetools.sunrise.myaccount.CustomerSessionUtils;
import com.commercetools.sunrise.shoppingcart.CartSessionUtils;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.play.controllers.ShopController;
import io.sphere.sdk.play.metrics.MetricAction;
import play.Configuration;
import play.mvc.Http;
import play.mvc.With;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * An application specific controller.
 */
@With(MetricAction.class)
@NoCache
public abstract class SunriseController extends ShopController {

    public static final String SESSION_COUNTRY = "countryCode";
    private final ControllerDependency controllerDependency;
    private final Optional<String> saleCategoryExtId;
    private final Optional<String> categoryNewExtId;
    private final boolean showInfoModal;

    @Inject
    private HomeReverseRouter homeReverseRouter;
    @Inject
    private ProductReverseRouter productReverseRouter;
    @Inject
    private CheckoutReverseRouter checkoutReverseRouter;
    @Inject
    private MyPersonalDetailsReverseRouter myPersonalDetailsReverseRouter;
    @Inject
    private CartReverseRouter cartReverseRouter;

    protected SunriseController(final ControllerDependency controllerDependency) {
        super(controllerDependency.sphere());
        this.controllerDependency = controllerDependency;
        final Configuration configuration = controllerDependency.configuration();
        this.saleCategoryExtId = Optional.ofNullable(configuration.getString("common.saleCategoryExternalId"));
        this.categoryNewExtId = Optional.ofNullable(configuration.getString("common.newCategoryExternalId"));
        this.showInfoModal = configuration.getBoolean("application.demoInfo.enabled", false);
    }

    protected final CategoryTree categoryTree() {
        return controllerDependency.categoryTree();
    }

    protected TemplateEngine templateEngine() {
        return controllerDependency.templateEngine();
    }

    protected final CmsService cmsService() {
        return controllerDependency.cmsService();
    }

    protected final I18nResolver i18nResolver() {
        return controllerDependency.i18nResolver();
    }

    protected final Configuration configuration() {
        return controllerDependency.configuration();
    }

    protected final ReverseRouter reverseRouter() {
        return controllerDependency.getReverseRouter();
    }

    protected ProjectContext projectContext() {
        return controllerDependency.projectContext();
    }

    protected final SunrisePageData pageData(final UserContext userContext, final PageContent content,
                                             final Http.Context ctx, final Http.Session session) {
        final SunrisePageData pageData = new SunrisePageData();
        pageData.setHeader(getPageHeader(userContext, content, session));
        pageData.setContent(content);
        pageData.setFooter(new PageFooter());
        pageData.setMeta(getPageMeta(userContext, ctx, session));
        return pageData;
    }

    private PageHeader getPageHeader(final UserContext userContext, final PageContent content, final Http.Session session) {
        final PageHeader pageHeader = new PageHeader(content.getTitle());
        pageHeader.setLocation(createLocalizationSelector(userContext, projectContext()));
        pageHeader.setNavMenu(new PageNavMenu(categoryTree(), userContext, productReverseRouter, saleCategoryExtId.orElse(null)));
        pageHeader.setMiniCart(CartSessionUtils.getMiniCart(session));
        pageHeader.setCustomerServiceNumber(configuration().getString("checkout.customerServiceNumber"));
        return pageHeader;
    }

    private PageMeta getPageMeta(final UserContext userContext, final Http.Context ctx, final Http.Session session) {
        final PageMeta pageMeta = new PageMeta();
        pageMeta.setUser(CustomerSessionUtils.getUserBean(session()));
        pageMeta.setAssetsPath(reverseRouter().themeAssets("").url());
        pageMeta.setBagQuantityOptions(IntStream.rangeClosed(1, 9).boxed().collect(toList()));
        pageMeta.setCsrfToken(SunriseController.getCsrfToken(ctx.session()));
        final String language = userContext.locale().getLanguage();
        pageMeta.addHalLink(homeReverseRouter.homePageCall(language), "home", "continueShopping")
                .addHalLink(reverseRouter().processSearchProductsForm(language), "search")
                .addHalLink(reverseRouter().processChangeLanguageForm(), "selectLanguage")
                .addHalLink(reverseRouter().processChangeCountryForm(language), "selectCountry")

                .addHalLink(cartReverseRouter.showCart(language), "cart")
                .addHalLink(cartReverseRouter.processAddProductToCartForm(language), "addToCart")
                .addHalLink(cartReverseRouter.processChangeLineItemQuantityForm(language), "changeLineItem")
                .addHalLink(cartReverseRouter.processDeleteLineItemForm(language), "deleteLineItem")

                .addHalLink(checkoutReverseRouter.checkoutAddressesPageCall(language), "checkout", "editShippingAddress", "editBillingAddress")
                .addHalLink(checkoutReverseRouter.checkoutAddressesProcessFormCall(language), "checkoutAddressSubmit")
                .addHalLink(checkoutReverseRouter.checkoutShippingPageCall(language), "editShippingMethod")
                .addHalLink(checkoutReverseRouter.checkoutShippingProcessFormCall(language), "checkoutShippingSubmit")
                .addHalLink(checkoutReverseRouter.checkoutPaymentPageCall(language), "editPaymentInfo")
                .addHalLink(checkoutReverseRouter.checkoutPaymentProcessFormCall(language), "checkoutPaymentSubmit")
                .addHalLink(checkoutReverseRouter.checkoutConfirmationProcessFormCall(language), "checkoutConfirmationSubmit")

                .addHalLink(reverseRouter().showLogInForm(language), "signIn", "logIn", "signUp")
                .addHalLink(reverseRouter().processLogInForm(language), "logInSubmit")
                .addHalLink(reverseRouter().processSignUpForm(language), "signUpSubmit")
                .addHalLink(reverseRouter().processLogOut(language), "logOut")

                .addHalLink(myPersonalDetailsReverseRouter.myPersonalDetailsProcessFormCall(language), "editMyPersonalDetails")

                .addHalLinkOfHrefAndRel(ctx.request().uri(), "self");
        newCategory().flatMap(nc -> productReverseRouter.productOverviewPageCall(userContext.locale(), nc))
                .ifPresent(call -> pageMeta.addHalLink(call, "newProducts"));
        pageMeta.setShowInfoModal(showInfoModal);
        return pageMeta;
    }

    protected UserContext userContext(final String languageTag) {
        final ProjectContext projectContext = projectContext();
        final Locale locale = Locale.forLanguageTag(languageTag);
        final List<Locale> acceptedLocales = acceptedLocales(locale, request(), projectContext);
        final CountryCode currentCountry = currentCountry(session(), projectContext);
        final CurrencyUnit currentCurrency = currentCurrency(currentCountry, projectContext);
        return UserContextImpl.of(acceptedLocales, currentCountry, currentCurrency);
    }

    protected Optional<Category> newCategory() {
        return categoryNewExtId.flatMap(extId -> categoryTree().findByExternalId(extId));
    }

    private static Locale currentLocale(final Locale locale, final ProjectContext projectContext) {
        return projectContext.isLocaleSupported(locale) ? locale : projectContext.defaultLocale();
    }

    public static CountryCode currentCountry(final Http.Session session, final ProjectContext projectContext) {
        final String countryCodeInSession = session.get(SESSION_COUNTRY);
        final CountryCode country = CountryCode.getByCode(countryCodeInSession, false);
        return projectContext.isCountrySupported(country) ? country : projectContext.defaultCountry();
    }

    public static CurrencyUnit currentCurrency(final CountryCode currentCountry, final ProjectContext projectContext) {
        return Optional.ofNullable(currentCountry.getCurrency())
                .map(countryCurrency -> {
                    final CurrencyUnit currency = Monetary.getCurrency(countryCurrency.getCurrencyCode());
                    return projectContext.isCurrencySupported(currency) ? currency : projectContext.defaultCurrency();
                }).orElseGet(projectContext::defaultCurrency);
    }

    public static List<Locale> acceptedLocales(@Nullable final Locale locale, final Http.Request request,
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

    @Nullable
    private static String getCsrfToken(final Http.Session session) {
        return session.get("csrfToken");
    }

    private LocalizationSelectorBean createLocalizationSelector(final UserContext userContext, final ProjectContext projectContext) {
        final LocalizationSelectorBean bean = new LocalizationSelectorBean();
        bean.setCountry(createCountryFormOptions(userContext, projectContext));
        bean.setLanguage(createLanguageFormOptions(userContext, projectContext));
        return bean;
    }

    private List<FormSelectableOptionBean> createCountryFormOptions(final UserContext userContext, final ProjectContext projectContext) {
        final List<FormSelectableOptionBean> countrySelector = projectContext.countries().stream()
                .map(countryCode -> {
                    final FormSelectableOptionBean bean = new FormSelectableOptionBean();
                    bean.setLabel(countryCode.getName());
                    bean.setValue(countryCode.getAlpha2());
                    bean.setSelected(countryCode.equals(userContext.country()));
                    return bean;
                }).collect(toList());
        return (countrySelector.size() > 1) ? countrySelector : emptyList();
    }

    private List<FormSelectableOptionBean> createLanguageFormOptions(final UserContext userContext, final ProjectContext projectContext) {
        final List<FormSelectableOptionBean> localeSelector = projectContext.locales().stream()
                .map(locale -> {
                    final FormSelectableOptionBean bean = new FormSelectableOptionBean();
                    bean.setLabel(locale.getDisplayName());
                    bean.setValue(locale.getLanguage());
                    bean.setSelected(locale.equals(userContext.locale()));
                    return bean;
                }).collect(toList());
        return (localeSelector.size() > 1) ? localeSelector : emptyList();
    }
}