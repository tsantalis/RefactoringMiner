package com.commercetools.sunrise.shoppingcart.checkout.shipping;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.CheckoutReverseRouter;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import com.commercetools.sunrise.shoppingcart.common.WithCartPreconditions;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.SetShippingMethod;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shippingmethods.ShippingMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseCheckoutShippingController extends SunriseFrameworkCartController
        implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<CheckoutShippingFormData, Cart, Cart>, WithCartPreconditions {
    private static final Logger logger = LoggerFactory.getLogger(SunriseCheckoutShippingController.class);

    @Inject
    private CheckoutShippingPageContentFactory pageContentFactory;

    @Override
    public Class<? extends CheckoutShippingFormData> getFormDataClass() {
        return DefaultCheckoutShippingFormData.class;
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
        final String shippingMethodId = findShippingMethodId(cart).orElse(null);
        final Form<? extends CheckoutShippingFormData> filledForm = createFilledForm(shippingMethodId);
        return asyncOk(renderPage(filledForm, cart));
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends CheckoutShippingFormData> form, final Cart cart) {
        return asyncBadRequest(renderPage(form, cart));
    }

    @Override
    public CompletionStage<? extends Cart> doAction(final CheckoutShippingFormData formData, final Cart cart) {
        return setShippingToCart(cart, formData.getShippingMethodId());
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends CheckoutShippingFormData> form, final Cart cart, final Throwable throwable) {
        if (throwable.getCause() instanceof BadRequestException) {
            saveUnexpectedFormError(form, throwable.getCause(), logger);
            return asyncBadRequest(renderPage(form, cart));
        }
        return exceptionallyCompletedFuture(throwable);
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final CheckoutShippingFormData formData, final Cart oldCart, final Cart updatedCart) {
        return redirectToCheckoutPayment();
    }

    protected Form<? extends CheckoutShippingFormData> createFilledForm(@Nullable final String shippingMethodId) {
        final DefaultCheckoutShippingFormData formData = new DefaultCheckoutShippingFormData();
        formData.setShippingMethodId(shippingMethodId);
        return formFactory().form(DefaultCheckoutShippingFormData.class).fill(formData);
    }

    protected CompletionStage<Html> renderPage(final Form<? extends CheckoutShippingFormData> form, final Cart cart) {
        return getShippingMethods()
                .thenComposeAsync(shippingMethods -> {
                    final CheckoutShippingPageContent pageContent = injector().getInstance(CheckoutShippingPageContentFactory.class).create(form, cart, shippingMethods);
                    return renderPage(pageContent, getTemplateName());
                }, HttpExecution.defaultContext());
    }

    @Override
    public String getTemplateName() {
        return "checkout-shipping";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("checkout", "checkout-shipping"));
    }

    private CompletionStage<Cart> setShippingToCart(final Cart cart, final String shippingMethodId) {
        final Reference<ShippingMethod> shippingMethodRef = ShippingMethod.referenceOfId(shippingMethodId);
        final SetShippingMethod setShippingMethod = SetShippingMethod.of(shippingMethodRef);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, setShippingMethod);
        return executeCartUpdateCommandWithHooks(cmd);
    }

    private CompletionStage<Result> redirectToCheckoutPayment() {
        final Call call = injector().getInstance(CheckoutReverseRouter.class).checkoutPaymentPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    private Optional<String> findShippingMethodId(final Cart cart) {
        return Optional.ofNullable(cart.getShippingInfo())
                .flatMap(info -> Optional.ofNullable(info.getShippingMethod()).map(Reference::getId));
    }
}