package com.commercetools.sunrise.hooks;

import com.commercetools.sunrise.framework.SunriseComponent;
import io.sphere.sdk.models.Base;
import io.sphere.sdk.utils.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecution;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.sphere.sdk.utils.CompletableFutureUtils.successful;
import static java.util.stream.Collectors.toList;

public class RequestHookContextImpl extends Base implements RequestHookContext {
    private static final Logger hookRunnerLogger = LoggerFactory.getLogger(HookRunner.class);
    private static final Logger componentRegistryLogger = LoggerFactory.getLogger(ComponentRegistry.class);

    private final List<Object> controllerComponents = new LinkedList<>();
    private final List<CompletionStage<Object>> asyncHooksCompletionStages = new LinkedList<>();

    @Override
    public <T extends Hook> CompletionStage<Object> runAsyncHook(final Class<T> hookClass, final Function<T, CompletionStage<?>> f) {
        hookRunnerLogger.debug("runAsyncHook {}", hookClass.getSimpleName());
        //TODO throw a helpful NPE if component returns null instead of CompletionStage
        final List<CompletionStage<Void>> collect = controllerComponents.stream()
                .filter(x -> hookClass.isAssignableFrom(x.getClass()))
                .map(hook -> f.apply((T) hook))
                .map(stage -> (CompletionStage<Void>) stage)
                .collect(toList());
        final CompletionStage<?> listCompletionStage = FutureUtils.listOfFuturesToFutureOfList(collect);
        final CompletionStage<Object> result = listCompletionStage.thenApply(z -> null);
        asyncHooksCompletionStages.add(result);
        return result;
    }

    @Override
    public <T extends Hook, R> CompletionStage<R> runAsyncFilterHook(final Class<T> hookClass, final BiFunction<T, R, CompletionStage<R>> f, final R param) {
        hookRunnerLogger.debug("runAsyncFilterHook {}", hookClass.getSimpleName());
        CompletionStage<R> result = successful(param);
        final List<T> applicableHooks = controllerComponents.stream()
                .filter(x -> hookClass.isAssignableFrom(x.getClass()))
                .map(x -> (T) x)
                .collect(Collectors.toList());
        for (final T hook : applicableHooks) {
            result = result.thenComposeAsync(res -> f.apply(hook, res), HttpExecution.defaultContext());
        }
        return result;
    }

    @Override
    public <T extends Hook, R> R runFilterHook(final Class<T> hookClass, final BiFunction<T, R, R> f, final R param) {
        hookRunnerLogger.debug("runFilterHook {}", hookClass.getSimpleName());
        R result = param;
        final List<T> applicableHooks = controllerComponents.stream()
                .filter(x -> hookClass.isAssignableFrom(x.getClass()))
                .map(x -> (T) x)
                .collect(Collectors.toList());
        for (final T hook : applicableHooks) {
            result = f.apply(hook, result);
        }
        return result;
    }

    @Override
    public <T extends Hook> void runVoidHook(final Class<T> hookClass, final Consumer<T> consumer) {
        hookRunnerLogger.debug("runVoidHook {}", hookClass.getSimpleName());
        controllerComponents.stream()
                .filter(x -> hookClass.isAssignableFrom(x.getClass()))
                .forEach(action -> consumer.accept((T) action));
    }

    @Override
    public CompletionStage<Object> allAsyncHooksCompletionStage() {
        return FutureUtils.listOfFuturesToFutureOfList(asyncHooksCompletionStages).thenApply(list -> null);
    }

    @Override
    public void add(final SunriseComponent component) {
        componentRegistryLogger.debug("add component {}", component);
        controllerComponents.add(component);
    }
}
