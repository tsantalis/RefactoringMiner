package com.commercetools.sunrise.hooks;

import java.util.concurrent.CompletionStage;

public interface RequestHookRunner {

    CompletionStage<Object> allAsyncHooksCompletionStage();
}
