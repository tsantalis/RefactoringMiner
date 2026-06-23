package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;

class CppFileProcessorTest {
	@Test
	void processesCFunctionDefinitionsIntoModuleOperations() {
		// Exercises the full C parsing path: source text -> CDT AST -> CASTFunctionDefinition -> UMLOperation.
		String filePath = "src/math.c";
		String fileContent = String.join("\n",
				"static inline const char *name(int id, const char *fallback) {",
				"  return fallback;",
				"}",
				"void reset(void) {",
				"}",
				"int sum(int count, ...) {",
				"  return count;",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		assertEquals(1, model.getClassList().size());

		UMLClass moduleClass = model.getClassList().get(0);
		assertEquals("math", moduleClass.getName());
		assertTrue(moduleClass.isModule());

		// The synthetic module class owns top-level C functions because C has no class container.
		List<UMLOperation> operations = moduleClass.getOperations();
		assertEquals(3, operations.size());

		// Verifies function specifiers, pointer return type, and named parameters.
		UMLOperation name = findOperation(operations, "name");
		assertEquals("math", name.getClassName());
		assertTrue(name.isStatic());
		assertTrue(name.isInline());
		assertEquals("const char*", name.getReturnParameter().getType().toString());
		assertEquals(List.of("id", "fallback"), name.getParameterNameList());
		assertEquals(List.of("int", "const char*"), parameterTypeNames(name));
		// Normal parameters should keep their source-level VariableDeclaration for locations and metadata.
		assertTrue(name.getParametersWithoutReturnType().stream()
				.allMatch(parameter -> parameter.getVariableDeclaration() != null));

		// In C, f(void) means there are no input parameters.
		UMLOperation reset = findOperation(operations, "reset");
		assertEquals("math", reset.getClassName());
		assertFalse(reset.isStatic());
		assertFalse(reset.isInline());
		assertEquals("void", reset.getReturnParameter().getType().toString());
		assertEquals(0, reset.getParametersWithoutReturnType().size());

		// C ellipsis varargs are represented with the same synthetic varargs parameter shape used elsewhere.
		UMLOperation sum = findOperation(operations, "sum");
		assertEquals("math", sum.getClassName());
		assertEquals("int", sum.getReturnParameter().getType().toString());
		assertEquals(List.of("count", "varargs"), sum.getParameterNameList());
		assertEquals(List.of("int", "Object[]"), parameterTypeNames(sum));
		// Only the real C parameter has a VariableDeclaration; the ellipsis varargs parameter is synthetic.
		assertNotNull(sum.getParametersWithoutReturnType().get(0).getVariableDeclaration());
		assertTrue(sum.getParametersWithoutReturnType().get(1).isVarargs());
	}

	@Test
	void cleansFunctionSpecifiersAndNamesUnnamedCParameters() {
		// Covers a function whose storage specifier must be removed from the UML return type.
		String filePath = "src/api.c";
		String fileContent = String.join("\n",
				"extern unsigned long size(unsigned long) {",
				"  return 0;",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		assertEquals(1, model.getClassList().size());

		UMLClass moduleClass = model.getClassList().get(0);
		assertEquals("api", moduleClass.getName());
		assertTrue(moduleClass.isModule());

		// Unnamed C parameters get stable fallback names like "arg0".
		UMLOperation size = findOperation(moduleClass.getOperations(), "size");
		assertEquals("api", size.getClassName());
		assertFalse(size.isStatic());
		assertFalse(size.isInline());
		assertEquals("unsigned long", size.getReturnParameter().getType().toString());
		assertEquals(List.of("arg0"), size.getParameterNameList());
		assertEquals(List.of("unsigned long"), parameterTypeNames(size));
		// Even unnamed parameters get a VariableDeclaration, with "arg0" used as their stable UML name.
		assertNotNull(size.getParametersWithoutReturnType().get(0).getVariableDeclaration());
	}

	@Test
	void extractsDotSeparatedQualifiedNamespaceFromCdtBinding() throws Exception {
		String fileContent = "namespace modern::nested::syntax { int value; }\n";
		IASTTranslationUnit translationUnit = parseCppTranslationUnit("src/Sample.cpp", fileContent);

		CPPASTNamespaceDefinition modern = (CPPASTNamespaceDefinition) translationUnit.getDeclarations()[0];
		CPPASTNamespaceDefinition nested = (CPPASTNamespaceDefinition) modern.getDeclarations()[0];
		CPPASTNamespaceDefinition syntax = (CPPASTNamespaceDefinition) nested.getDeclarations()[0];

		assertEquals("modern", extractQualifiedNamespace(modern, "Sample"));
		assertEquals("modern.nested", extractQualifiedNamespace(nested, "modern"));
		assertEquals("modern.nested.syntax", extractQualifiedNamespace(syntax, "modern.nested"));
	}

	private static UMLOperation findOperation(List<UMLOperation> operations, String name) {
		return operations.stream()
				.filter(operation -> operation.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected operation: " + name));
	}

	private static List<String> parameterTypeNames(UMLOperation operation) {
		return operation.getParameterTypeList().stream()
				.map(Object::toString)
				.toList();
	}

	private static String extractQualifiedNamespace(CPPASTNamespaceDefinition namespaceDefinition, String namespaceContext) throws Exception {
		Method method = CppFileProcessor.class.getDeclaredMethod(
				"extractQualifiedNamespace", CPPASTNamespaceDefinition.class, String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, namespaceDefinition, namespaceContext);
	}

	private static IASTTranslationUnit parseCppTranslationUnit(String filePath, String source) throws CoreException {
		FileContent fileContent = FileContent.create(filePath, source.toCharArray());
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
