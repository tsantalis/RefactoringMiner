package com.commercetools.sunrise.common.forms;

import io.sphere.sdk.models.Address;

public interface AddressFormData {

    Address toAddress();

    void applyAddress(final Address address);
}
