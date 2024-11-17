package com.commercetools.sunrise.shoppingcart.checkout.confirmation;

import com.commercetools.sunrise.shoppingcart.common.CheckoutPageContent;
import play.data.Form;

public class CheckoutConfirmationPageContent extends CheckoutPageContent {

    private Form<?> checkoutForm;

    public Form<?> getCheckoutForm() {
        return checkoutForm;
    }

    public void setCheckoutForm(final Form<?> checkoutForm) {
        this.checkoutForm = checkoutForm;
    }
}
