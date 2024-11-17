package com.commercetools.sunrise.myaccount.addressbook;

import com.commercetools.sunrise.common.reverserouter.AddressBookReverseRouter;
import com.commercetools.sunrise.myaccount.common.MyAccountController;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import play.libs.concurrent.HttpExecution;
import play.mvc.Call;
import play.mvc.Result;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseAddressBookManagementController extends MyAccountController {

    protected CompletionStage<Result> handleNotFoundAddress(final Customer customer) {
        return redirectToAddressBook();
    }

    protected CompletionStage<Result> ifValidAddress(final Customer customer, @Nullable final Address address,
                                                     final Function<Address, CompletionStage<Result>> onValidAddress) {
        return Optional.ofNullable(address)
                .map(notNullAddress -> runHookOnFoundAddress(notNullAddress)
                        .thenComposeAsync(unused -> onValidAddress.apply(address), HttpExecution.defaultContext()))
                .orElseGet(() -> handleNotFoundAddress(customer));
    }

    protected final CompletionStage<?> runHookOnFoundAddress(final Address address) {
        //return runAsyncHook(SingleCustomerHook.class, hook -> hook.onSingleCustomerLoaded(customer));
        return completedFuture(null);
    }

    protected final Optional<Address> findAddress(final Customer customer, final String addressId) {
        return customer.getAddresses().stream()
                .filter(address -> Objects.equals(address.getId(), addressId))
                .findFirst();
    }

    protected final CompletionStage<Result> redirectToAddressBook() {
        final Call call = injector().getInstance(AddressBookReverseRouter.class).addressBookCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected final boolean isDefaultAddress(final String addressId, @Nullable final String defaultAddressId) {
        return Objects.equals(defaultAddressId, addressId);
    }
}
