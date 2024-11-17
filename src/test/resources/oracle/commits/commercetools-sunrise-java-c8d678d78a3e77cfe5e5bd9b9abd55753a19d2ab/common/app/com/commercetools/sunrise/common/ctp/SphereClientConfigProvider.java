package com.commercetools.sunrise.common.ctp;

import com.google.inject.Provider;
import io.sphere.sdk.client.SphereClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;

import javax.inject.Inject;
import java.util.Optional;

public final class SphereClientConfigProvider implements Provider<SphereClientConfig> {
    private static final Logger logger = LoggerFactory.getLogger(SphereClientConfigProvider.class);
    private static final String CONFIG_PROJECT_KEY = "ctp.projectKey";
    private static final String CONFIG_CLIENT_ID = "ctp.clientId";
    private static final String CONFIG_CLIENT_SECRET = "ctp.clientSecret";
    private static final String CONFIG_API_URL = "ctp.apiUrl";
    private static final String CONFIG_AUTH_URL = "ctp.authUrl";
    private static final String DEFAULT_API_URL = "https://api.sphere.io";
    private static final String DEFAULT_AUTH_URL = "https://auth.sphere.io";

    @Inject
    private Configuration configuration;

    @Override
    public SphereClientConfig get() {
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
