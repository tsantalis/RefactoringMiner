package com.commercetools.sunrise.myaccount.authentication.login;

import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.controllers.SimpleFormBindingControllerTrait;
import com.commercetools.sunrise.common.controllers.SunriseFrameworkController;
import com.commercetools.sunrise.common.controllers.WithOverwriteableTemplateName;
import com.commercetools.sunrise.common.reverserouter.MyPersonalDetailsReverseRouter;
import com.commercetools.sunrise.common.reverserouter.ProductReverseRouter;
import com.commercetools.sunrise.myaccount.authentication.AuthenticationPageContent;
import com.commercetools.sunrise.myaccount.authentication.AuthenticationPageContentFactory;
import com.commercetools.sunrise.shoppingcart.CartSessionUtils;
import io.sphere.sdk.client.ClientErrorException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerSignInCommand;
import io.sphere.sdk.customers.errors.CustomerInvalidCredentials;
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
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@RequestScoped
public abstract class SunriseLogInController extends SunriseFrameworkController implements WithOverwriteableTemplateName, SimpleFormBindingControllerTrait<LogInFormData, Void, CustomerSignInResult> {

    private static final Logger logger = LoggerFactory.getLogger(SunriseLogInController.class);

    @Override
    public Set<String> getFrameworkTags() {
        return new HashSet<>(asList("my-account", "sign-up", "customer", "user"));
    }

    @Override
    public String getTemplateName() {
        return "my-account-login";
    }

    @Override
    public Class<? extends LogInFormData> getFormDataClass() {
        return DefaultLogInFormData.class;
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
    public CompletionStage<? extends CustomerSignInResult> doAction(final LogInFormData formData, final Void context) {
        final CustomerSignInCommand signInCommand = CustomerSignInCommand.of(formData.getUsername(), formData.getPassword(), anonymousCartId().orElse(null));
        return sphere().execute(signInCommand);
    }

    @Override
    public CompletionStage<Result> handleClientErrorFailedAction(final Form<? extends LogInFormData> form, final Void context, final ClientErrorException clientErrorException) {
        if (isInvalidCredentialsError(clientErrorException)) {
            saveFormError(form, "Invalid credentials"); // TODO i18n
        } else {
            saveUnexpectedFormError(form, clientErrorException, logger);
        }
        return asyncBadRequest(renderPage(form, context, null));
    }

    @Override
    public CompletionStage<Result> handleSuccessfulAction(final LogInFormData formData, final Void context, final CustomerSignInResult result) {
        final ProductReverseRouter productReverseRouter = injector().getInstance(ProductReverseRouter.class);
        overwriteCartSessionData(result.getCart(), session(), userContext(), productReverseRouter);
        overwriteCustomerSessionData(result.getCustomer(), session());
        return redirectToMyPersonalDetails();
    }

    @Override
    public CompletionStage<Html> renderPage(final Form<? extends LogInFormData> form, final Void context, @Nullable final CustomerSignInResult result) {
        final AuthenticationPageContent pageContent = injector().getInstance(AuthenticationPageContentFactory.class).createWithLogInForm(form);
        return renderPageWithTemplate(pageContent, getTemplateName());
    }

    @Override
    public void fillFormData(final LogInFormData formData, final Void context) {
        // Do nothing
    }

    protected final Optional<String> anonymousCartId() {
        return CartSessionUtils.getCartId(session());
    }

    protected final CompletionStage<Result> redirectToMyPersonalDetails() {
        final Call call = injector().getInstance(MyPersonalDetailsReverseRouter.class).myPersonalDetailsPageCall(userContext().languageTag());
        return completedFuture(redirect(call));
    }

    protected final boolean isInvalidCredentialsError(final ClientErrorException clientErrorException) {
        return clientErrorException instanceof ErrorResponseException
                && ((ErrorResponseException) clientErrorException).hasErrorCode(CustomerInvalidCredentials.CODE);
    }
}
