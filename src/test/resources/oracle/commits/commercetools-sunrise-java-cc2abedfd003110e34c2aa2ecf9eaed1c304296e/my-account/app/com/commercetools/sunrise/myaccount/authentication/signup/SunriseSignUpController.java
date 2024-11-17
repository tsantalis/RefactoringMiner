package com.commercetools.sunrise.myaccount.authentication.signup;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.MyPersonalDetailsReverseRouter;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import com.commercetools.sunrise.myaccount.authentication.AuthenticationPageContent;
import com.commercetools.sunrise.myaccount.authentication.AuthenticationPageContentFactory;
import com.commercetools.sunrise.shoppingcart.CartSessionUtils;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Call;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sunrise.myaccount.CustomerSessionUtils.overwriteCustomerSessionData;
import static com.commercetools.sunrise.shoppingcart.CartSessionUtils.overwriteCartSessionData;
import static io.sphere.sdk.utils.FutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@RequestScoped
public abstract class SunriseSignUpController extends SunriseFrameworkController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<SignUpFormData, Void, CustomerSignInResult> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseSignUpController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("my-account", "log-in", "customer", "user"));
    }

    @Override
    public String getTemplateName() {
        return "my-account-login";
    }

    @Override
    public Class<? extends SignUpFormData> getFormDataClass() {
        return DefaultSignUpFormData.class;
    }

    @AddCSRFToken
    public CompletionStage<Result> show(final String languageTag) {
        return doRequest(() -> {
            logger.debug("show sign up form in locale={}", languageTag);
            return showForm(null);
        });
    }

    @RequireCSRFCheck
    public CompletionStage<Result> process(final String languageTag) {
        return doRequest(() -> {
            logger.debug("process sign up form in locale={}", languageTag);
            return validateForm(null);
        });
    }

    @Override
    public CompletionStage<Result> showForm(final Void context) {
        final Form<? extends SignUpFormData> filledForm = createFilledForm();
        return asyncOk(renderPage(filledForm, context, null));
    }

    @Override
    public CompletionStage<Result> handleInvalidForm(final Form<? extends SignUpFormData> form, final Void context) {
        return asyncBadRequest(renderPage(form, context, null));
    }

    @Override
    public CompletionStage<? extends CustomerSignInResult> doAction(final SignUpFormData formData, final Void context) {
        final CustomerDraft customerDraft = formData.toCustomerDraftBuilder()
                .customerNumber(generateCustomerNumber())
                .anonymousCartId(anonymousCartId().orElse(null))
                .build();
        final CustomerCreateCommand customerCreateCommand = CustomerCreateCommand.of(customerDraft);
        return sphere().execute(customerCreateCommand);
    }

    @Override
    public CompletionStage<Result> handleFailedAction(final Form<? extends SignUpFormData> form, final Void context, final Throwable throwable) {
        if (throwable.getCause() instanceof BadRequestException) {
            saveFormError(form, "Something went wrong, probably a user with this email already exists"); // TODO i18n
            logger.error("Unknown error, probably customer already exists", throwable.getCause());
            return asyncBadRequest(renderPage(form, context, null));
        }
        return exceptionallyCompletedFuture(throwable);
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final SignUpFormData formData, final Void context, final CustomerSignInResult result) {
        final ProductReverseRouter productReverseRouter = injector().getInstance(ProductReverseRouter.class);
        overwriteCartSessionData(result.getCart(), session(), userContext(), productReverseRouter);
        overwriteCustomerSessionData(result.getCustomer(), session());
        final Call call = injector().getInstance(MyPersonalDetailsReverseRouter.class).myPersonalDetailsPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected String generateCustomerNumber() {
        return RandomStringUtils.randomNumeric(6);
    }

    protected Optional<String> anonymousCartId() {
        return CartSessionUtils.getCartId(session());
    }

    protected CompletionStage<Html> renderPage(final Form<? extends SignUpFormData> form, final Void context, @Nullable final CustomerSignInResult result) {
        final AuthenticationPageContent pageContent = injector().getInstance(AuthenticationPageContentFactory.class).createWithSignUpForm(form);
        return renderPage(pageContent, getTemplateName());
    }

    protected Form<? extends SignUpFormData> createFilledForm() {
        final DefaultSignUpFormData formData = new DefaultSignUpFormData();
        return formFactory().form(DefaultSignUpFormData.class).fill(formData);
    }
}
