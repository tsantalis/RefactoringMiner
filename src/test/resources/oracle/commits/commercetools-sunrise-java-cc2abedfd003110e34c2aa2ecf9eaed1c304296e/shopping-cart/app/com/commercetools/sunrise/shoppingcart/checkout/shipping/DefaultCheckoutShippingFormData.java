package com.commercetools.sunrise.shoppingcart.checkout.shipping;

import io.sphere.sdk.models.Base;
import play.data.validation.Constraints;

public class DefaultCheckoutShippingFormData extends Base implements CheckoutShippingFormData {

    @Constraints.Required
    @Constraints.MinLength(1)
    private String shippingMethodId;

    @Override
    public String getShippingMethodId() {
        return shippingMethodId;
    }

    public void setShippingMethodId(final String shippingMethodId) {
        this.shippingMethodId = shippingMethodId;
    }
}
