package com.commercetools.sunrise.myaccount.addressbook;

import com.commercetools.sunrise.common.forms.AddressFormData;

public interface AddressBookAddressFormData extends AddressFormData {

    boolean isDefaultShippingAddress();

    boolean isDefaultBillingAddress();
}
