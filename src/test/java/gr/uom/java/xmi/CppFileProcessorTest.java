package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLTypeParameterDiff;


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
	void processesCppFunctionDefinitionsIntoNamespaceQualifiedOperations() {
		String filePath = "src/math.cpp";
		String fileContent = String.join("\n",
				"namespace outer {",
				"namespace inner {",
				"static inline int add(int lhs, int rhs) {",
				"  return lhs + rhs;",
				"}",
				"}",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass moduleClass = findClass(model.getClassList(), "math");
		assertTrue(moduleClass.isModule());

		UMLOperation add = findOperation(moduleClass.getOperations(), "add");
		assertTrue(add.getClassName().contains("outer.inner"));
		assertTrue(add.isStatic());
		assertTrue(add.isInline());
		assertEquals("int", add.getReturnParameter().getType().toString());
		assertEquals(List.of("lhs", "rhs"), add.getParameterNameList());
		assertEquals(List.of("int", "int"), parameterTypeNames(add));
	}

	@Test
	void processesCppClassesUnderNamespaces() {
		String filePath = "src/widgets.cpp";
		String fileContent = String.join("\n",
				"namespace outer {",
				"namespace inner {",
				"class Widget {",
				"public:",
				"  int value() {",
				"    return 1;",
				"  }",
				"};",
				"}",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass widget = findClass(model.getClassList(), "Widget");
		assertTrue(widget.getPackageName().contains("outer.inner"));

		UMLOperation value = findOperation(widget.getOperations(), "value");
		assertTrue(value.getClassName().contains("outer.inner"));
		assertTrue(value.getClassName().contains("Widget"));
	}

	@Test
	void appliesCppVisibilityLabelsToFunctionDefinitions() {
		String filePath = "src/widget.cpp";
		String fileContent = String.join("\n",
				"class Widget {",
				"public:",
				"  int visible() {",
				"    return 1;",
				"  }",
				"protected:",
				"  int inherited() {",
				"    return 2;",
				"  }",
				"private:",
				"  int hidden() {",
				"    return 3;",
				"  }",
				"};") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass widget = findClass(model.getClassList(), "Widget");
		assertEquals(Visibility.PUBLIC, findOperation(widget.getOperations(), "visible").getVisibility());
		assertEquals(Visibility.PROTECTED, findOperation(widget.getOperations(), "inherited").getVisibility());
		assertEquals(Visibility.PRIVATE, findOperation(widget.getOperations(), "hidden").getVisibility());
	}

	@Test
	void processesCppRangeBasedForStatementsAsEnhancedForStatements() {
		String filePath = "src/ranges.cpp";
		String fileContent = String.join("\n",
				"void visit() {",
				"  int values[3] = {1, 2, 3};",
				"  for (int value : values) {",
				"    value += 1;",
				"  }",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass moduleClass = findClass(model.getClassList(), "ranges");
		UMLOperation testedOperation = findOperation(moduleClass.getOperations(), "visit");

		List<CompositeStatementObject> enhancedForStatements = testedOperation.getBody().getCompositeStatement().getInnerNodes().stream()
				.filter(statement -> statement.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT))
				.toList();
		assertEquals(1, enhancedForStatements.size());

		CompositeStatementObject enhancedFor = enhancedForStatements.get(0);
		assertEquals(List.of("value"), enhancedFor.getVariableDeclarations().stream()
				.map(VariableDeclaration::getVariableName)
				.toList());
		assertTrue(enhancedFor.getExpressions().stream()
				.anyMatch(expression -> expression.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION)));

		AbstractCodeFragment loopBodyStatement = enhancedFor.getLeaves().stream()
				.filter(statement -> statement.getString().contains("value += 1"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected range-for body statement"));
		assertTrue(testedOperation.getVariableDeclarationsInScope(loopBodyStatement.getLocationInfo()).stream()
				.anyMatch(variableDeclaration -> variableDeclaration.getVariableName().equals("value")));
	}

	@Test
	void processesCppTryStatementsWithCatchClauses() {
		String filePath = "src/errors.cpp";
		String fileContent = String.join("\n",
				"void handle() {",
				"  try {",
				"    risky();",
				"  } catch (const int& ex) {",
				"    recover(ex);",
				"  }",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass moduleClass = findClass(model.getClassList(), "errors");
		UMLOperation testedOperation = findOperation(moduleClass.getOperations(), "handle");

		List<CompositeStatementObject> tryStatements = testedOperation.getBody().getCompositeStatement().getInnerNodes().stream()
				.filter(statement -> statement.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT))
				.toList();
		assertEquals(1, tryStatements.size());
		TryStatementObject tryStatement = (TryStatementObject) tryStatements.get(0);

		List<CompositeStatementObject> catchClauses = testedOperation.getBody().getCompositeStatement().getInnerNodes().stream()
				.filter(statement -> statement.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE))
				.toList();
		assertEquals(1, catchClauses.size());

		CompositeStatementObject catchClause = catchClauses.get(0);
		assertEquals(List.of(catchClause), tryStatement.getCatchClauses());
		assertTrue(catchClause.getTryContainer().isPresent());
		assertSame(tryStatement, catchClause.getTryContainer().get());
		assertEquals(List.of("ex"), catchClause.getVariableDeclarations().stream()
				.map(VariableDeclaration::getVariableName)
				.toList());

		AbstractCodeFragment catchBodyStatement = catchClause.getLeaves().stream()
				.filter(statement -> statement.getString().contains("recover"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected catch body statement"));
		assertTrue(testedOperation.getVariableDeclarationsInScope(catchBodyStatement.getLocationInfo()).stream()
				.anyMatch(variableDeclaration -> variableDeclaration.getVariableName().equals("ex")));
	}

	@Test
	void scopesCppControlStatementDeclarationsToTheirBodies() {
		String filePath = "src/controls.cpp";
		String fileContent = String.join("\n",
				"void controls() {",
				"  if (int flag = 1) {",
				"    flag += 1;",
				"  }",
				"  while (int item = 1) {",
				"    item += 1;",
				"    break;",
				"  }",
				"  switch (int code = 1) {",
				"    case 1:",
				"      code += 1;",
				"      break;",
				"  }",
				"  for (int i = 0; int keep = i < 1; ++i) {",
				"    keep += 1;",
				"  }",
				"}") + "\n";

		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);

		processor.processCppFile(filePath, fileContent, false);

		UMLClass moduleClass = findClass(model.getClassList(), "controls");
		UMLOperation controls = findOperation(moduleClass.getOperations(), "controls");

		assertVariableInScope(controls, "flag += 1", "flag");
		assertVariableInScope(controls, "item += 1", "item");
		assertVariableInScope(controls, "code += 1", "code");
		assertVariableInScope(controls, "keep += 1", "keep");
		assertVariableInScope(controls, "keep += 1", "i");
	}

	@Test
	void skipsUnnamedCppNonTypeTemplateParameters() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <int>",
				"class Buffer {};",
				"template <int>",
				"int fixed() {",
				"  return 1;",
				"}") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLClass buffer = findClass(model.getClassList(), "Buffer");
		assertTrue(buffer.getTypeParameters().isEmpty());

		UMLOperation fixed = findOperation(findClass(model.getClassList(), "templates").getOperations(), "fixed");
		assertTrue(fixed.getTypeParameters().isEmpty());
	}

	@Test
	void processesCppAliasDeclarations() {
		String filePath = "src/aliases.cpp";
		String fileContent = String.join("\n",
				"using Distance = unsigned long;",
				"class Box {",
				"public:",
				"  using Size = int;",
				"};") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLTypeAlias distance = findTypeAlias(findClass(model.getClassList(), "aliases"), "Distance");
		assertEquals("unsigned long", distance.getRightType().toString());
		assertEquals(CodeElementType.TYPE_ALIAS, distance.getLocationInfo().getCodeElementType());

		UMLTypeAlias size = findTypeAlias(findClass(model.getClassList(), "Box"), "Size");
		assertEquals("int", size.getRightType().toString());
		assertEquals(CodeElementType.TYPE_ALIAS, size.getLocationInfo().getCodeElementType());
	}

	@Test
	void processesCppTemplateAliasDeclarations() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <typename T>",
				"using Vec = std::vector<T>;") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLTypeAlias vec = findTypeAlias(findClass(model.getClassList(), "templates"), "Vec");
		assertEquals("std::vector<T>", vec.getRightType().toString());
		assertEquals("typealias Vec<T> = std::vector<T>", vec.toString());
	}

	@Test
	void processesCppVariableTemplateDeclarations() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <typename T>",
				"constexpr T pi = T(3.1415926535897932385);",
				"template <typename T, int N>",
				"T scaled = T(N);") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "templates");

		UMLAttribute pi = findAttribute(moduleClass.getAttributes(), "pi");
		assertEquals("T", pi.getType().toString());
		assertEquals(List.of("T"), pi.getTypeParameterNames());
		assertEquals("public pi<T> : T", pi.toString());

		UMLAttribute scaled = findAttribute(moduleClass.getAttributes(), "scaled");
		assertEquals("T", scaled.getType().toString());
		assertEquals(List.of("T", "N"), scaled.getTypeParameterNames());
		assertEquals("public scaled<T,N> : T", scaled.toString());
	}

	@Test
	void processesCppUsingDirectivesAsImports() {
		String filePath = "src/usings.cpp";
		String fileContent = String.join("\n",
				"using namespace std;",
				"using std::string;",
				"namespace fs = std::filesystem;",
				"namespace local {",
				"  using namespace outer::inner;",
				"  using outer::inner::Thing;",
				"  namespace rng = std::ranges;",
				"}") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "usings");

		UMLImport moduleImport = findImport(moduleClass, "std");
		assertTrue(moduleImport.isOnDemand());
		assertFalse(moduleImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, moduleImport.getLocationInfo().getCodeElementType());

		UMLImport usingDeclarationImport = findImport(moduleClass, "std.string");
		assertFalse(usingDeclarationImport.isOnDemand());
		assertFalse(usingDeclarationImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, usingDeclarationImport.getLocationInfo().getCodeElementType());

		UMLImport namespaceAliasImport = findImport(moduleClass, "std.filesystem");
		assertFalse(namespaceAliasImport.isOnDemand());
		assertFalse(namespaceAliasImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, namespaceAliasImport.getLocationInfo().getCodeElementType());

		UMLImport namespaceImport = findImport(moduleClass, "outer.inner");
		assertTrue(namespaceImport.isOnDemand());
		assertFalse(namespaceImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, namespaceImport.getLocationInfo().getCodeElementType());

		UMLImport namespaceUsingDeclarationImport = findImport(moduleClass, "outer.inner.Thing");
		assertFalse(namespaceUsingDeclarationImport.isOnDemand());
		assertFalse(namespaceUsingDeclarationImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, namespaceUsingDeclarationImport.getLocationInfo().getCodeElementType());

		UMLImport nestedNamespaceAliasImport = findImport(moduleClass, "std.ranges");
		assertFalse(nestedNamespaceAliasImport.isOnDemand());
		assertFalse(nestedNamespaceAliasImport.isStatic());
		assertEquals(CodeElementType.IMPORT_DECLARATION, nestedNamespaceAliasImport.getLocationInfo().getCodeElementType());
	}

	@Test
	void processesCppLinkageSpecificationDeclarations() {
		String filePath = "src/linkage.cpp";
		String fileContent = String.join("\n",
				"extern \"C\" {",
				"  int declared(int value);",
				"  int defined(int value) {",
				"    return value;",
				"  }",
				"}",
				"extern \"C\" int single(int value);") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "linkage");

		UMLOperation declared = findOperation(moduleClass.getOperations(), "declared");
		assertEquals("int", declared.getReturnParameter().getType().toString());
		assertEquals(List.of("value"), declared.getParameterNameList());

		UMLOperation defined = findOperation(moduleClass.getOperations(), "defined");
		assertEquals("int", defined.getReturnParameter().getType().toString());
		assertNotNull(defined.getBody());

		UMLOperation single = findOperation(moduleClass.getOperations(), "single");
		assertEquals("int", single.getReturnParameter().getType().toString());
		assertEquals(List.of("value"), single.getParameterNameList());
	}

	@Test
	void processesCppTemplateSpecializations() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <typename T>",
				"class Box {",
				"public:",
				"  T value(T input) {",
				"    return input;",
				"  }",
				"};",
				"template <>",
				"class Box<int> {",
				"public:",
				"  int value(int input) {",
				"    return input + 1;",
				"  }",
				"};",
				"template <typename T>",
				"T identity(T value) {",
				"  return value;",
				"}",
				"template <>",
				"int identity<int>(int value) {",
				"  return value + 1;",
				"}") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLClass specializedBox = model.getClassList().stream()
				.filter(umlClass -> umlClass.getName().contains("Box<int>"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected specialized class"));
		assertTrue(specializedBox.getActualSignature().contains("class Box<int>"));
		assertTrue(specializedBox.getTypeParameters().isEmpty());
		assertEquals("int", findOperation(specializedBox.getOperations(), "value").getReturnParameter().getType().toString());

		UMLOperation specializedIdentity = findClass(model.getClassList(), "templates").getOperations().stream()
				.filter(operation -> operation.getActualSignature() != null && operation.getActualSignature().contains("<int>"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected specialized function"));
		assertTrue(specializedIdentity.getActualSignature().contains("identity<int>"));
		assertTrue(specializedIdentity.getTypeParameters().isEmpty());
		assertEquals("int", specializedIdentity.getReturnParameter().getType().toString());
	}

	@Test
	void processesCppTemplateExplicitInstantiation() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <typename T>",
				"T identity(T value) {",
				"  return value;",
				"}",
				"template int identity<int>(void);") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLOperation instantiatedIdentity = findClass(model.getClassList(), "templates").getOperations().stream()
				.filter(operation -> operation.getActualSignature() != null && operation.getActualSignature().contains("identity<int>"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected explicit template instantiation"));

		assertEquals("identity<int>", instantiatedIdentity.getName());
		assertEquals("int", instantiatedIdentity.getReturnParameter().getType().toString());
		assertTrue(instantiatedIdentity.getTypeParameters().isEmpty());
	}
	
	@Test
	void processesCppPartialTemplateSpecializations() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"template <typename T>",
				"class Box {",
				"public:",
				"  T value(T input) {",
				"    return input;",
				"  }",
				"};",
				"template <typename T>",
				"class Box<T*> {",
				"public:",
				"  T* value(T* input) {",
				"    return input;",
				"  }",
				"};") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		UMLClass partialBox = model.getClassList().stream()
				.filter(umlClass -> umlClass.getName().contains("Box<T*>"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected partial specialized class"));
		assertTrue(partialBox.getActualSignature().contains("class Box<T*>"));
		assertEquals(List.of("T"), partialBox.getTypeParameterNames());
		assertEquals("T*", findOperation(partialBox.getOperations(), "value").getReturnParameter().getType().toString().replace(" ", ""));
	}

	private static UMLOperation findOperation(List<UMLOperation> operations, String name) {
		return operations.stream()
				.filter(operation -> operation.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected operation: " + name));
	}

	private static UMLClass findClass(List<UMLClass> classes, String name) {
		return classes.stream()
				.filter(umlClass -> umlClass.getName().equals(name) || umlClass.getName().endsWith("." + name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected class: " + name));
	}

	private static UMLTypeAlias findTypeAlias(UMLClass umlClass, String name) {
		return umlClass.getTypeAliasList().stream()
				.filter(typeAlias -> typeAlias.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected type alias: " + name));
	}

	private static UMLAttribute findAttribute(List<UMLAttribute> attributes, String name) {
		return attributes.stream()
				.filter(attribute -> attribute.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected attribute: " + name));
	}

	private static UMLImport findImport(UMLClass umlClass, String name) {
		return umlClass.getImportedTypes().stream()
				.filter(umlImport -> umlImport.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected import: " + name));
	}

	private static List<String> parameterTypeNames(UMLOperation operation) {
		return operation.getParameterTypeList().stream()
				.map(Object::toString)
				.toList();
	}

	private static void assertVariableInScope(UMLOperation operation, String statementText, String variableName) {
		AbstractCodeFragment statement = operation.getBody().getCompositeStatement().getLeaves().stream()
				.filter(leaf -> leaf.getString().contains(statementText))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected statement: " + statementText));
		assertTrue(operation.getVariableDeclarationsInScope(statement.getLocationInfo()).stream()
				.anyMatch(variableDeclaration -> variableDeclaration.getVariableName().equals(variableName)));
	}

	private static UMLModel processCppModel(String filePath, String fileContent) {
		UMLModel model = new UMLModel(Set.of("src"));
		CppFileProcessor processor = new CppFileProcessor(model);
		processor.processCppFile(filePath, fileContent, false);
		return model;
	}
}
