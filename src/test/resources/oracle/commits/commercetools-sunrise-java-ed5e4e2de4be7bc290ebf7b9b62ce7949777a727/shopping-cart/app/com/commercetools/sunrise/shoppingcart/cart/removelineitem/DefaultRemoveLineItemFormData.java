package com.commercetools.sunrise.shoppingcart.cart.removelineitem;

import io.sphere.sdk.models.Base;
import play.data.validation.Constraints;

public class DefaultRemoveLineItemFormData extends Base implements RemoveLineItemFormData {
    @Constraints.Required
    @Constraints.MinLength(1)
    private String lineItemId;

    @Override
    public String getLineItemId() {
        return lineItemId;
    }

    public void setLineItemId(final String lineItemId) {
        this.lineItemId = lineItemId;
    }
}
