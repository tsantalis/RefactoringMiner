package com.commercetools.sunrise.shoppingcart.cart.removelineitem;

import com.commercetools.sunrise.common.reverserouter.CartReverseRouter;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.RemoveLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Result;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.CompletableFutureUtils.failed;
import static io.sphere.sdk.utils.FutureUtils.recoverWithAsync;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

public abstract class SunriseRemoveLineItemController extends SunriseFrameworkCartController {
    private static final Logger logger = LoggerFactory.getLogger(SunriseRemoveLineItemController.class);

    @RequireCSRFCheck
    public CompletionStage<Result> removeLineItem(final String languageTag) {
        return doRequest(() -> {
            final Form<DefaultRemoveLineItemFormData> removeLineItemForm = formFactory().form(DefaultRemoveLineItemFormData.class).bindFromRequest();
            return removeLineItemForm.hasErrors() ? handleRemoveLineItemFormErrors(removeLineItemForm) : handleValidForm(removeLineItemForm);
        });
    }

    private CompletionStage<Result> handleValidForm(final Form<DefaultRemoveLineItemFormData> removeLineItemForm) {
        return getOrCreateCart()
                .thenComposeAsync(cart -> {
                    final String lineItemId = removeLineItemForm.get().getLineItemId();
                    final CompletionStage<Result> resultStage = removeLineItem(lineItemId, cart)
                            .thenComposeAsync(updatedCart -> handleSuccessfulCartChange(updatedCart), defaultContext());
                    return recoverWithAsync(resultStage, defaultContext(), throwable ->
                            handleRemoveLineItemError(throwable, removeLineItemForm, cart));
                }, defaultContext());
    }

    protected CompletionStage<Cart> removeLineItem(final String lineItemId, final Cart cart) {
        final RemoveLineItem removeLineItem = RemoveLineItem.of(lineItemId);
        final CartUpdateCommand cmd = CartUpdateCommand.of(cart, removeLineItem);
        return executeCartUpdateCommandWithHooks(cmd);
    }

    //TODO this is duplicated
    protected CompletionStage<Result> handleSuccessfulCartChange(final Cart cart) {
        overrideCartSessionData(cart);
        return completedFuture(redirect(injector().getInstance(CartReverseRouter.class).showCart(userContext().languageTag())));
    }

    protected CompletionStage<Result> handleRemoveLineItemFormErrors(final Form<DefaultRemoveLineItemFormData> form) {
        return failed(new RuntimeException(form.toString()));//TODO handle form error
    }

    protected CompletionStage<Result> handleRemoveLineItemError(final Throwable throwable,
                                                                final Form<DefaultRemoveLineItemFormData> removeLineItemForm,
                                                                final Cart cart) {
        return failed(throwable);
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "remove-line-item-from-cart"));
    }
}
