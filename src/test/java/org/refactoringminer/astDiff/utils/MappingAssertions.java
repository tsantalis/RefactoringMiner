package org.refactoringminer.astDiff.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Compares the expected and the calculated mappings of the AST diff tests, so that a failing case reports
 * the mappings that were removed or added in every file, not only that the first file differs in length.
 */
public class MappingAssertions {
	private static final int MAX_REPORTED_MAPPINGS = 20;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	//collects the failures of the files of a case, so that all differing files are reported at once
	public static class Failures {
		private final List<String> messages = new ArrayList<>();

		public void check(String expected, String calculated, String message) {
			if (!calculated.equals(expected))
				messages.add(message + "\n" + report(expected, calculated));
		}

		public void assertNone() {
			if (!messages.isEmpty())
				fail(messages.size() + " file(s) differ from the expected mappings\n\n" + String.join("\n\n", messages));
		}
	}

	//the mappings missing from (removed) or new in (added) the calculated mappings, compared as exported
	public static String report(String expected, String calculated) {
		List<Map<String, Object>> expectedMappings = parse(expected);
		List<Map<String, Object>> calculatedMappings = parse(calculated);
		if (expectedMappings == null || calculatedMappings == null)
			return "the mappings could not be parsed, first difference at character " + firstDifference(expected, calculated);
		Map<String, Map<String, Object>> expectedByKey = byKey(expectedMappings);
		Map<String, Map<String, Object>> calculatedByKey = byKey(calculatedMappings);
		List<Map<String, Object>> removed = new ArrayList<>();
		for (Map.Entry<String, Map<String, Object>> entry : expectedByKey.entrySet())
			if (!calculatedByKey.containsKey(entry.getKey()))
				removed.add(entry.getValue());
		List<Map<String, Object>> added = new ArrayList<>();
		for (Map.Entry<String, Map<String, Object>> entry : calculatedByKey.entrySet())
			if (!expectedByKey.containsKey(entry.getKey()))
				added.add(entry.getValue());
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("expected %d mappings, calculated %d: %d removed, %d added", expectedMappings.size(), calculatedMappings.size(), removed.size(), added.size()));
		if (removed.isEmpty() && added.isEmpty())
			sb.append(" (same mappings in another order)");
		append(sb, "removed", removed);
		append(sb, "added", added);
		return sb.toString();
	}

	private static void append(StringBuilder sb, String label, List<Map<String, Object>> mappings) {
		if (mappings.isEmpty())
			return;
		sb.append("\n ").append(label).append(":");
		for (int i = 0; i < Math.min(mappings.size(), MAX_REPORTED_MAPPINGS); i++)
			sb.append("\n  ").append(describe(mappings.get(i)));
		if (mappings.size() > MAX_REPORTED_MAPPINGS)
			sb.append("\n  ... ").append(mappings.size() - MAX_REPORTED_MAPPINGS).append(" more");
	}

	//i.e., simple_identifier[writeByte] @7385-7394 (in call_expression) -> simple_identifier[writeByte] @6551-6560 (in call_expression)
	private static String describe(Map<String, Object> mapping) {
		return node(mapping, "first") + " -> " + node(mapping, "second");
	}

	private static String node(Map<String, Object> mapping, String side) {
		String label = String.valueOf(mapping.get(side + "Label"));
		String text = mapping.get(side + "Type") + (label.isEmpty() || label.equals("null") ? "" : "[" + label + "]");
		return text + " @" + mapping.get(side + "Pos") + "-" + mapping.get(side + "EndPos") + " (in " + mapping.get(side + "ParentType") + ")";
	}

	private static Map<String, Map<String, Object>> byKey(List<Map<String, Object>> mappings) {
		Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
		for (Map<String, Object> mapping : mappings)
			byKey.put(key(mapping), mapping);
		return byKey;
	}

	private static String key(Map<String, Object> mapping) {
		Set<String> fields = new LinkedHashSet<>(mapping.keySet());
		StringBuilder sb = new StringBuilder();
		for (String field : fields)
			sb.append(field).append('=').append(mapping.get(field)).append(';');
		return sb.toString();
	}

	private static List<Map<String, Object>> parse(String json) {
		if (json == null)
			return null;
		try {
			return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static int firstDifference(String s1, String s2) {
		if (s1 == null || s2 == null)
			return 0;
		int i = 0;
		while (i < s1.length() && i < s2.length() && s1.charAt(i) == s2.charAt(i))
			i++;
		return i;
	}
}
