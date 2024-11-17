package com.commercetools.sunrise.myaccount.addressbook.changeaddress;


import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.addressbook.AddressBookActionData;
import com.commercetools.sunrise.myaccount.addressbook.AddressBookAddressFormData;
import com.commercetools.sunrise.myaccount.addressbook.SunriseAddressBookManagementController;
import com.commercetools.sunrise.myaccount.addressbook.DefaultAddressBookAddressFormData;
import com.google.inject.Injector;
import io.sphere.sdk.client.ClientErrorException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultBillingAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultShippingAddress;
import io.sphere.sdk.models.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.Arrays.asList;

@RequestScoped
public abstract class SunriseChangeAddressController extends SunriseAddressBookManagementController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<AddressBookAddressFormData, AddressBookActionData, Customer> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseChangeAddressController.class);

    @Inject
    private Injector injector;

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(asList("address-book", "change-address", "address"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-edit-address";
    }

    @Override
    public Class<? extends AddressBookAddressFormData> getFormDataClass() {
        return DefaultAddressBookAddressFormData.class;
    }

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag, final String addressId) {
        return doRequest(() -> {
            logger.debug("show edit form for address with id={} in locale={}", addressId, languageTag);
            return injector.getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customerOpt -> {
                        Customer nullableCustomer = customerOpt.orElse(null);
                        Address nullableAddress = customerOpt.flatMap(customer -> findAddress(customer, addressId)).orElse(null);
                        return validateInput(nullableCustomer, nullableAddress, this::showForm);
                    }, HttpExecution.defaultContext());
        });
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag, final String addressId) {
        return doRequest(() -> {
            logger.debug("try to change address with id={} in locale={}", addressId, languageTag);
            return injector.getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customerOpt -> {
                        Customer nullableCustomer = customerOpt.orElse(null);
                        Address nullableAddress = customerOpt.flatMap(customer -> findAddress(customer, addressId)).orElse(null);
                        return validateInput(nullableCustomer, nullableAddress, this::validateForm);
                    }, HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<? extends Customer> doAction(final AddressBookAddressFormData formData, final AddressBookActionData context) {
        return changeAddress(context.getCustomer(), context.getAddress(), formData)
                .thenApplyAsync(updatedCustomer -> updatedCustomer, HttpExecution.defaultContext());
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends AddressBookAddressFormData> form, final AddressBookActionData context, final ClientErrorException clientErrorException) {
        saveUnexpectedFormError(form, clientErrorException, logger);
        return asyncBadRequest(renderPage(form, context, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final AddressBookAddressFormData formData, final AddressBookActionData context, final Customer updatedCustomer) {
        return redirectToAddressBook();
    }

    @Override
    public void fillFormData(final AddressBookAddressFormData formData, final AddressBookActionData context) {
        formData.applyAddress(context.getAddress());
        formData.setDefaultShippingAddress(isDefaultAddress(context.getAddress().getId(), context.getCustomer().getDefaultShippingAddressId()));
        formData.setDefaultBillingAddress(isDefaultAddress(context.getAddress().getId(), context.getCustomer().getDefaultBillingAddressId()));
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends AddressBookAddressFormData> form, final AddressBookActionData context, @Nullable final Customer updatedCustomer) {
        final Customer customerToRender = Optional.ofNullable(updatedCustomer).orElse(context.getCustomer());
        final ChangeAddressPageContent pageContent = injector.getInstance(ChangeAddressPageContentFactory.class).create(form, customerToRender);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    protected final CompletionStage<Result> validateInput(@Nullable final Customer nullableCustomer, @Nullable final Address nullableAddress,
                                                          final Function<AddressBookActionData, CompletionStage<Result>> onValidInput) {
        return ifValidCustomer(nullableCustomer, customer ->
                ifValidAddress(customer, nullableAddress, address ->
                        onValidInput.apply(new AddressBookActionData(customer, address))));
    }

    private CompletionStage<Customer> changeAddress(final Customer customer, final Address oldAddress, final AddressBookAddressFormData formData) {
        final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
        updateActions.add(ChangeAddress.ofOldAddressToNewAddress(oldAddress, formData.toAddress()));
        updateActions.addAll(setDefaultAddressActions(customer, oldAddress.getId(), formData));
        return sphere().execute(CustomerUpdateCommand.of(customer, updateActions));
    }

    private List<UpdateAction<Customer>> setDefaultAddressActions(final Customer customer, final String addressId, final AddressBookAddressFormData formData) {
        final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
        setDefaultAddressAction(addressId, formData.isDefaultShippingAddress(), customer.getDefaultShippingAddressId(), SetDefaultShippingAddress::of)
                .ifPresent(updateActions::add);
        setDefaultAddressAction(addressId, formData.isDefaultBillingAddress(), customer.getDefaultBillingAddressId(), SetDefaultBillingAddress::of)
                .ifPresent(updateActions::add);
        return updateActions;
    }

    private Optional<UpdateAction<Customer>> setDefaultAddressAction(final String addressId, final boolean isNewDefaultAddress,
                                                                     @Nullable final String defaultAddressId,
                                                                     final Function<String, UpdateAction<Customer>> actionCreator) {
        final boolean defaultNeedsChange = isDefaultAddressDifferent(addressId, isNewDefaultAddress, defaultAddressId);
        if (defaultNeedsChange) {
            final String addressIdToSetAsDefault = isNewDefaultAddress ? addressId : null;
            return Optional.of(actionCreator.apply(addressIdToSetAsDefault));
        }
        return Optional.empty();
    }

    private boolean isDefaultAddressDifferent(final String addressId, final boolean isNewDefaultAddress, @Nullable final String defaultAddressId) {
        return isNewDefaultAddress ^ isDefaultAddress(addressId, defaultAddressId);
    }
}