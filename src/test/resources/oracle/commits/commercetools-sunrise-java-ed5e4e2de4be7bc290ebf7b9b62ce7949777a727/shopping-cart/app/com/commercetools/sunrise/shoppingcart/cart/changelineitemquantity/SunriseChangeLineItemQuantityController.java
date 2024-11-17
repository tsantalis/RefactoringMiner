package com.commercetools.sunrise.shoppingcart.cart.changelineitemquantity;

import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.shoppingcart.cart.SunriseCartManagementController;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContent;
import com.commercetools.sunrise.shoppingcart.cart.cartdetail.CartDetailPageContentFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.ChangeLineItemQuantity;
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

public abstract class SunriseChangeLineItemQuantityController extends SunriseCartManagementController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<ChangeLineItemQuantityFormData, Cart, Cart> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseChangeLineItemQuantityController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "change-line-item-quantity"));
    }

    @Override
    public String getTemplateName() {
        return "cart";
    }

    @Override
    public Class<? extends ChangeLineItemQuantityFormData> getFormDataClass() {
        return DefaultChangeLineItemQuantityFormData.class;
    }

    @RequireCSRFCheck
    public CompletionStage<Result> changeLineItemQuantity(final String languageTag) {
        return doRequest(() -> {
            logger.debug("process change line item quantity form in locale={}", languageTag);
            return getOrCreateCart()
                    .thenComposeAsync(this::validateForm, HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<? extends Cart> doAction(final ChangeLineItemQuantityFormData formData, final Cart cart) {
        return changeLineItemQuantity(cart, formData.getLineItemId(), formData.getQuantity());
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends ChangeLineItemQuantityFormData> form, final Cart cart, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, cart, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final ChangeLineItemQuantityFormData formData, final Cart cart, final Cart updatedCart) {
        overrideCartSessionData(updatedCart);
        return redirectToCartDetail();
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends ChangeLineItemQuantityFormData> form, final Cart cart, @Nullable final Cart updatedCart) {
        final Cart cartToRender = Optional.ofNullable(updatedCart).orElse(cart);
        final CartDetailPageContent pageContent = injector().getInstance(CartDetailPageContentFactory.class).create(cartToRender);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    @Override
    public void fillFormData(final ChangeLineItemQuantityFormData formData, final Cart cart) {
        // Do nothing
    }

    protected CompletionStage<Cart> changeLineItemQuantity(final Cart cart, final String lineItemId, final long quantity) {
        final ChangeLineItemQuantity changeLineItemQuantity = ChangeLineItemQuantity.of(lineItemId, quantity);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, changeLineItemQuantity);
        return executeCartUpdateCommandWithHooks(cmd);
    }
}
