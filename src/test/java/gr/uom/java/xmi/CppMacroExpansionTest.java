package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.CppFileProcessor.CppMacroDefinition;
import gr.uom.java.xmi.CppFileProcessor.CppMacroExpansion;

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

		List<CppMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertEquals(1, expansions.size());
		assertEquals("SIZE", expansions.get(0).name());
		assertEquals("SIZE", expansions.get(0).definitionName());
		assertTrue(expansions.get(0).nestedReferences().isEmpty());
		assertTrue(expansions.get(0).startOffset() >= 0);
		assertTrue(expansions.get(0).endOffset() > expansions.get(0).startOffset());
	}

	@Test
	void findsFunctionStyleMacroExpansionInsideDeclaration() throws Exception {
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(SOURCE);
		IASTDeclaration declaration = findDeclaration(ast, "int result = SQUARE(5);");

		List<CppMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertEquals(1, expansions.size());
		assertEquals("SQUARE", expansions.get(0).name());
		assertEquals("SQUARE", expansions.get(0).definitionName());
		assertTrue(expansions.get(0).nestedReferences().isEmpty());
		assertTrue(expansions.get(0).startOffset() >= 0);
		assertTrue(expansions.get(0).endOffset() > expansions.get(0).startOffset());
	}

	@Test
	void ignoresMacroExpansionsOutsideDeclarationRange() throws Exception {
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(SOURCE);
		IASTDeclaration declaration = findDeclaration(ast, "int untouched = 0;");

		List<CppMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		assertTrue(expansions.isEmpty());
	}

	@Test
	void keepsAdjacentDeclarationMacroExpansionsSeparate() throws Exception {
		String source = String.join("\n",
				"#define LEFT 1",
				"#define RIGHT 2",
				"int first = LEFT; int second = RIGHT;") + "\n";
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(source);

		List<CppMacroExpansion> firstExpansions =
				findMacroExpansionsInDeclaration(findDeclaration(ast, "int first = LEFT;"), ast);
		List<CppMacroExpansion> secondExpansions =
				findMacroExpansionsInDeclaration(findDeclaration(ast, "int second = RIGHT;"), ast);

		assertEquals(List.of("LEFT"), firstExpansions.stream().map(CppMacroExpansion::name).toList());
		assertEquals(List.of("RIGHT"), secondExpansions.stream().map(CppMacroExpansion::name).toList());
	}

	@Test
	void findsMultipleMacroExpansionsInsideSameDeclaration() throws Exception {
		String source = String.join("\n",
				"#define WIDTH 3",
				"#define HEIGHT 4",
				"int area = WIDTH * HEIGHT;") + "\n";
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(source);
		IASTDeclaration declaration = findDeclaration(ast, "int area = WIDTH * HEIGHT;");

		List<CppMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);

		//Assert that the macro expansions found in this declaration are WIDTH and HEIGHT, in that order.
		assertEquals(List.of("WIDTH", "HEIGHT"), expansions.stream().map(CppMacroExpansion::name).toList());
	}

	@Test
	void findsMacroExpansionInsideFunctionDefinitionBody() throws Exception {
		String source = String.join("\n",
				"#define VALUE 7",
				"int compute() {",
				"  return VALUE;",
				"}") + "\n";
		IASTTranslationUnit ast = LocationInfoCppTest.parseTranslationUnit(source);
		IASTDeclaration declaration = findDeclaration(ast, String.join("\n",
				"int compute() {",
				"  return VALUE;",
				"}"));

		List<CppMacroExpansion> expansions = findMacroExpansionsInDeclaration(declaration, ast);
		
		//Assert that the macro expansion found in this declaration is VALUE
		assertEquals(List.of("VALUE"), expansions.stream().map(CppMacroExpansion::name).toList());
	}


	private static List<CppMacroExpansion> findMacroExpansionsInDeclaration(
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
