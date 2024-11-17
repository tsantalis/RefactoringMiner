package com.commercetools.sunrise.myaccount.mydetails;

import com.commercetools.sunrise.common.controllers.ReverseRouter;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.ctp.ProductDataConfig;
import com.commercetools.sunrise.common.reverserouter.MyPersonalDetailsReverseRouter;
import com.commercetools.sunrise.common.template.i18n.I18nResolver;
import com.commercetools.sunrise.myaccount.CustomerFinderBySession;
import com.commercetools.sunrise.myaccount.common.MyAccountController;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sunrise.myaccount.CustomerSessionUtils.overwriteCustomerSessionData;
import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class SunriseMyPersonalDetailsController extends MyAccountController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<MyPersonalDetailsFormData, Customer, Customer> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseMyPersonalDetailsController.class);

    @Inject
    protected ProductDataConfig productDataConfig;
    @Inject
    protected FormFactory formFactory;
    @Inject
    protected I18nResolver i18nResolver;
    @Inject
    protected Configuration configuration;
    @Inject
    protected ReverseRouter reverseRouter;//TODO framework use smaller router

    @Override
    public Set<String> getFrameworkTags() {
        final Set<String> frameworkTags = super.getFrameworkTags();
        frameworkTags.addAll(singletonList("my-personal-details"));
        return frameworkTags;
    }

    @Override
    public String getTemplateName() {
        return "my-account-personal-details";
    }

    @Override
    public Class<? extends MyPersonalDetailsFormData> getFormDataClass() {
        return DefaultMyPersonalDetailsFormData.class;
    }

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            logger.debug("show my personal details form in locale={}", languageTag);
            return injector().getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customerOpt ->
                            ifValidCustomer(customerOpt.orElse(null), this::showForm), HttpExecution.defaultContext());
        });
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag) {
        return doRequest(() -> {
            logger.debug("process my personal details form in locale={}", languageTag);
            return injector().getInstance(CustomerFinderBySession.class).findCustomer(session())
                    .thenComposeAsync(customerOpt ->
                            ifValidCustomer(customerOpt.orElse(null), this::validateForm), HttpExecution.defaultContext());
        });
    }

    @Override
    public CompletionStage<Result> showForm(final Customer customer) {
        final Form<? extends MyPersonalDetailsFormData> filledForm = createFilledForm(customer);
        return asyncOk(renderPage(filledForm, customer));
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends MyPersonalDetailsFormData> form, final Customer customer) {
        return asyncBadRequest(renderPage(form, customer));
    }

    @Override
    public CompletionStage<? extends Customer> doAction(final MyPersonalDetailsFormData formData, final Customer customer) {
        final CompletionStage<Customer> customerStage = updateCustomer(formData, customer);
        customerStage.thenAcceptAsync(updatedCustomer ->
                overwriteCustomerSessionData(updatedCustomer, session()), HttpExecution.defaultContext());
        return customerStage;
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends MyPersonalDetailsFormData> form, final Customer customer, final Throwable throwable) {
        if (throwable.getCause() instanceof BadRequestException) {
            saveUnexpectedFormError(form, throwable.getCause(), logger);
            return asyncBadRequest(renderPage(form, customer));
        }
        return exceptionallyCompletedFuture(throwable);
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final MyPersonalDetailsFormData formData, final Customer oldCustomer, final Customer updatedCustomer) {
        return redirectToMyPersonalDetails();
    }

    protected Form<? extends MyPersonalDetailsFormData> createFilledForm(final Customer customer) {
        final DefaultMyPersonalDetailsFormData formData = new DefaultMyPersonalDetailsFormData();
        formData.applyCustomerName(customer.getName());
        formData.setEmail(customer.getEmail());
        return formFactory().form(DefaultMyPersonalDetailsFormData.class).fill(formData);
    }

    protected CompletionStage<Html> renderPage(final Form<? extends MyPersonalDetailsFormData> form, final Customer customer) {
        final MyPersonalDetailsPageContent pageContent = injector().getInstance(MyPersonalDetailsPageContentFactory.class).create(form, customer);
        return renderPage(pageContent, getTemplateName());
    }

    protected CompletionStage<Customer> updateCustomer(final MyPersonalDetailsFormData formData, final Customer customer) {
        final List<UpdateAction<Customer>> updateActions = buildUpdateActions(formData, customer);
        if (!updateActions.isEmpty()) {
            return sphere().execute(CustomerUpdateCommand.of(customer, updateActions));
        } else {
            return completedFuture(customer);
        }
    }

    protected List<UpdateAction<Customer>> buildUpdateActions(final MyPersonalDetailsFormData formData, final Customer customer) {
        final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
        final CustomerName customerName = formData.toCustomerName();
        if (!Objects.equals(customer.getTitle(), customerName.getTitle())) {
            updateActions.add(SetTitle.of(customerName.getTitle()));
        }
        if (!Objects.equals(customer.getFirstName(), customerName.getFirstName())) {
            updateActions.add(SetFirstName.of(customerName.getFirstName()));
        }
        if (!Objects.equals(customer.getLastName(), customerName.getLastName())) {
            updateActions.add(SetLastName.of(customerName.getLastName()));
        }
        if (!Objects.equals(customer.getEmail(), formData.getEmail())) {
            updateActions.add(ChangeEmail.of(formData.getEmail()));
        }
        return updateActions;
    }

    protected final CompletionStage<Result> redirectToMyPersonalDetails() {
        final Call call = injector().getInstance(MyPersonalDetailsReverseRouter.class).myPersonalDetailsPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }
}
