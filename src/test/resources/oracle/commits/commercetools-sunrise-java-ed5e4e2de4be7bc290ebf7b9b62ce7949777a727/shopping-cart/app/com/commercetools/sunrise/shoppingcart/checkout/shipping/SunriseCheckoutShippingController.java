package com.commercetools.sunrise.shoppingcart.checkout.shipping;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.CheckoutReverseRouter;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import com.commercetools.sunrise.shoppingcart.common.WithCartPreconditions;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.SetShippingMethod;
import io.sphere.sdk.client.ClientErrorException;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseCheckoutShippingController extends SunriseFrameworkCartController
        implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<CheckoutShippingFormData, Cart, Cart>, WithCartPreconditions {

    private static final Logger logger = LoggerFactory.getLogger(SunriseCheckoutShippingController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("checkout", "checkout-shipping"));
    }

    @Override
    public String getTemplateName() {
        return "checkout-shipping";
    }

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
    public CompletionStage<? extends Cart> doAction(final CheckoutShippingFormData formData, final Cart cart) {
        return setShippingToCart(cart, formData.getShippingMethodId());
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends CheckoutShippingFormData> form, final Cart cart, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, cart, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final CheckoutShippingFormData formData, final Cart oldCart, final Cart updatedCart) {
        return redirectToCheckoutPayment();
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends CheckoutShippingFormData> form, final Cart cart, @Nullable final Cart updatedCart) {
        return getShippingMethods()
                .thenComposeAsync(shippingMethods -> {
                    final Cart cartToRender = Optional.ofNullable(updatedCart).orElse(cart);
                    final CheckoutShippingPageContent pageContent = injector().getInstance(CheckoutShippingPageContentFactory.class).create(form, cartToRender, shippingMethods);
                    return renderPageWithTemplate(pageContent, getTemplateName());
                }, HttpExecution.defaultContext());
    }

    @Override
    public void fillFormData(final CheckoutShippingFormData formData, final Cart cart) {
        final String shippingMethodId = findShippingMethodId(cart).orElse(null);
        formData.setShippingMethodId(shippingMethodId);
    }

    protected final CompletionStage<Result> redirectToCheckoutPayment() {
        final Call call = injector().getInstance(CheckoutReverseRouter.class).checkoutPaymentPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected final Optional<String> findShippingMethodId(final Cart cart) {
        return Optional.ofNullable(cart.getShippingInfo())
                .flatMap(info -> Optional.ofNullable(info.getShippingMethod()).map(Reference::getId));
    }

    private CompletionStage<Cart> setShippingToCart(final Cart cart, final String shippingMethodId) {
        final Reference<ShippingMethod> shippingMethodRef = ShippingMethod.referenceOfId(shippingMethodId);
        final SetShippingMethod setShippingMethod = SetShippingMethod.of(shippingMethodRef);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, setShippingMethod);
        return executeCartUpdateCommandWithHooks(cmd);
    }
}