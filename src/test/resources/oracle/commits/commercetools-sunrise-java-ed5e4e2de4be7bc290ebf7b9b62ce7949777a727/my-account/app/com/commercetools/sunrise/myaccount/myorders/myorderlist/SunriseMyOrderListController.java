package com.commercetools.sunrise.myaccount.myorders.myorderlist;

import com.commercetools.sunrise.common.controllers.ReverseRouter;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.ctp.ProductDataConfig;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import com.commercetools.sunrise.common.template.i18n.I18nResolver;
import com.commercetools.sunrise.hooks.OrderQueryFilterHook;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.common.MyAccountController;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;

public abstract class SunriseMyOrderListController extends MyAccountController implements WithOverwriteableTemplateName {

    private static final Logger logger = LoggerFactory.getLogger(SunriseMyOrderListController.class);

    @Inject
    protected ProductDataConfig productDataConfig;
    @Inject
    protected ReverseRouter reverseRouter;
    @Inject
    protected I18nResolver i18nResolver;
    @Inject
    protected ProductReverseRouter productReverseRouter;

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("my-orders", "my-order-list", "order"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-my-orders";
    }

    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            logger.debug("show my orders in locale={}", languageTag);
            return findCustomer().thenComposeAsync(customerOpt ->
                    ifValidCustomer(customerOpt.orElse(null), customer ->
                            findOrderList(customer).thenComposeAsync(this::showOrders, HttpExecution.defaultContext())),
                    HttpExecution.defaultContext());
        });
    }

    protected CompletionStage<Result> showOrders(final PagedQueryResult<Order> orders) {
        return asyncOk(renderPage(orders));
    }

    protected CompletionStage<Html> renderPage(final PagedQueryResult<Order> orderQueryResult) {
        final MyOrderListPageContent pageContent = injector().getInstance(MyOrderListPageContentFactory.class).create(orderQueryResult);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    protected CompletionStage<Optional<Customer>> findCustomer() {
        return injector().getInstance(CustomerFinderBySession.class).findCustomer(session());
    }

    protected CompletionStage<PagedQueryResult<Order>> findOrderList(final Customer customer) {
        return injector().getInstance(OrderListFinderByCustomerId.class).findOrderList(customer.getId(), this::runHookOnOrderQuery);
    }

    protected final OrderQuery runHookOnOrderQuery(final OrderQuery orderQuery) {
        return hooks().runFilterHook(OrderQueryFilterHook.class, (hook, query) -> hook.filterQuery(query), orderQuery);
    }
}
