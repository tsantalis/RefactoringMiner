package com.commercetools.sunrise.common.ctp;

import com.google.inject.Provider;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.ApplicationLifecycle;

import javax.inject.Inject;

import static java.util.concurrent.CompletableFuture.completedFuture;

public final class SphereAccessTokenSupplierProvider implements Provider<SphereAccessTokenSupplier> {
    private static final Logger logger = LoggerFactory.getLogger(SphereAccessTokenSupplierProvider.class);

    @Inject
    private ApplicationLifecycle applicationLifecycle;
    @Inject
    private SphereClientConfig sphereClientConfig;
    @Inject
    private HttpClient httpClient;

    @Override
    public SphereAccessTokenSupplier get() {
        final SphereAccessTokenSupplier sphereAccessTokenSupplier =
                SphereAccessTokenSupplier.ofAutoRefresh(sphereClientConfig, httpClient, false);
        applicationLifecycle.addStopHook(() -> {
            sphereAccessTokenSupplier.close();
            return completedFuture(null);
        });
        return sphereAccessTokenSupplier;
    }
}
