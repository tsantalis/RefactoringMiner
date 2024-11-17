package com.commercetools.sunrise.myaccount.mydetails;

import io.sphere.sdk.customers.CustomerName;

public interface MyPersonalDetailsFormData {

    CustomerName toCustomerName();

    String getEmail();
}

