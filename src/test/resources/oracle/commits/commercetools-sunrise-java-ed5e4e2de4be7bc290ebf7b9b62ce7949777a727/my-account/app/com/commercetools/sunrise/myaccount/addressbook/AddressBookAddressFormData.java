package com.commercetools.sunrise.myaccount.addressbook;

import com.commercetools.sunrise.common.forms.AddressFormData;

public interface AddressBookAddressFormData extends AddressFormData {

    boolean isDefaultShippingAddress();

    boolean isDefaultBillingAddress();

    void setDefaultShippingAddress(final boolean defaultShippingAddress);

    void setDefaultBillingAddress(final boolean defaultBillingAddress);
}
