package com.commercetools.sunrise.shoppingcart.checkout.confirmation;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.forms.ErrorsBean;
import com.commercetools.sunrise.common.reverserouter.CheckoutReverseRouter;
import com.commercetools.sunrise.shoppingcart.CartLikeBeanFactory;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import com.commercetools.sunrise.shoppingcart.common.WithCartPreconditions;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.Form;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sunrise.common.forms.FormUtils.extractBooleanFormField;
import static com.commercetools.sunrise.shoppingcart.CartSessionUtils.removeCartSessionData;
import static com.commercetools.sunrise.shoppingcart.OrderSessionUtils.overwriteLastOrderIdSessionData;
import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

@RequestScoped
public abstract class SunriseCheckoutConfirmationController extends SunriseFrameworkCartController
        implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<CheckoutConfirmationFormData, Cart, Order>, WithCartPreconditions {

    @Inject
    protected CartLikeBeanFactory cartLikeBeanFactory;

    @Override
    public Class<? extends CheckoutConfirmationFormData> getFormDataClass() {
        return DefaultCheckoutConfirmationFormData.class;
    }

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> loadCartWithPreconditions().thenComposeAsync(this::showForm, defaultContext()));
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag) {
        return doRequest(() -> loadCartWithPreconditions().thenComposeAsync(this::validateForm, defaultContext()));
    }

    @Override
    public CompletionStage<Cart> loadCartWithPreconditions() {
        return requiringExistingPrimaryCartWithLineItem();
    }

    @Override
    public CompletionStage<Result> showForm(final Cart cart) {
        final CheckoutConfirmationPageContent pageContent = createPageContent();
        return asyncOk(renderCheckoutConfirmationPage(cart, pageContent));
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends CheckoutConfirmationFormData> form, final Cart cart) {
        final ErrorsBean errors = new ErrorsBean(form);
        final CheckoutConfirmationPageContent pageContent = createPageContentWithConfirmationError(form, errors);
        return asyncBadRequest(renderCheckoutConfirmationPage(cart, pageContent));
    }

    @Override
    public CompletionStage<? extends Order> doAction(final CheckoutConfirmationFormData formData, final Cart cart) {
        return createOrder(cart);
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends CheckoutConfirmationFormData> form, final Cart cart, final Throwable throwable) {
        return exceptionallyCompletedFuture(throwable);
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final CheckoutConfirmationFormData formData, final Cart cart, final Order order) {
        final Call call = injector().getInstance(CheckoutReverseRouter.class).checkoutThankYouPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected PaymentState orderInitialPaymentState(final Cart cart) {
        return PaymentState.PENDING;
    }

    protected String generateOrderNumber() {
        return RandomStringUtils.randomNumeric(8);
    }

    protected CheckoutConfirmationPageContent createPageContent() {
        final CheckoutConfirmationPageContent pageContent = new CheckoutConfirmationPageContent();
        pageContent.setCheckoutForm(new CheckoutConfirmationFormBean());
        return pageContent;
    }

    protected CheckoutConfirmationPageContent createPageContentWithConfirmationError(final Form<? extends CheckoutConfirmationFormData> confirmationForm,
                                                                                     final ErrorsBean errors) {
        final CheckoutConfirmationPageContent pageContent = new CheckoutConfirmationPageContent();
        final boolean agreeToTerms = extractBooleanFormField(confirmationForm, "agreeTerms");
        final CheckoutConfirmationFormBean formBean = new CheckoutConfirmationFormBean(agreeToTerms);
        formBean.setErrors(errors);
        pageContent.setCheckoutForm(formBean);
        return pageContent;
    }

    protected CompletionStage<Html> renderCheckoutConfirmationPage(final Cart cart, final CheckoutConfirmationPageContent pageContent) {
        pageContent.setCart(cartLikeBeanFactory.create(cart));
        setI18nTitle(pageContent, "checkout:confirmationPage.title");
        return renderPage(pageContent, getTemplateName());
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("checkout", "checkout-confirmation"));
    }

    @Override
    public String getTemplateName() {
        return "checkout-confirmation";
    }

    private CompletionStage<Order> createOrder(final Cart cart) {
        final String orderNumber = generateOrderNumber();
        final OrderFromCartDraft orderDraft = OrderFromCartDraft.of(cart, orderNumber, orderInitialPaymentState(cart));
        return sphere().execute(OrderFromCartCreateCommand.of(orderDraft))
                .thenApplyAsync(order -> {
                    overwriteLastOrderIdSessionData(order, session());
                    removeCartSessionData(session());
                    return order;
                }, defaultContext());
    }
}
