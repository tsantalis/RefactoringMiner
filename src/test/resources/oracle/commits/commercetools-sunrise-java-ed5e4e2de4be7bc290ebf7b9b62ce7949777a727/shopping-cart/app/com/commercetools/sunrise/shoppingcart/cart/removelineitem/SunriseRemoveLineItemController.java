package com.commercetools.sunrise.shoppingcart.cart.removelineitem;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.shoppingcart.cart.SunriseCartManagementController;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContent;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContentFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.RemoveLineItem;
import io.sphere.sdk.client.ClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;

public abstract class SunriseRemoveLineItemController extends SunriseCartManagementController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<RemoveLineItemFormData, Cart, Cart> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseRemoveLineItemController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "remove-line-item-from-cart"));
    }

    @Override
    public Class<? extends RemoveLineItemFormData> getFormDataClass() {
        return DefaultRemoveLineItemFormData.class;
    }

    @Override
    public String getTemplateName() {
        return "cart";
    }

    @RequireCSRFCheck
    public CompletionStage<Result> removeLineItem(final String languageTag) {
        return doRequest(() -> {
            logger.debug("process remove line item form in locale={}", languageTag);
            return getOrCreateCart()
                    .thenComposeAsync(this::validateForm, HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<? extends Cart> doAction(final RemoveLineItemFormData formData, final Cart cart) {
        return removeLineItem(formData.getLineItemId(), cart);
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends RemoveLineItemFormData> form, final Cart cart, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, cart, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final RemoveLineItemFormData formData, final Cart cart, final Cart updatedCart) {
        overrideCartSessionData(updatedCart); //TODO this is duplicated
        return redirectToCartDetail();
    }

    // TODO duplicated
    @Override
    public CompletionStage<Html> renderPage(final Form<? extends RemoveLineItemFormData> form, final Cart cart, @Nullable final Cart updatedCart) {
        final Cart cartToRender = Optional.ofNullable(updatedCart).orElse(cart);
        final CartDetailPageContent pageContent = injector().getInstance(CartDetailPageContentFactory.class).create(cartToRender);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    @Override
    public void fillFormData(final RemoveLineItemFormData formData, final Cart context) {
        // Do nothing
    }

    protected CompletionStage<Cart> removeLineItem(final String lineItemId, final Cart cart) {
        final RemoveLineItem removeLineItem = RemoveLineItem.of(lineItemId);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, removeLineItem);
        return executeCartUpdateCommandWithHooks(cmd);
    }
}
