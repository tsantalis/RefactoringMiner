package com.commercetools.sunrise.myaccount.myorders.myorderdetail;

import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.MyOrdersReverseRouter;
import com.commercetools.sunrise.hooks.OrderQueryFilterHook;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.common.MyAccountController;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseMyOrderDetailController extends MyAccountController implements WithOverwriteableTemplateName {

    private static final Logger logger = LoggerFactory.getLogger(SunriseMyOrderDetailController.class);

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("my-orders", "my-order-detail", "order"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-my-orders-order";
    }

    public CompletionStage<Result> showByOrderNumber(final String languageTag, final String orderNumber) {
        return doRequest(() -> {
            logger.debug("show order with order number={} in locale={}", orderNumber, languageTag);
            return findCustomer().thenComposeAsync(customerOpt ->
                    ifValidCustomer(customerOpt.orElse(null), customer ->
                            findOrder(customer, orderNumber).thenComposeAsync(orderOpt ->
                                    ifValidOrder(orderOpt.orElse(null), this::showOrder), HttpExecution.defaultContext())),
                    HttpExecution.defaultContext());
        });
    }

    protected CompletionStage<Result> showOrder(final Order order) {
        return asyncOk(renderPage(order));
    }

    protected CompletionStage<Result> handleNotFoundOrder() {
        return redirectToMyOrders();
    }

    protected CompletionStage<Html> renderPage(final Order order) {
        final MyOrderDetailPageContent pageContent = injector().getInstance(MyOrderDetailPageContentFactory.class).create(order);
        return renderPage(pageContent, getTemplateName());
    }

    protected CompletionStage<Result> ifValidOrder(@Nullable final Order order,
                                                   final Function<Order, CompletionStage<Result>> onValidOrder) {
        return Optional.ofNullable(order)
                .map(notNullOrder -> runHookOnFoundOrder(notNullOrder)
                        .thenComposeAsync(unused -> onValidOrder.apply(notNullOrder), HttpExecution.defaultContext()))
                .orElseGet(this::handleNotFoundOrder);
    }

    protected CompletionStage<Optional<Customer>> findCustomer() {
        return injector().getInstance(CustomerFinderBySession.class).findCustomer(session());
    }

    protected CompletionStage<Optional<Order>> findOrder(final Customer customer, final String orderNumber) {
        final CustomerIdOrderNumberPair customerIdOrderNumberPair = new CustomerIdOrderNumberPair(customer.getId(), orderNumber);
        return injector().getInstance(OrderFinderByCustomerIdAndOrderNumber.class).findOrder(customerIdOrderNumberPair, this::runHookOnOrderQuery);
    }

    protected final OrderQuery runHookOnOrderQuery(final OrderQuery orderQuery) {
        return hooks().runFilterHook(OrderQueryFilterHook.class, (hook, query) -> hook.filterQuery(query), orderQuery);
    }

    protected final CompletionStage<?> runHookOnFoundOrder(final Order order) {
        //return runAsyncHook(SingleCustomerHook.class, hook -> hook.onSingleCustomerLoaded(customer));
        return completedFuture(null);
    }

    protected final CompletionStage<Result> redirectToMyOrders() {
        final Call call = injector().getInstance(MyOrdersReverseRouter.class).myOrderListPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }
}
