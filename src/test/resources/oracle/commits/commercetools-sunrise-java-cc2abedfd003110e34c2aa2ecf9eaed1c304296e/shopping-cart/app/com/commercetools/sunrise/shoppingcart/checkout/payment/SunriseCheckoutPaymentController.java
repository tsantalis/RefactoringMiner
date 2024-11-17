package com.commercetools.sunrise.shoppingcart.checkout.payment;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.contexts.UserContext;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.forms.ErrorsBean;
import com.commercetools.sunrise.common.reverserouter.CheckoutReverseRouter;
import com.commercetools.sunrise.payments.PaymentConfiguration;
import com.commercetools.sunrise.shoppingcart.CartLikeBeanFactory;
import com.commercetools.sunrise.shoppingcart.common.SunriseFrameworkCartController;
import com.commercetools.sunrise.shoppingcart.common.WithCartPreconditions;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.PaymentInfo;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.RemovePayment;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfo;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.utils.FutureUtils;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.commercetools.sunrise.common.forms.FormUtils.extractFormField;
import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static io.sphere.sdk.utils.FutureUtils.recoverWithAsync;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static play.libs.concurrent.HttpExecution.defaultContext;

@RequestScoped
public abstract class SunriseCheckoutPaymentController extends SunriseFrameworkCartController
        implements WithOverwriteableTemplateName, WithCartPreconditions {
    @Inject
    private FormFactory formFactory;
    @Inject
    private PaymentConfiguration paymentConfiguration;
    @Inject
    protected CartLikeBeanFactory cartLikeBeanFactory;

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> loadCartWithPreconditions()
                .thenComposeAsync(cart -> getPaymentMethodInfos(cart)
                        .thenComposeAsync(paymentMethodInfos -> {
                            final CheckoutPaymentPageContent pageContent = createPageContent(cart, paymentMethodInfos);
                            return asyncOk(renderCheckoutPaymentPage(cart, pageContent));
                        }, defaultContext()), defaultContext()));
    }

    @Override
    public CompletionStage<Cart> loadCartWithPreconditions() {
        return requiringExistingPrimaryCartWithLineItem();
    }

    //it returns CompletionStage maybe if some (external) fraud protection logic needs to be executed
    protected CompletionStage<List<PaymentMethodInfo>> getPaymentMethodInfos(final Cart cart) {
        return completedFuture(paymentConfiguration.getPaymentMethodInfoList());
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag) {
        return doRequest(() -> {
            final Form<DefaultCheckoutPaymentFormData> paymentForm = formFactory.form(DefaultCheckoutPaymentFormData.class).bindFromRequest();
            return loadCartWithPreconditions()
                    .thenComposeAsync(cart -> {
                        return getPaymentMethodInfos(cart).thenComposeAsync(paymentMethodInfos -> {
                            if (paymentForm.hasErrors()) {
                                return handleFormErrors(paymentForm, paymentMethodInfos, cart);
                            } else {
                                return handleValidForm(paymentForm, cart, paymentMethodInfos);
                            }
                        }, defaultContext());
                    }, defaultContext());
        });
    }

    private CompletionStage<Result> handleValidForm(final Form<DefaultCheckoutPaymentFormData> paymentForm, final Cart cart, final List<PaymentMethodInfo> paymentMethodInfos) {
        final DefaultCheckoutPaymentFormData checkoutPaymentFormData = paymentForm.get();
        final List<String> selectedMethodNames = singletonList(checkoutPaymentFormData.getPayment());
        final List<PaymentMethodInfo> selectedPaymentMethods = getSelectedPaymentMethodsInfo(selectedMethodNames, paymentMethodInfos);
        if (selectedPaymentMethods.isEmpty()) {
            return handleInvalidPaymentError(paymentForm, selectedPaymentMethods, cart);
        } else {
            final CompletionStage<Result> resultStage = setPaymentToCart(cart, selectedPaymentMethods)
                    .thenComposeAsync(updatedCart -> handleSuccessfulSetPayment(userContext()), defaultContext());
            return recoverWithAsync(resultStage, defaultContext(), throwable ->
                    handleSetPaymentToCartError(throwable, paymentForm, paymentMethodInfos, cart));
        }
    }

    protected CompletionStage<Cart> setPaymentToCart(final Cart cart, final List<PaymentMethodInfo> selectedPaymentMethods) {
        return withPaymentsToRemove(cart, selectedPaymentMethods, paymentsToRemove ->
                withPaymentsToAdd(cart, selectedPaymentMethods, paymentsToAdd -> {
                    final Stream<RemovePayment> removePaymentStream = paymentsToRemove.stream().map(RemovePayment::of);
                    final Stream<AddPayment> addPaymentStream = paymentsToAdd.stream().map(AddPayment::of);
                    final List<UpdateAction<Cart>> updateActions = Stream.concat(removePaymentStream, addPaymentStream).collect(toList());
                    return sphere().execute(CartUpdateCommand.of(cart, updateActions));
                })
        );
    }

    protected CompletionStage<Cart> withPaymentsToRemove(final Cart cart, final List<PaymentMethodInfo> selectedPaymentMethods,
                                                         final Function<List<Payment>, CompletionStage<Cart>> setPaymentAction) {
        final List<Reference<Payment>> paymentRefs = Optional.ofNullable(cart.getPaymentInfo())
                .map(PaymentInfo::getPayments)
                .orElseGet(() -> {
                    Logger.error("Payment info is not expanded in cart: the new payment information can be saved but the previous payments will not be removed.");
                    return emptyList();
                });
        final List<CompletionStage<Payment>> paymentStages = paymentRefs.stream()
                .map(paymentRef -> sphere().execute(PaymentByIdGet.of(paymentRef)))
                .collect(toList());
        return FutureUtils.listOfFuturesToFutureOfList(paymentStages)
                .thenComposeAsync(payments -> {
                    payments.removeIf(Objects::isNull);
                    final CompletionStage<Cart> updatedCartStage = setPaymentAction.apply(payments);
                    updatedCartStage.thenAccept(updatedCart ->
                            payments.forEach(payment -> sphere().execute(PaymentDeleteCommand.of(payment))));
                    return updatedCartStage;
                });
    }

    protected CompletionStage<Cart> withPaymentsToAdd(final Cart cart, final List<PaymentMethodInfo> selectedPaymentMethods,
                                                      final Function<List<Payment>, CompletionStage<Cart>> setPaymentAction) {
        final List<CompletionStage<Payment>> paymentStages = selectedPaymentMethods.stream()
                .map(selectedPaymentMethod -> {
                    final PaymentDraft paymentDraft = PaymentDraftBuilder.of(cart.getTotalPrice())
                            .paymentMethodInfo(selectedPaymentMethod)
                            .customer(Optional.ofNullable(cart.getCustomerId()).map(Customer::referenceOfId).orElse(null))
                            .build();
                    return sphere().execute(PaymentCreateCommand.of(paymentDraft));
                })
                .collect(toList());
        return FutureUtils.listOfFuturesToFutureOfList(paymentStages)
                .thenComposeAsync(payments -> {
                    payments.removeIf(Objects::isNull);
                    return setPaymentAction.apply(payments);
                });
    }

    protected List<PaymentMethodInfo> getSelectedPaymentMethodsInfo(final List<String> paymentMethod, final List<PaymentMethodInfo> allPaymentMethodInfos) {
        return allPaymentMethodInfos.stream()
                .filter(info -> info.getMethod() != null && paymentMethod.contains(info.getMethod()))
                .collect(toList());
    }

    protected CompletionStage<Result> handleSuccessfulSetPayment(final UserContext userContext) {
        final Call call = injector().getInstance(CheckoutReverseRouter.class).checkoutConfirmationPageCall(userContext.languageTag());
        return completedFuture(redirect(call));
    }

    protected CompletionStage<Result> handleFormErrors(final Form<DefaultCheckoutPaymentFormData> paymentForm,
                                                       final List<PaymentMethodInfo> paymentMethods,
                                                       final Cart cart) {
        final ErrorsBean errors = new ErrorsBean(paymentForm);
        final CheckoutPaymentPageContent pageContent = createPageContentWithPaymentError(paymentForm, errors, paymentMethods);
        return asyncBadRequest(renderCheckoutPaymentPage(cart, pageContent));
    }

    protected CompletionStage<Result> handleInvalidPaymentError(final Form<DefaultCheckoutPaymentFormData> paymentForm,
                                                                final List<PaymentMethodInfo> paymentMethods,
                                                                final Cart cart) {
        final ErrorsBean errors = new ErrorsBean("Invalid payment error"); // TODO use i18n
        final CheckoutPaymentPageContent pageContent = createPageContentWithPaymentError(paymentForm, errors, paymentMethods);
        return asyncBadRequest(renderCheckoutPaymentPage(cart, pageContent));
    }

    protected CompletionStage<Result> handleSetPaymentToCartError(final Throwable throwable,
                                                                  final Form<DefaultCheckoutPaymentFormData> paymentForm,
                                                                  final List<PaymentMethodInfo> paymentMethods,
                                                                  final Cart cart) {
        if (throwable.getCause() instanceof ErrorResponseException) {
            final ErrorResponseException errorResponseException = (ErrorResponseException) throwable.getCause();
            Logger.error("The request to set payment to cart raised an exception", errorResponseException);
            final ErrorsBean errors = new ErrorsBean("Something went wrong, please try again"); // TODO get from i18n
            final CheckoutPaymentPageContent pageContent = createPageContentWithPaymentError(paymentForm, errors, paymentMethods);
            return asyncBadRequest(renderCheckoutPaymentPage(cart, pageContent));
        }
        return exceptionallyCompletedFuture(new IllegalArgumentException(throwable));
    }

    protected CheckoutPaymentPageContent createPageContent(final Cart cart, final List<PaymentMethodInfo> paymentMethods) {
        final CheckoutPaymentPageContent pageContent = new CheckoutPaymentPageContent();
        final List<String> selectedPaymentMethods = Optional.ofNullable(cart.getPaymentInfo())
                .map(info -> info.getPayments().stream()
                        .filter(ref -> ref.getObj() != null)
                        .map(ref -> ref.getObj().getPaymentMethodInfo().getMethod())
                        .collect(toList()))
                .orElse(emptyList());
        pageContent.setPaymentForm(new CheckoutPaymentFormBean(paymentMethods, selectedPaymentMethods, userContext()));
        return pageContent;
    }

    protected CheckoutPaymentPageContent createPageContentWithPaymentError(final Form<DefaultCheckoutPaymentFormData> paymentForm,
                                                                           final ErrorsBean errors, final List<PaymentMethodInfo> paymentMethods) {
        final CheckoutPaymentPageContent pageContent = new CheckoutPaymentPageContent();
        final List<String> selectedPaymentMethodKeys = Optional.ofNullable(extractFormField(paymentForm, "payment"))
                .map(Collections::singletonList)
                .orElse(emptyList());
        final CheckoutPaymentFormBean formBean = new CheckoutPaymentFormBean(paymentMethods, selectedPaymentMethodKeys, userContext());
        formBean.setErrors(errors);
        pageContent.setPaymentForm(formBean);
        return pageContent;
    }

    protected CompletionStage<Html> renderCheckoutPaymentPage(final Cart cart, final CheckoutPaymentPageContent pageContent) {
        pageContent.setCart(cartLikeBeanFactory.create(cart));
        setI18nTitle(pageContent, "checkout:paymentPage.title");
        return renderPage(pageContent, getTemplateName());
    }

    @Override
    public String getTemplateName() {
        return "checkout-payment";
    }

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("checkout", "checkout-payment"));
    }

}
