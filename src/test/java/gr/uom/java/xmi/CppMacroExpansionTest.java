package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroExpansion;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.junit.jupiter.api.Test;

class CppMacroExpansionTest {
	private static final String SOURCE = String.join("\n",
			"#define SIZE 10",
			"#define SQUARE(x) ((x) * (x))",
			"",
			"int values[SIZE];",
			"int result = SQUARE(5);",
			"int untouched = 0;") + "\n";

	@Test
	void findsObjectStyleMacroExpansionInsideDeclaration() throws Exception {
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(SOURCE);
		IASTDeclaration declaration = findDeclaration(ast, "int values[SIZE];");

		List<IASTPreprocessorMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertEquals(1, expansions.size());
		assertEquals("SIZE", expansions.get(0).getMacroReference().toString());
		assertEquals("SIZE", expansions.get(0).getMacroDefinition().getName().toString());
	}

	@Test
	void findsFunctionStyleMacroExpansionInsideDeclaration() throws Exception {
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(SOURCE);
		IASTDeclaration declaration = findDeclaration(ast, "int result = SQUARE(5);");

		List<IASTPreprocessorMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertEquals(1, expansions.size());
		assertEquals("SQUARE", expansions.get(0).getMacroReference().toString());
		assertEquals("SQUARE", expansions.get(0).getMacroDefinition().getName().toString());
	}

	@Test
	void ignoresMacroExpansionsOutsideDeclarationRange() throws Exception {
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(SOURCE);
		IASTDeclaration declaration = findDeclaration(ast, "int untouched = 0;");

		List<IASTPreprocessorMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertTrue(expansions.isEmpty());
	}
	private static List<IASTPreprocessorMacroExpansion> findMacroExpansionsInDeclaration(
			IASTDeclaration declaration, IASTTranslationUnit ast) {
		CppFileProcessor processor = new CppFileProcessor(null);
		processor.processMacroExpansions(ast);
		return processor.findMacroExpansionsInDeclaration(declaration);
	}

	private static IASTDeclaration findDeclaration(IASTTranslationUnit ast, String rawSignature) {
		IASTNode declaration = LocationInfoCppTest.findDeclaration(ast, rawSignature);
		return (IASTDeclaration) declaration;
	}
}
