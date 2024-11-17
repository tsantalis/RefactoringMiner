package com.commercetools.sunrise.common.controllers;

import play.data.Form;
import play.libs.concurrent.HttpExecution;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.FutureUtils.recoverWithAsync;

/**
 * Approach to handle form data (Template Method Pattern).
 * @param <F> stereotype of the in a form wrapped class
 * @param <T> type of the context of the form, possibly a parameter object
 * @param <R> the type of the updated object if the form is valid
 */
public interface SimpleFormBindingControllerTrait<F, T, R> extends FormBindingTrait<F> {

    CompletionStage<Result> showForm(final T context);

    default CompletionStage<Result> validateForm(final T context) {
        return bindForm().thenComposeAsync(form -> {
            if (!form.hasErrors()) {
                return handleValidForm(form, context);
            } else {
                return handleInvalidForm(form, context);
            }
        }, HttpExecution.defaultContext());
    }

    CompletionStage<Result> handleInvalidForm(final Form<? extends F> form, final T context);

    default CompletionStage<Result> handleValidForm(final Form<? extends F> form, final T context) {
        final CompletionStage<Result> resultStage = doAction(form.get(), context)
                .thenComposeAsync(result -> handleSuccessfulAction(form.get(), context, result), HttpExecution.defaultContext());
        return recoverWithAsync(resultStage, HttpExecution.defaultContext(), throwable ->
                handleFailedAction(form, context, throwable));
    }

    CompletionStage<? extends R> doAction(final F formData, final T context);

    CompletionStage<Result> handleFailedAction(final Form<? extends F> form, final T context, final Throwable throwable);

    CompletionStage<Result> handleSuccessfulAction(final F formData, final T context, final R result);
}
