package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TypeScriptFileProcessorTest {

	@Test
	public void testMinifiedFileNamePattern() {
		// Matches -min.js and .min.js
		assertTrue(TypeScriptFileProcessor.isMinified("jquery-min.js", "var x = 1;"));
		assertTrue(TypeScriptFileProcessor.isMinified("bundle.min.js", "var x = 1;"));
		assertTrue(TypeScriptFileProcessor.isMinified("path/to/vendor-min.js", "console.log('test');"));
		assertTrue(TypeScriptFileProcessor.isMinified("path/to/vendor.min.js", "console.log('test');"));

		// Must not match without delimiter before 'min' (mirrors Linguist test: !sample_blob("foomin.css").vendored?)
		assertFalse(TypeScriptFileProcessor.isMinified("foomin.js", "var x = 1;"));
	}

	@Test
	public void testLongLineJavaScriptFileIsMinified() {
		// Long-line JavaScript file (average line length > 110 characters)
		String longLine = "var a = 1; " + "x".repeat(300) + "\n";
		assertTrue(TypeScriptFileProcessor.isMinified("bundle.js", longLine));

		// Multi-line JavaScript file where average line length exceeds 110 characters
		String multiLineLong = "const a = 1;\n" + "x".repeat(500) + "\n";
		assertTrue(TypeScriptFileProcessor.isMinified("Makefile.js", multiLineLong));
	}

	@Test
	public void testNormalFileIsNotMinified() {
		// Normal JavaScript file with standard line lengths <= 110 characters
		String normalJs = String.join("\n",
				"function add(a, b) {",
				"    return a + b;",
				"}",
				"export default add;"
		);
		assertFalse(TypeScriptFileProcessor.isMinified("app.js", normalJs));

		// Edge cases: null or empty
		assertFalse(TypeScriptFileProcessor.isMinified("app.js", ""));
		assertFalse(TypeScriptFileProcessor.isMinified("app.js", null));
		assertFalse(TypeScriptFileProcessor.isMinified(null, normalJs));
	}

	@Test
	public void testTypeScriptAndTsxWithLongLinesAreNotMinified() {
		// Long line content (e.g. SVG path data d="..." or long string literals)
		String longLineContent = "export const Icon = () => (\n"
				+ "    <svg viewBox=\"0 0 100 100\">\n"
				+ "        <path d=\"" + "M0,0 L10,10 ".repeat(30) + "\" />\n"
				+ "    </svg>\n"
				+ ");\n";

		// Average line length rule must be restricted to .js files and not drop .ts, .tsx, or .jsx
		assertFalse(TypeScriptFileProcessor.isMinified("Icon.tsx", longLineContent));
		assertFalse(TypeScriptFileProcessor.isMinified("iconData.ts", longLineContent));
		assertFalse(TypeScriptFileProcessor.isMinified("Icon.jsx", longLineContent));
	}
}
