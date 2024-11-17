package com.commercetools.sunrise.common.ctp;

import com.google.inject.Provider;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.ApplicationLifecycle;
import play.mvc.Http;

import javax.inject.Inject;

import static java.util.concurrent.CompletableFuture.completedFuture;

public final class RequestScopedSphereClientProvider implements Provider<SphereClient> {
    private static final Logger logger = LoggerFactory.getLogger(RequestScopedSphereClientProvider.class);

    @Inject
    private ApplicationLifecycle applicationLifecycle;
    @Inject
    private HttpClient httpClient;
    @Inject
    private SphereAccessTokenSupplier sphereAccessTokenSupplier;
    @Inject
    private SphereClientConfig sphereClientConfig;
    @Inject
    private Http.Context context;

    @Override
    public SphereClient get() {
        final MetricHttpClient metricHttpClient = MetricHttpClient.of(httpClient, context);
        logger.info("Provide SphereClient");
        final SphereClient sphereClient = SphereClient.of(sphereClientConfig, metricHttpClient, sphereAccessTokenSupplier);
        applicationLifecycle.addStopHook(() -> {
            sphereClient.close();
            return completedFuture(null);
        });
        return sphereClient;
    }

}
