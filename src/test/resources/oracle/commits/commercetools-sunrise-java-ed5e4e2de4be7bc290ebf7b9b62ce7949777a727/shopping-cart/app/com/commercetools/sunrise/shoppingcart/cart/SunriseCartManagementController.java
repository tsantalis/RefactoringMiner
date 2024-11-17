package com.commercetools.sunrise.shoppingcart.cart;

import com.commercetools.sunrise.common.reverserouter.CartReverseRouter;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import play.mvc.Call;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseCartManagementController extends SunriseFrameworkCartController {

    protected final CompletionStage<Result> redirectToCartDetail() {
        final Call call = injector().getInstance(CartReverseRouter.class).showCart(userContext().languageTag());
        return completedFuture(redirect(call));
    }
}
