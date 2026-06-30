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
		UMLOperation visit = findOperation(moduleClass.getOperations(), "visit");

		List<CompositeStatementObject> enhancedForStatements = visit.getBody().getCompositeStatement().getInnerNodes().stream()
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
		assertTrue(visit.getVariableDeclarationsInScope(loopBodyStatement.getLocationInfo()).stream()
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
		UMLOperation handle = findOperation(moduleClass.getOperations(), "handle");

		List<CompositeStatementObject> tryStatements = handle.getBody().getCompositeStatement().getInnerNodes().stream()
				.filter(statement -> statement.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT))
				.toList();
		assertEquals(1, tryStatements.size());
		TryStatementObject tryStatement = (TryStatementObject) tryStatements.get(0);

		List<CompositeStatementObject> catchClauses = handle.getBody().getCompositeStatement().getInnerNodes().stream()
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
		assertTrue(handle.getVariableDeclarationsInScope(catchBodyStatement.getLocationInfo()).stream()
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
}
