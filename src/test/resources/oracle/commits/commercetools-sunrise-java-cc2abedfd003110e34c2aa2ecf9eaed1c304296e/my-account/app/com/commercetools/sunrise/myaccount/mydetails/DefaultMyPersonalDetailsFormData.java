package com.commercetools.sunrise.myaccount.mydetails;

import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.models.Base;
import play.data.validation.Constraints;

public class DefaultMyPersonalDetailsFormData extends Base implements MyPersonalDetailsFormData {

    private String title;
    @Constraints.Required
    private String firstName;
    @Constraints.Required
    private String lastName;
    @Constraints.Required
    private String email;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public CustomerName toCustomerName() {
        return CustomerName.ofTitleFirstAndLastName(title, firstName, lastName);
    }

    public void applyCustomerName(final CustomerName customerName) {
        this.title = customerName.getTitle();
        this.firstName = customerName.getFirstName();
        this.lastName = customerName.getLastName();
    }
}

