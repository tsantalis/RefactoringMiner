package com.commercetools.sunrise.shoppingcart.cart.addtocart;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContent;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContentFactory;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.client.ClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseAddProductToCartController extends SunriseFrameworkCartController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<AddProductToCartFormData, Cart, Cart> {
    private static final Logger logger = LoggerFactory.getLogger(SunriseAddProductToCartController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "add-line-item-to-cart"));
    }

    @Override
    public String getTemplateName() {
        return "cart";
    }

    @Override
    public Class<? extends AddProductToCartFormData> getFormDataClass() {
        return DefaultAddProductToCartFormData.class;
    }

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
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends AddProductToCartFormData> form, final Cart cart, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, cart, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final AddProductToCartFormData formData, final Cart context, final Cart result) {
        overrideCartSessionData(result);
        return successfulResult();
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends AddProductToCartFormData> form, final Cart cart, @Nullable final Cart updatedCart) {
        final CartDetailPageContent pageContent = injector().getInstance(CartDetailPageContentFactory.class).create(cart);
        return renderPageWithTemplate(pageContent, getTemplateName()); // TODO abstract results better instead of forcing HTML, to support this use case properly
    }

    @Override
    public void fillFormData(final AddProductToCartFormData formData, final Cart context) {
        // Do nothing
    }

    protected abstract CompletableFuture<Result> successfulResult();
}
