package org.refactoringminer.perforce;

/**
 * Record to pass around Perforce connection details
 * @author Davood Mazinanian
 */
public record PerforceConnectionDetails(String perforceServerUri, String userName, String password) {
}
