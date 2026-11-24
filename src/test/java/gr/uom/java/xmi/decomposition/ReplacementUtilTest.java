package gr.uom.java.xmi.decomposition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class ReplacementUtilTest {

	private static List<Replacement> replacements(String... beforeAfter) {
		List<Replacement> replacements = new ArrayList<>();
		for(int i = 0; i < beforeAfter.length; i += 2) {
			replacements.add(new Replacement(beforeAfter[i], beforeAfter[i + 1], ReplacementType.VARIABLE_NAME));
		}
		return replacements;
	}

	private static String sequential(String text, List<Replacement> replacements) {
		for(Replacement replacement : replacements) {
			text = ReplacementUtil.performReplacement(text, replacement.getBefore(), replacement.getAfter());
		}
		return text;
	}

	@Test
	void appliesIndependentReplacementsLikeTheSequentialReplacements() {
		List<Replacement> replacements = replacements("c", "x", "d", "y", "b", "z");
		for(String text : List.of("a.b(c) + d", "a+b", "c", "f(c, d)")) {
			assertEquals(sequential(text, replacements), ReplacementUtil.performReplacements(text, replacements));
		}
	}

	@Test
	void appliesReplacementsFoundOnTheTextWithTheEarlierReplacementsApplied() {
		// elasticsearch connector MockHttpClient: the second replacement contains the whole text inserted by the first
		assertEquals("unsentRecords.size()", ReplacementUtil.performReplacements("random.nextInt(batch.size() - 1)",
				replacements("-", ">=", "random.nextInt(batch.size() >= 1)", "unsentRecords.size()")));
		assertEquals("z", ReplacementUtil.performReplacements("x", replacements("x", "y", "y", "z")));
	}

	@Test
	void doesNotReplaceInsideTheTextInsertedByAnEarlierReplacement() {
		// osmdroid CacheManager: 0 -> 180.0 followed by 0 -> Math.PI gave 18Math.PI.0
		assertEquals("wayPoint.getLatitude() * 180.0", ReplacementUtil.performReplacements("mUpperLeft.y > 0",
				replacements(">", "*", "mUpperLeft.y", "wayPoint.getLatitude()", "0", "180.0", "0", "Math.PI")));
		// intellij ActionUrl: myComponent -> myGroupPath rewrote the inserted myComponentId
		assertEquals("Objects.equals(myComponentId,url.myComponentId)", ReplacementUtil.performReplacements("url.myComponent",
				replacements("url.myComponent", "Objects.equals(myComponentId,url.myComponentId)", "myComponent", "myGroupPath")));
	}

	@Test
	void doesNotCompoundReplacementsWhoseNewTextContainsTheOldTextOfLaterOnes() {
		// babel Makefile.js (minified): single-letter names replaced with function expressions containing them grew past the maximum string length
		StringBuilder text = new StringBuilder("t");
		for(int i = 0; i < 20; i++) {
			text.append(" + t(e, r)");
		}
		List<Replacement> replacements = new ArrayList<>();
		for(int i = 0; i < 6; i++) {
			replacements.addAll(replacements("t", "function(e, t) { return t(e, r) + t(r, e); }", "e", "t(e) + t(e)", "r", "t(r, e)"));
		}
		String result = ReplacementUtil.performReplacements(text.toString(), replacements);
		assertTrue(result.length() < 10_000, "length " + result.length());
	}
}
