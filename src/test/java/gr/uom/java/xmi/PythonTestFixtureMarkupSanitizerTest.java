package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PythonTestFixtureMarkupSanitizerTest {
	@Test
	void preservesLengthWhenReplacingKnownFixtureMarkup() {
		String fileContent = "yield <warning descr=\"problem\">1</warning>\n";
		String sanitized = PythonTestFixtureMarkupSanitizer.sanitize("python/testData/inspections/sample.py", fileContent);

		assertEquals(fileContent.length(), sanitized.length());
		assertEquals(fileContent.indexOf("1"), sanitized.indexOf("1"));
		assertEquals(-1, sanitized.indexOf("<warning"));
	}

	@Test
	void doesNotSanitizeNonTestDataFiles() {
		String fileContent = "value = \"<warning descr=\\\"text\\\">\"\n";

		assertSame(fileContent, PythonTestFixtureMarkupSanitizer.sanitize("src/main/python/sample.py", fileContent));
	}

	@Test
	void pythonFixtureDiagnosticsDoNotCrashModelReader() {
		String fileContent = """
				from typing import Iterable

				def values() -> Iterable[str]:
				    yield <warning descr="Expected yield type 'str', got 'int' instead">42</warning>
				""";

		assertDoesNotThrow(() -> new UMLModelASTReader(
				Map.of("python/testData/inspections/PyTypeCheckerInspection/FunctionYieldTypePy3.py", fileContent),
				Set.of("python/testData/inspections/PyTypeCheckerInspection"), false).getUmlModel());
	}
}
