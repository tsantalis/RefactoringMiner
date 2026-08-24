package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTMacroExpansionLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocationInfoCppTest {
	private static final String SRC_FOLDER = "src";

	private static final String FILE_PATH = "src/Sample.cpp";
	private static final String SOURCE = String.join("\n",
			"class A {",
			"public:",
			"  int value;",
			"  void run() {}",
			"};") + "\n";

	@Test
	void computesOneBasedColumnsForIndentedCppField() throws Exception {
		IASTNode field = findDeclaration(SOURCE, "int value;");

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, field,
				LocationInfo.CodeElementType.FIELD_DECLARATION, SOURCE);

		int startOffset = SOURCE.indexOf("int value;");
		int endOffset = startOffset + "int value;".length();
		assertEquals(startOffset, location.getStartOffset());
		assertEquals(endOffset, location.getEndOffset());
		assertEquals(3, location.getStartLine());
		assertEquals(3, location.getEndLine());
		assertEquals(3, location.getStartColumn());
		assertEquals(12, location.getEndColumn());
		assertEquals(5, location.getCompilationUnitLength());
	}

	@Test
	void computesEndColumnFromLastCharacterInsideCppNode() throws Exception {
		IASTNode method = findDeclaration(SOURCE, "void run() {}");

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, method,
				LocationInfo.CodeElementType.METHOD_DECLARATION, SOURCE);

		int startOffset = SOURCE.indexOf("void run() {}");
		int endOffset = startOffset + "void run() {}".length();
		assertEquals(startOffset, location.getStartOffset());
		assertEquals(endOffset, location.getEndOffset());
		assertEquals(4, location.getStartLine());
		assertEquals(4, location.getEndLine());
		assertEquals(3, location.getStartColumn());
		assertEquals(15, location.getEndColumn());
	}

	@Test
	void computesColumnOneForNodeAtStartOfFile() throws Exception {
		String source = "int value;\n";
		IASTNode field = findDeclaration(source, "int value;");

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, field,
				LocationInfo.CodeElementType.FIELD_DECLARATION, source);

		assertEquals(0, location.getStartOffset());
		assertEquals(1, location.getStartColumn());
		assertEquals(10, location.getEndColumn());
	}

	@ParameterizedTest
	@ValueSource(strings = {"\n", "\r\n", "\r"})
	void computesColumnsForDifferentLineEndings(String lineEnding) throws Exception {
		String source = String.join(lineEnding,
				"class A {",
				"public:",
				"  int value;",
				"};") + lineEnding;
		IASTNode field = findDeclaration(source, "int value;");

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, field,
				LocationInfo.CodeElementType.FIELD_DECLARATION, source);

		assertEquals(3, location.getStartColumn());
		assertEquals(12, location.getEndColumn());
		assertEquals(4, location.getCompilationUnitLength());
	}

	@Test
	void returnsZeroColumnsWhenFileContentIsMissing() throws Exception {
		IASTNode field = findDeclaration(SOURCE, "int value;");

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, field,
				LocationInfo.CodeElementType.FIELD_DECLARATION, null);

		assertEquals(0, location.getStartColumn());
		assertEquals(0, location.getEndColumn());
		assertEquals(0, location.getCompilationUnitLength());
	}

	@Test
	void mapsMixedMacroExpansionLocationsToOriginalCppSourceSpan() throws Exception {
		String source = String.join("\n",
				"#define TEST(suite, name) void suite##_##name()",
				"",
				"TEST(DynamicUpdateSliceOpTest, SimpleTestF32InPlaceInput) {",
				"  int value = 1;",
				"}") + "\n";
		IASTNode function = findDeclaration(source, String.join("\n",
				"TEST(DynamicUpdateSliceOpTest, SimpleTestF32InPlaceInput) {",
				"  int value = 1;",
				"}"));

		boolean hasMacroExpansionLocation = false;
		for(IASTNodeLocation nodeLocation : function.getNodeLocations()) {
			if(nodeLocation instanceof IASTMacroExpansionLocation) {
				hasMacroExpansionLocation = true;
			}
		}

		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, function,
				LocationInfo.CodeElementType.METHOD_DECLARATION, source);

		int startOffset = source.indexOf("TEST(DynamicUpdateSliceOpTest, SimpleTestF32InPlaceInput)");
		int endOffset = source.indexOf("}", startOffset) + 1;
		assertTrue(function.getNodeLocations().length > 1);
		assertTrue(hasMacroExpansionLocation);
		assertEquals(startOffset, location.getStartOffset());
		assertEquals(endOffset, location.getEndOffset());
		assertEquals(3, location.getStartLine());
		assertEquals(5, location.getEndLine());
		assertEquals(1, location.getStartColumn());
		assertEquals(1, location.getEndColumn());
	}

	@Test
	void mapsMacroArgumentExpressionsToTheirOriginalSourceRanges() throws Exception {
		String source = String.join("\n",
				"#define ASSERT_EQ(lhs, rhs) if ((lhs) == (rhs)) {}",
				"void run() {",
				"  ASSERT_EQ(foo(), bar());",
				"}") + "\n";
		IASTTranslationUnit translationUnit = parseTranslationUnit(source);

		assertMacroArgumentLocation(translationUnit, source, "foo()");
		assertMacroArgumentLocation(translationUnit, source, "bar()");
	}

	private static void assertMacroArgumentLocation(IASTTranslationUnit translationUnit, String source,
			String expressionText) {
		IASTExpression expression = findMacroArgumentExpression(translationUnit, source, expressionText);
		LocationInfo location = new LocationInfo(SRC_FOLDER, FILE_PATH, expression,
				LocationInfo.CodeElementType.METHOD_INVOCATION, source);

		int startOffset = source.indexOf(expressionText);
		assertEquals(startOffset, location.getStartOffset());
		assertEquals(startOffset + expressionText.length(), location.getEndOffset());
		assertEquals(3, location.getStartLine());
		assertEquals(3, location.getEndLine());
	}

	private static IASTExpression findMacroArgumentExpression(IASTTranslationUnit translationUnit, String source,
			String expressionText) {
		class MacroArgumentVisitor extends ASTVisitor {
			private IASTExpression match;

			private MacroArgumentVisitor() {
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTExpression expression) {
				if(expression instanceof IASTFunctionCallExpression && expression instanceof ASTNode astNode) {
					IASTImageLocation imageLocation = astNode.getImageLocation();
					if(imageLocation != null &&
							imageLocation.getLocationKind() == IASTImageLocation.ARGUMENT_TO_MACRO_EXPANSION &&
							expressionText.equals(source.substring(imageLocation.getNodeOffset(),
									imageLocation.getNodeOffset() + imageLocation.getNodeLength()))) {
						match = expression;
						return PROCESS_ABORT;
					}
				}
				return PROCESS_CONTINUE;
			}
		}

		MacroArgumentVisitor visitor = new MacroArgumentVisitor();
		translationUnit.accept(visitor);
		assertNotNull(visitor.match, "Could not find macro argument expression: " + expressionText);
		return visitor.match;
	}

	//Search parsed C++ AST for specific declaration and return its node
	private static IASTNode findDeclaration(String source, String rawSignature) throws CoreException {
		IASTTranslationUnit translationUnit = parseTranslationUnit(source);
		return findDeclaration(translationUnit, rawSignature);
	}

	private static IASTNode findDeclaration(IASTTranslationUnit translationUnit, String rawSignature) {
		MyVisitor visitor = new MyVisitor(rawSignature);
		translationUnit.accept(visitor);
		assertNotNull(visitor.getMatch(), "Could not find C++ declaration: " + rawSignature);
		return visitor.getMatch();
	}
	private static class MyVisitor extends ASTVisitor {
		private IASTNode match;
		private String rawSignature;
		
		public MyVisitor (String rawSignature) {
			this.rawSignature = rawSignature;
			shouldVisitDeclarations = true;
		}
		
		public IASTNode getMatch() {
			return match;
		}

		@Override
		public int visit(IASTDeclaration declaration) {
			if(rawSignature.equals(declaration.getRawSignature())) {
				match = declaration;
				return PROCESS_ABORT;
			}
			return PROCESS_CONTINUE;
		}
	}

	//parse C++ src code into AST
	private static IASTTranslationUnit parseTranslationUnit(String source) throws CoreException {
		FileContent fileContent = FileContent.create(FILE_PATH, source.toCharArray());
		IScannerInfo scannerInfo = new ScannerInfo(Map.of(), new String[0]);
		IncludeFileContentProvider includes = IncludeFileContentProvider.getEmptyFilesProvider();
		IParserLogService log = new DefaultLogService();
		ILanguage language = GPPLanguage.getDefault();
		return language.getASTTranslationUnit(
				fileContent,
				scannerInfo,
				includes,
				(IIndex) null,
				ILanguage.OPTION_IS_SOURCE_UNIT,
				log);
	}
}
