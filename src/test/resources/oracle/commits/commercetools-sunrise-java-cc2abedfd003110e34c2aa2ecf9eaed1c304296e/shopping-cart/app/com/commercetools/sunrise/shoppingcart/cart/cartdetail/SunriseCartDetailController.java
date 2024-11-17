package com.commercetools.sunrise.shoppingcart.cart.cartdetail;

import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.filters.csrf.AddCSRFToken;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;
import static play.libs.concurrent.HttpExecution.defaultContext;

/**
 * Shows and modifies the contents of the cart.
 */
public abstract class SunriseCartDetailController extends SunriseFrameworkCartController implements WithOverwriteableTemplateName {
    private static final Logger logger = LoggerFactory.getLogger(SunriseCartDetailController.class);

    @Inject
    private CartDetailPageContentFactory pageContentFactory;

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            return getOrCreateCart()
                    .thenComposeAsync(cart -> asyncOk(renderPage(pageContentFactory.create(cart), getTemplateName())), defaultContext());
        });
    }

    @Override
    public String getTemplateName() {
        return "cart";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("cart", "cart-detail"));
    }
}
