package org.refactoringminer.perforce;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import spark.utils.IOUtils;

import java.io.InputStream;
import java.util.List;

/**
 * Utils to allow connecting to Perforce and supporting operations for AST Diff and more.
 * The methods in this class are not static for ease of testing.
 * @author Davood Mazinanian
 */
public class PerforceUtils {

    /**
     * Returns the contents of a file at a specific revision, given its Depot path
     */
    public static String getFileContentsAtRevision(String depotPath, int previousRevisionNumber, IOptionsServer perforceServer) throws Exception {
        // The file spec for this file follows the following format: `depot_path#revision_number`
        String previousVersionFileSpecString = "%s#%s".formatted(depotPath, previousRevisionNumber);

        List<IFileSpec> previousFileSpecList = FileSpecBuilder.makeFileSpecList(previousVersionFileSpecString);

        // No need to bring all revisions - only get the current one
        boolean getAllRevisions = false;

        // Suppress the initial line that displays the file name and revision.
        boolean noHeaderLine = true;

        try (InputStream fileContentsStream = perforceServer.getFileContents(previousFileSpecList, getAllRevisions, noHeaderLine)) {
            return IOUtils.toString(fileContentsStream);
        }
    }

    /**
     * Returns the contents of a file given its {@link IFileSpec}
     */
    public static String getFileContents(IFileSpec fileSpec) throws Exception {
        // Exclude the header line from the contents (only bring the contents of the file)
        boolean noHeaderLine = true;
        try (InputStream fileContentsInputStream = fileSpec.getContents(noHeaderLine)) {
            return IOUtils.toString(fileContentsInputStream);
        }
    }

    /**
     * Build a {@link PerforceConnectionDetails} from the given URL and credentials.
     * Sanitize the URL as required by P4Java, see {@link PerforceUtils#sanitizePerforceServerUrl}
     */
    public static PerforceConnectionDetails getPerforceCredentialsObject(String url, String userName, String password) {
        url = sanitizePerforceServerUrl(url);
        return new PerforceConnectionDetails(url, userName, password);
    }

    /**
     * The perforce URL must start with "p4java://".
     * If the URL starts with anything other than that, it will be removed.
     * In case HTTPS is required, we use "p4javassl//" instead.
     */
    private static String sanitizePerforceServerUrl(String url) {
        String result;
        // Don't change anything if the URL already starts with p4java:// or p4javassl://
        if (url.startsWith("p4java://") || url.startsWith("p4javassl://")) {
            result = url;
        } else {
            // Only if the URL is given with https, we use p4javassl
            boolean isHttps = url.startsWith("https://");
            // Remove anything before ://
            url = url.replaceAll("^.+://", "");
            result = "p4java%s://%s".formatted(isHttps ? "ssl" : "", url);
        }
        return result;
    }
}
