package com.commercetools.sunrise.shoppingcart.checkout.thankyou;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.HomeReverseRouter;
import com.commercetools.sunrise.hooks.OrderByIdGetFilterHook;
import com.commercetools.sunrise.hooks.RequestHook;
import com.commercetools.sunrise.hooks.SingleOrderHook;
import com.commercetools.sunrise.hooks.PageDataHook;
import com.commercetools.sunrise.shoppingcart.OrderSessionUtils;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderByIdGet;
import play.mvc.Call;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.libs.concurrent.HttpExecution.defaultContext;

/**
 * Controller to show as last checkout step the confirmation of the order data.
 * By default the last order ID is taken from the cookie.
 *
 * <p id="hooks">supported hooks</p>
 * <ul>
 *     <li>{@link RequestHook}</li>
 *     <li>{@link PageDataHook}</li>
 *     <li>{@link OrderByIdGetFilterHook}</li>
 *     <li>{@link SingleOrderHook}</li>
 * </ul>
 * <p>tags</p>
 * <ul>
 *     <li>checkout-thank-you</li>
 *     <li>checkout</li>
 * </ul>
 */
@RequestScoped
public abstract class SunriseCheckoutThankYouController extends SunriseFrameworkCartController
        implements WithOverwriteableTemplateName {

    @Inject
    private CheckoutThankYouPageContentFactory pageContentFactory;

    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> findLastOrder().
                thenComposeAsync(orderOpt -> orderOpt
                        .map(this::handleFoundOrder)
                        .orElseGet(this::handleNotFoundOrder), defaultContext()));
    }

    protected CompletionStage<Optional<Order>> findLastOrder() {
        final Optional<String> lastOrderId = OrderSessionUtils.getLastOrderId(session());
        return lastOrderId
                .map(orderId -> findOrderById(orderId))
                .orElseGet(() -> completedFuture(Optional.empty()));
    }

    protected CompletionStage<Optional<Order>> findOrderById(final String orderId) {
        final OrderByIdGet baseRequest = OrderByIdGet.of(orderId).plusExpansionPaths(m -> m.paymentInfo().payments());
        final OrderByIdGet orderByIdGet = hooks().runFilterHook(OrderByIdGetFilterHook.class, (hook, getter) -> hook.filterOrderByIdGet(getter), baseRequest);
        return sphere().execute(orderByIdGet)
                .thenApplyAsync(nullableOrder -> {
                    if (nullableOrder != null) {
                        hooks().runAsyncHook(SingleOrderHook.class, hook -> hook.onSingleOrderLoaded(nullableOrder));
                    }
                    return Optional.ofNullable(nullableOrder);
                }, defaultContext());
    }

    protected CompletionStage<Result> handleFoundOrder(final Order order) {
        final CheckoutThankYouPageContent pageContent = pageContentFactory.create(order);
        return asyncOk(renderPageWithTemplate(pageContent, getTemplateName()));
    }

    protected CompletionStage<Result> handleNotFoundOrder() {
        final Call call = injector().getInstance(HomeReverseRouter.class).homePageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    @Override
    public String getTemplateName() {
        return "checkout-thankyou";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("checkout", "checkout-thank-you"));
    }
}
