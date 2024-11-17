package com.commercetools.sunrise.shoppingcart.cart.addtocart;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Result;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseAddProductToCartController extends SunriseFrameworkCartController implements SimpleFormBindingControllerTrait<AddProductToCartFormData, Cart, Cart> {
    private static final Logger logger = LoggerFactory.getLogger(SunriseAddProductToCartController.class);

    @SuppressWarnings("unused")
    @RequireCSRFCheck
    public CompletionStage<Result> addProductToCart(final String languageTag) {
        return doRequest(() -> getOrCreateCart().thenComposeAsync(this::validateForm, defaultContext()));
    }

    @Override
    public CompletionStage<? extends Cart> doAction(final AddProductToCartFormData formData, final Cart cart) {
        final String productId = formData.getProductId();
        final int variantId = formData.getVariantId();
        final long quantity = formData.getQuantity();
        final AddLineItem updateAction = AddLineItem.of(productId, variantId, quantity);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, updateAction);
        return executeCartUpdateCommandWithHooks(cmd);
    }

    @Override
    public CompletionStage<Result> showForm(final Cart context) {
        throw new UnsupportedOperationException("need to implement SunriseAddProductToCartController.showForm(final Cart context)");
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends AddProductToCartFormData> form, final Cart context) {
        throw new UnsupportedOperationException("need to implement SunriseAddProductToCartController.handleInvalidForm");
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends AddProductToCartFormData> form, final Cart context, final Throwable throwable) {
        throw new UnsupportedOperationException("need to implement SunriseAddProductToCartController.handleFailedAction");
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final AddProductToCartFormData formData, final Cart context, final Cart result) {
        overrideCartSessionData(result);
        return successfulResult();
    }

    @Override
    public Class<? extends AddProductToCartFormData> getFormDataClass() {
        return DefaultAddProductToCartFormData.class;
    }

    protected abstract CompletableFuture<Result> successfulResult();

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "add-line-item-to-cart"));
    }
}
