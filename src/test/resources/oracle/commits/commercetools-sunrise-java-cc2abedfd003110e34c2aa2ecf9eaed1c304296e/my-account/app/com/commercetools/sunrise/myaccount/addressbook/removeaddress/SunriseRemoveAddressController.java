package com.commercetools.sunrise.myaccount.addressbook.removeaddress;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.addressbook.AddressBookActionData;
import com.commercetools.sunrise.myaccount.addressbook.AddressBookManagementController;
import com.commercetools.sunrise.myaccount.addressbook.addresslist.AddressBookPageContent;
import com.commercetools.sunrise.myaccount.addressbook.addresslist.AddressBookPageContentFactory;
import com.google.inject.Injector;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.models.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;

@RequestScoped
public abstract class SunriseRemoveAddressController extends AddressBookManagementController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<RemoveAddressFormData, AddressBookActionData, Customer> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseRemoveAddressController.class);

    @Inject
    private Injector injector;
    @Inject
    private AddressBookPageContentFactory addressBookPageContentFactory;

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("address-book", "remove-address", "address"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-address-book";
    }

    @Override
    public Class<? extends RemoveAddressFormData> getFormDataClass() {
        return RemoveAddressFormData.class;
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag, final String addressId) {
        return doRequest(() -> {
            logger.debug("try to remove address with id={} in locale={}", addressId, languageTag);
            return injector.getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customerOpt -> {
                        Customer nullableCustomer = customerOpt.orElse(null);
                        Address nullableAddress = customerOpt.flatMap(customer -> findAddress(customer, addressId)).orElse(null);
                        return validateInput(nullableCustomer, nullableAddress, this::validateForm);
                    }, HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<Result> showForm(final AddressBookActionData context) {
        return redirectToAddressBook();
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends RemoveAddressFormData> form, final AddressBookActionData context) {
        return asyncBadRequest(renderPage(form, context.getCustomer()));
    }

    @Override
    public CompletionStage<? extends Customer> doAction(final RemoveAddressFormData formData, final AddressBookActionData context) {
        final RemoveAddress updateAction = RemoveAddress.of(context.getAddress());
        return sphere().execute(CustomerUpdateCommand.of(context.getCustomer(), updateAction));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final RemoveAddressFormData formData, final AddressBookActionData context, final Customer result) {
        return redirectToAddressBook();
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends RemoveAddressFormData> form, final AddressBookActionData context, final Throwable throwable) {
        if (throwable.getCause() instanceof BadRequestException) {
            saveUnexpectedFormError(form, throwable.getCause(), logger);
            return asyncBadRequest(renderPage(form, context.getCustomer()));
        }
        return exceptionallyCompletedFuture(throwable);
    }

    protected CompletionStage<Html> renderPage(final Form<? extends RemoveAddressFormData> form, final Customer customer) {
        final AddressBookPageContent pageContent = addressBookPageContentFactory.create(customer);
        return renderPage(pageContent, getTemplateName());
    }

    protected final CompletionStage<Result> validateInput(@Nullable final Customer nullableCustomer, @Nullable final Address nullableAddress,
                                                          final Function<AddressBookActionData, CompletionStage<Result>> onValidInput) {
        return ifValidCustomer(nullableCustomer, customer ->
                ifValidAddress(customer, nullableAddress, address ->
                        onValidInput.apply(new AddressBookActionData(customer, address))));
    }
}
