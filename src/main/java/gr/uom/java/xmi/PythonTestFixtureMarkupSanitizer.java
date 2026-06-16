package gr.uom.java.xmi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PythonTestFixtureMarkupSanitizer {
	private static final Pattern FIXTURE_MARKUP = Pattern.compile(
			"</?(?:warning|weak_warning|error|info|caret|selection|fold|ref|symbolName|lineMarker|TYPO|inject)(?:\\s+[^<>]*)?>");

	private PythonTestFixtureMarkupSanitizer() {
	}

	static String sanitize(String filePath, String fileContent) {
		if(fileContent == null || fileContent.indexOf('<') == -1 || !isTestFixtureFile(filePath)) {
			return fileContent;
		}
		Matcher matcher = FIXTURE_MARKUP.matcher(fileContent);
		StringBuffer sanitized = new StringBuffer(fileContent.length());
		while(matcher.find()) {
			matcher.appendReplacement(sanitized, Matcher.quoteReplacement(toWhitespace(matcher.group())));
		}
		matcher.appendTail(sanitized);
		return sanitized.toString();
	}

	private static boolean isTestFixtureFile(String filePath) {
		String normalizedPath = filePath.replace('\\', '/');
		return normalizedPath.startsWith("testData/") || normalizedPath.contains("/testData/");
	}

	private static String toWhitespace(String value) {
		StringBuilder builder = new StringBuilder(value.length());
		for(int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			builder.append(ch == '\n' || ch == '\r' ? ch : ' ');
		}
		return builder.toString();
	}
}
