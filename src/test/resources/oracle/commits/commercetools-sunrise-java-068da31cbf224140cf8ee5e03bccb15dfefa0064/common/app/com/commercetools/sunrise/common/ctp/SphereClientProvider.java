package com.commercetools.sunrise.common.ctp;

import com.google.inject.Provider;
import io.sphere.sdk.client.*;
import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.play.metrics.MetricAction;
import io.sphere.sdk.play.metrics.MetricHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.ApplicationLifecycle;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.concurrent.CompletableFuture.completedFuture;

public final class SphereClientProvider implements Provider<SphereClient> {
    private static final Logger logger = LoggerFactory.getLogger(SphereClientProvider.class);
    private static final String CONFIG_PROJECT_KEY = "ctp.projectKey";
    private static final String CONFIG_CLIENT_ID = "ctp.clientId";
    private static final String CONFIG_CLIENT_SECRET = "ctp.clientSecret";
    private static final String CONFIG_API_URL = "ctp.apiUrl";
    private static final String CONFIG_AUTH_URL = "ctp.authUrl";
    private static final String DEFAULT_API_URL = "https://api.sphere.io";
    private static final String DEFAULT_AUTH_URL = "https://auth.sphere.io";

    @Inject
    private ApplicationLifecycle applicationLifecycle;
    @Inject
    private Configuration configuration;

    @Override
    public SphereClient get() {
        final SphereClientConfig config = getClientConfig();
        final SphereClient sphereClient = createClient(config);
        applicationLifecycle.addStopHook(() -> {
            sphereClient.close();
            return completedFuture(null);
        });
        return sphereClient;
    }

    private SphereClient createClient(final SphereClientConfig config) {
        final boolean metricsEnabled = configuration.getBoolean(MetricAction.CONFIG_METRICS_ENABLED);
        return metricsEnabled ? createClientWithMetrics(config) : createRegularClient(config);
    }

    private SphereClient createRegularClient(final SphereClientConfig clientConfig) {
        logger.info("Provide SphereClient");
        return SphereClientFactory.of().createClient(clientConfig);
    }

    private SphereClient createClientWithMetrics(final SphereClientConfig clientConfig) {
        final HttpClient underlyingHttpClient = SphereAsyncHttpClientFactory.create();
        final MetricHttpClient httpClient = MetricHttpClient.of(underlyingHttpClient);
        final SphereAccessTokenSupplier tokenSupplier = SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
        logger.info("Provide SphereClient with metrics");
        return SphereClient.of(clientConfig, httpClient, tokenSupplier);
    }

    private SphereClientConfig getClientConfig() {
        final String project = getValue(CONFIG_PROJECT_KEY);
        final String clientId = getValue(CONFIG_CLIENT_ID);
        final String clientSecret = getValue(CONFIG_CLIENT_SECRET);
        final String authUrl = configuration.getString(CONFIG_AUTH_URL, DEFAULT_AUTH_URL);
        final String apiUrl = configuration.getString(CONFIG_API_URL, DEFAULT_API_URL);
        return SphereClientConfig.of(project, clientId, clientSecret, authUrl, apiUrl);
    }

    private String getValue(final String key) {
        return Optional.ofNullable(configuration.getString(key)).orElseThrow(() -> new SphereClientCredentialsException("missing value for configuration key " + key));
    }
}
