package org.refactoringminer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class GitHubOAuthTokenProvider {
    private static final String PROPERTY_NAME = "OAuthToken";
    private static final String FILE_NAME = "github-oauth.properties";

    private GitHubOAuthTokenProvider() {
    }

    public static String getOAuthToken() {
        String oAuthToken = System.getenv(PROPERTY_NAME);
        if (isBlank(oAuthToken)) {
            oAuthToken = System.getProperty(PROPERTY_NAME);
        }
        if (!isBlank(oAuthToken)) {
            return oAuthToken;
        }
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(FILE_NAME)) {
            prop.load(input);
            return blankToNull(prop.getProperty(PROPERTY_NAME));
        } catch (IOException ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }
}
