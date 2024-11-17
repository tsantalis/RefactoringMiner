package com.commercetools.sunrise.hooks;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface HookRunner {
    /**
     * Executes a hook which takes 0 to n parameters and returns a {@link CompletionStage}.
     * The execution (just the creation of the {@link CompletionStage}) is synchronous and each implementing component will be called after each other and does not wait for the {@link CompletionStage} to be completed.
     * The underlying computation to complete the {@link CompletionStage} can be asynchronous and should run in parallel for the components.
     * The result should be completed at some point and a successful completion can also contain the value {@code null} hence the successful result is not used directly by the framework.
     * Before the hook {@link PageDataHook} is called, all asynchronous computations for the requests need to be completed successfully.
     *
     * @param hookClass the class which represents the hook
     * @param f         a possible asynchronous computation using the hook
     * @param <T>       the type of the hook
     * @return a {@link CompletionStage} which is completed successfully if all underlying components completed successfully with this hook, otherwise a exceptionally completed {@link CompletionStage}
     */
    <T extends Hook> CompletionStage<Object> runAsyncHook(final Class<T> hookClass, final Function<T, CompletionStage<?>> f);

    <T extends Hook, R> CompletionStage<R> runAsyncFilterHook(final Class<T> hookClass, final BiFunction<T, R, CompletionStage<R>> f, final R param);

    /**
     * Executes a hook with one parameter that returns a value of the same type as the parameter. The execution is synchronous and each
     * implementing component will be called after each other. A typical use case is to use this as filter especially in combination with "withers".
     *
     * @param hookClass the class which represents the hook
     * @param f a computation (filter) that takes the hook and the parameter of type {@code R} as argument and returns a computed value of the type {@code R}
     * @param param the initial parameter for the filter
     * @param <T> the type of the hook
     * @param <R> the type of the parameter
     * @return the result of the filter chain, if there is no hooks then it will be the parameter itself, if there are multiple hooks then it will be applied like this: f<sub>3</sub>(f<sub>2</sub>(f<sub>1</sub>(initialParameter)))
     */
    <T extends Hook, R> R runFilterHook(final Class<T> hookClass, final BiFunction<T, R, R> f, final R param);

    /**
     * Executes a hook which takes 0 to n parameters and returns nothing. The execution is synchronous and each
     * implementing component will be called after each other. This is normally used to achieve side effects.
     *
     * @param hookClass the class which represents the hook
     * @param consumer a computation that takes the hook instance as parameter which represents executing the hook
     * @param <T> the type of the hook
     */
    <T extends Hook> void runVoidHook(final Class<T> hookClass, final Consumer<T> consumer);

    CompletionStage<Object> allAsyncHooksCompletionStage();
}