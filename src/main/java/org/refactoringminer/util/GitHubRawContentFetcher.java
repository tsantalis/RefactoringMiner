package org.refactoringminer.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vdurmont.emoji.EmojiParser;

/**
 * Helpers for retrieving raw file content from GitHub and for deriving the raw URL of the same file
 * at a different commit (and, for renames, at a different path).
 * <p>
 * GitHub serves blobs without a size limit from its raw endpoint, whereas the Contents API
 * ({@code GHRepository.getFileContent}) caps responses at ~1&nbsp;MB. These helpers are therefore
 * used whenever a full file revision must be retrieved for a public repository, e.g. to obtain the
 * parent version of a changed file by pointing the head raw URL at the parent/merge-base commit.
 */
public final class GitHubRawContentFetcher {

	private GitHubRawContentFetcher() {
	}

	/** Downloads the content located at {@code rawURL}, stripping emojis and decoding it as UTF-8. */
	public static String fetch(URL rawURL) throws IOException {
		URLConnection connection = rawURL.openConnection();
		// GitHub may answer raw requests without a User-Agent with HTTP 403.
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		try (InputStream input = connection.getInputStream()) {
			return read(input);
		}
	}

	/** Reads {@code input} into a String, stripping emojis and decoding it as UTF-8. */
	public static String read(InputStream input) throws IOException {
		return EmojiParser.removeAllEmojis(IOUtils.toString(input, StandardCharsets.UTF_8));
	}

	/** Returns {@code rawURL} re-pointed from {@code fromCommitId} to {@code toCommitId}. */
	public static URL atCommit(URL rawURL, String fromCommitId, String toCommitId) throws MalformedURLException {
		return new URL(rawURL.toString().replace(fromCommitId, toCommitId));
	}

	/**
	 * Returns {@code rawURL} re-pointed to {@code toCommitId} and to {@code toPath} (e.g. the
	 * pre-rename name), used to obtain the parent revision of a renamed file.
	 */
	public static URL atCommitAndPath(URL rawURL, String fromCommitId, String toCommitId, String fromPath, String toPath)
			throws MalformedURLException {
		String url = rawURL.toString().replace(fromCommitId, toCommitId).replace(encodePath(fromPath), encodePath(toPath));
		return new URL(url);
	}

	/**
	 * Encodes a repository path the way GitHub encodes it inside a raw file URL (slashes become %2F).
	 * {@link URLEncoder} maps spaces to {@code '+'}, whereas GitHub uses %20, so we convert those; a
	 * literal {@code '+'} is encoded by {@link URLEncoder} as %2B, so this replacement only ever
	 * touches encoded spaces.
	 */
	public static String encodePath(String path) {
		return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
