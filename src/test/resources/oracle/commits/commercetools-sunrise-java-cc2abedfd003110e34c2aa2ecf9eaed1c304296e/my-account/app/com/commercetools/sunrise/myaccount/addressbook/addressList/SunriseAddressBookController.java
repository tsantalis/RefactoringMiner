package com.commercetools.sunrise.myaccount.addressbook.addresslist;


import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.common.MyAccountController;
import com.google.inject.Injector;
import io.sphere.sdk.customers.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.filters.csrf.AddCSRFToken;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Arrays.asList;

@RequestScoped
public abstract class SunriseAddressBookController extends MyAccountController implements WithOverwriteableTemplateName {

    protected static final Logger logger = LoggerFactory.getLogger(SunriseAddressBookController.class);

    @Inject
    private Injector injector;
    @Inject
    private AddressBookPageContentFactory addressBookPageContentFactory;

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("address-book"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-address-book";
    }

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            logger.debug("show address book in locale={}", languageTag);
            return injector.getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customer -> showAddressBook(customer.orElse(null)));
        });
    }

    protected CompletionStage<Result> showAddressBook(@Nullable final Customer customer) {
        return ifValidCustomer(customer, notNullCustomer -> asyncOk(renderPage(customer)));
    }

    protected CompletionStage<Html> renderPage(final Customer customer) {
        final AddressBookPageContent pageContent = addressBookPageContentFactory.create(customer);
        return renderPage(pageContent, getTemplateName());
    }
}