package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLPreprocessorStatement.Directive;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;


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
		assertEquals(List.of("count"), sum.getParameterNameList());
		assertEquals(List.of("int"), parameterTypeNames(sum));
		// Only the real C parameter has a VariableDeclaration; the ellipsis varargs parameter is synthetic.
		assertNotNull(sum.getParametersWithoutReturnType().get(0).getVariableDeclaration());
		//assertTrue(sum.getParametersWithoutReturnType().get(1).isVarargs());
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
	void processesCppDeclarationsInInactivePreprocessorBlocks() {
		String filePath = "src/features.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_FEATURE)",
				"namespace feature {",
				"struct Matcher {",
				"  bool match() {",
				"    return true;",
				"  }",
				"};",
				"extern \"C\" {",
				"  void inactiveBranch() {",
				"  }",
				"}",
				"}",
				"#endif",
				"void activeBranch() {",
				"}") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "features");

		assertEquals(List.of("inactiveBranch", "activeBranch"), moduleClass.getOperations().stream()
				.map(UMLOperation::getName)
				.toList());
		assertEquals(List.of("match"), findClass(model.getClassList(), "Matcher").getOperations().stream()
				.map(UMLOperation::getName)
				.toList());
	}

	@Test
	void processesCatch2ShapedDeclarationsInInactiveCppBlock() {
		String filePath = "tests/SelfTest/UsageTests/Matchers.tests.cpp";
		String fileContent = String.join("\n",
				"#if defined(CATCH_INTERNAL_CONSTEXPR_MATCHERS_ENABLED)",
				"namespace {",
				"struct MatchAllMatcher final : public Catch::Matchers::MatcherGenericBase {",
				"public:",
				"  template <typename Any>",
				"  constexpr bool match(Any&&) const { return true; }",
				"  std::string describe() const override { return {}; }",
				"};",
				"constexpr MatchAllMatcher MatchAll() { return MatchAllMatcher(); }",
				"}",
				"#endif") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass matcher = findClass(model.getClassList(), "MatchAllMatcher");
		UMLOperation match = findOperation(matcher.getOperations(), "match");

		assertEquals(List.of("Any"), match.getTypeParameterNames());
		assertEquals(1, match.getParameterTypeList().size());
		assertTrue(match.getParameterTypeList().get(0).toString().contains("Any"));
		assertNotNull(findOperation(findClass(model.getClassList(), "Matchers.tests").getOperations(), "MatchAll"));
	}

	@Test
	void keepsCppOperationVariantsOfSiblingPreprocessorBranches() {
		String filePath = "src/platform.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_FEATURE)",
				"int mode() {",
				"  return 1;",
				"}",
				"#else",
				"int mode() {",
				"  return 2;",
				"}",
				"#endif",
				"#if defined(UNDEFINED_FEATURE)",
				"void inactiveOnly() {",
				"}",
				"#endif") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "platform");

		// Same-signature definitions in mutually exclusive branches are all kept, each identified by its branch,
		// and inactive declarations without a counterpart are preserved too.
		assertEquals(List.of("mode", "mode", "inactiveOnly"), moduleClass.getOperations().stream()
				.map(UMLOperation::getName)
				.toList());
		List<UMLOperation> modes = siblingVariants(moduleClass.getOperations(), "mode", 2);
		assertTrue(bodyContains(modes.get(1), "return 2"));
		assertNull(findOperation(moduleClass.getOperations(), "inactiveOnly").getPreprocessorBranch());
	}

	@Test
	void identifiesCppOperationVariantsByTheirBranch() {
		String filePath = "src/active-first.cpp";
		String fileContent = String.join("\n",
				"#if 1",
				"int mode() { return 2; }",
				"#else",
				"int mode() { return 1; }",
				"#endif") + "\n";

		List<UMLOperation> modes = findClass(processCppModel(filePath, fileContent).getClassList(), "active-first").getOperations().stream()
				.filter(operation -> operation.getName().equals("mode"))
				.toList();

		assertEquals(2, modes.size());
		assertEquals("#if 1[0]", modes.get(0).getPreprocessorBranch());
		assertEquals("#if 1[1]", modes.get(1).getPreprocessorBranch());
		assertTrue(bodyContains(modes.get(0), "return 2"));
	}

	@Test
	void keepsCppOperationVariantsWhenSiblingBranchParameterNamesDiffer() {
		String filePath = "src/buffer.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_FEATURE)",
				"int total(int c[count]) {",
				"  return 1;",
				"}",
				"#else",
				"int total(int n[count]) {",
				"  return 2;",
				"}",
				"#endif") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "buffer");

		// Parameter names differ across sibling branches and appear as substrings of other declarator
		// tokens, but the modeled identity depends only on the parameter type, so both are variants of one function.
		List<UMLOperation> totals = siblingVariants(moduleClass.getOperations(), "total", 2);
		assertTrue(bodyContains(totals.get(1), "return 2"));
	}

	@Test
	void normalizesOnlyTopLevelCvQualifiersInCppOperationIdentity() {
		String filePath = "src/cv-parameters.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"void value(const int input);",
				"void wrapped(const int (input));",
				"void state(volatile int input);",
				"void pointer(int* const input);",
				"void pointee(const int* input);",
				"#else",
				"void value(int input);",
				"void wrapped(int input);",
				"void state(int input);",
				"void pointer(int* input);",
				"void pointee(int* input);",
				"#endif") + "\n";

		List<UMLOperation> operations = findClass(processCppModel(filePath, fileContent).getClassList(), "cv-parameters").getOperations();

		// Top-level cv-qualifiers do not change the identity, so these are variants of the same function in sibling branches.
		siblingVariants(operations, "value", 2);
		siblingVariants(operations, "wrapped", 2);
		siblingVariants(operations, "state", 2);
		siblingVariants(operations, "pointer", 2);
		// A pointee qualifier changes the identity, so these are different functions with no branch identity.
		List<UMLOperation> pointees = operations.stream().filter(operation -> operation.getName().equals("pointee")).toList();
		assertEquals(2, pointees.size());
		assertTrue(pointees.stream().allMatch(operation -> operation.getPreprocessorBranch() == null));
	}

	@Test
	void prefersActiveCppClassOverInactiveSiblingPreprocessorBranch() {
		String filePath = "src/config.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_FEATURE)",
				"struct Settings {",
				"  int cacheSize;",
				"};",
				"#else",
				"struct Settings {",
				"  long cacheSize;",
				"};",
				"#endif") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		List<UMLClass> settingsClasses = model.getClassList().stream()
				.filter(umlClass -> umlClass.getName().endsWith("Settings"))
				.toList();
		assertEquals(1, settingsClasses.size());
		assertEquals("long", findAttribute(settingsClasses.get(0).getAttributes(), "cacheSize").getType().toString());
	}

	@Test
	void preservesActiveCppVisibilityAcrossInactivePreprocessorAlternatives() {
		String filePath = "src/widget.cpp";
		String fileContent = String.join("\n",
				"class Widget {",
				"#if 1",
				"public:",
				"#else",
				"private:",
				"#endif",
				"  void visible() {",
				"  }",
				"};") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);

		assertEquals(Visibility.PUBLIC, findOperation(findClass(model.getClassList(), "Widget").getOperations(), "visible")
				.getVisibility());
	}

	@Test
	void mergesInactiveOnlyMembersIntoActiveCppSiblingClass() {
		String filePath = "src/backend.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_PLATFORM)",
				"struct Backend {",
				"  int common() { return 1; }",
				"  void windowsOnly() {}",
				"};",
				"#else",
				"struct Backend {",
				"  int common() { return 2; }",
				"  void posixOnly() {}",
				"};",
				"#endif") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		List<UMLClass> backends = model.getClassList().stream()
				.filter(umlClass -> umlClass.getName().equals("Backend") || umlClass.getName().endsWith(".Backend"))
				.toList();

		assertEquals(1, backends.size(), model.getClassList().stream().map(UMLClass::getName).toList().toString());
		List<String> operationNames = backends.get(0).getOperations().stream()
				.map(UMLOperation::getName)
				.toList();
		assertEquals(4, operationNames.size());
		assertTrue(operationNames.containsAll(List.of("common", "windowsOnly", "posixOnly")));
		List<UMLOperation> commons = siblingVariants(backends.get(0).getOperations(), "common", 2);
		assertEquals(1, commons.stream().filter(operation -> bodyContains(operation, "return 2")).count());
	}

	@Test
	void mergesCppNamespaceSiblingBranchesAtDeclarationGranularity() {
		String filePath = "src/platform.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_PLATFORM)",
				"namespace platform {",
				"  int common() { return 1; }",
				"  void inactiveOnly() {}",
				"}",
				"#else",
				"namespace platform {",
				"  int common() { return 2; }",
				"  void activeOnly() {}",
				"}",
				"#endif") + "\n";

		UMLClass moduleClass = findClass(processCppModel(filePath, fileContent).getClassList(), "platform");
		List<String> operationNames = moduleClass.getOperations().stream()
				.map(UMLOperation::getName)
				.toList();

		assertEquals(4, operationNames.size());
		assertTrue(operationNames.containsAll(List.of("common", "inactiveOnly", "activeOnly")));
		List<UMLOperation> commons = siblingVariants(moduleClass.getOperations(), "common", 2);
		assertEquals(1, commons.stream().filter(operation -> bodyContains(operation, "return 2")).count());
	}

	@Test
	void doesNotReportChangesForCppSiblingCollisionSelfDiff() throws Exception {
		String filePath = "src/platform.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_PLATFORM)",
				"int mode() { return 1; }",
				"struct Settings { int first; };",
				"#else",
				"int mode() { return 2; }",
				"struct Settings { int second; };",
				"#endif") + "\n";

		UMLModelDiff diff = processCppModel(filePath, fileContent).diff(processCppModel(filePath, fileContent));

		assertTrue(diff.getRefactorings().isEmpty());
		assertTrue(diff.getAddedClasses().isEmpty());
		assertTrue(diff.getRemovedClasses().isEmpty());
		assertNotNull(diff.getUMLClassDiff("platform"));
		// Each variant is mapped to the variant of the same branch.
		List<UMLOperationBodyMapper> modeMappers = diff.getUMLClassDiff("platform").getOperationBodyMapperList().stream()
				.filter(mapper -> mapper.getOperation1().getName().equals("mode"))
				.toList();
		assertEquals(2, modeMappers.size());
		assertTrue(modeMappers.stream().allMatch(mapper -> mapper.getOperation1().getPreprocessorBranch() != null &&
				mapper.getOperation1().getPreprocessorBranch().equals(mapper.getOperation2().getPreprocessorBranch())));
	}

	@Test
	void preservesCppDeclarationDefinitionPairAcrossIndependentConditionals() {
		String filePath = "src/platform.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"int mode();",
				"#endif",
				"#if 1",
				"int mode() { return 2; }",
				"#endif") + "\n";

		List<UMLOperation> modes = findClass(processCppModel(filePath, fileContent).getClassList(), "platform").getOperations().stream()
				.filter(operation -> operation.getName().equals("mode"))
				.toList();

		assertEquals(2, modes.size());
		assertEquals(1, modes.stream().filter(operation -> operation.getBody() == null).count());
		assertEquals(1, modes.stream().filter(operation -> operation.getBody() != null).count());
	}

	@Test
	void keepsCppSiblingVariantsWithinEachClass() {
		String filePath = "src/containers.cpp";
		String fileContent = String.join("\n",
				"class ActiveContainer {",
				"#if 0",
				"  int value() { return 1; }",
				"#else",
				"  int value() { return 2; }",
				"#endif",
				"};",
				"class InactiveOnlyContainer {",
				"#if 0",
				"  int value() { return 3; }",
				"#endif",
				"};") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass activeContainer = findClass(model.getClassList(), "ActiveContainer");
		UMLClass inactiveOnlyContainer = findClass(model.getClassList(), "InactiveOnlyContainer");

		siblingVariants(activeContainer.getOperations(), "value", 2);
		// The same signature in another class is not a sibling variant.
		assertEquals(1, inactiveOnlyContainer.getOperations().stream().filter(operation -> operation.getName().equals("value")).count());
		assertNull(findOperation(inactiveOnlyContainer.getOperations(), "value").getPreprocessorBranch());
	}

	@Test
	void filtersCppSiblingFieldsAtDeclaratorGranularity() {
		String filePath = "src/fields.cpp";
		String fileContent = String.join("\n",
				"struct Fields {",
				"#if 0",
				"  int shared, inactiveOnly;",
				"#else",
				"  long shared;",
				"#endif",
				"};") + "\n";

		UMLClass fields = findClass(processCppModel(filePath, fileContent).getClassList(), "Fields");
		List<String> attributeNames = fields.getAttributes().stream()
				.map(UMLAttribute::getName)
				.toList();

		assertEquals(2, attributeNames.size());
		assertEquals(1, attributeNames.stream().filter("shared"::equals).count());
		assertTrue(attributeNames.contains("inactiveOnly"));
		assertEquals("long", findAttribute(fields.getAttributes(), "shared").getType().toString());
	}

	@Test
	void filtersCppUsingDirectivesInSiblingBranches() {
		String filePath = "src/imports.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"using namespace shared;",
				"#else",
				"using namespace shared;",
				"#endif") + "\n";

		UMLClass moduleClass = findClass(processCppModel(filePath, fileContent).getClassList(), "imports");

		assertEquals(1, moduleClass.getImportedTypes().stream()
				.filter(umlImport -> umlImport.getName().equals("shared"))
				.count());
	}

	@Test
	void preservesCppSiblingTemplatesWithDifferentTemplateParameters() {
		String filePath = "src/templates.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename T>",
				"void configure(int value) {}",
				"#else",
				"template <int N>",
				"void configure(int value) {}",
				"#endif") + "\n";

		List<UMLOperation> configureOperations = findClass(processCppModel(filePath, fileContent).getClassList(), "templates").getOperations().stream()
				.filter(operation -> operation.getName().equals("configure"))
				.toList();

		assertEquals(2, configureOperations.size());
		assertTrue(configureOperations.stream().anyMatch(operation -> operation.getTypeParameterNames().contains("T")));
		assertTrue(configureOperations.stream().anyMatch(operation -> operation.getTypeParameterNames().contains("N")));
	}

	@Test
	void preservesCppSiblingFunctionTemplatesWithDifferentParameterKinds() {
		String filePath = "src/template-kinds.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename T>",
				"void configure(int value) {}",
				"#else",
				"template <int T>",
				"void configure(int value) {}",
				"#endif") + "\n";

		long configureCount = findClass(processCppModel(filePath, fileContent).getClassList(), "template-kinds").getOperations().stream()
				.filter(operation -> operation.getName().equals("configure"))
				.count();

		assertEquals(2, configureCount);
	}

	@Test
	void recognizesCppFunctionTemplateVariantsAcrossParameterRenames() {
		String filePath = "src/template-renames.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename _Tp>",
				"int configure(_Tp value) { return 1; }",
				"#else",
				"template <typename _Up>",
				"int configure(_Up value) { return 2; }",
				"#endif") + "\n";

		List<UMLOperation> configureOperations = findClass(processCppModel(filePath, fileContent).getClassList(), "template-renames").getOperations().stream()
				.filter(operation -> operation.getName().equals("configure"))
				.toList();

		// Renamed template parameters do not change the identity, so both are variants of one function template.
		assertEquals(2, configureOperations.size());
		assertEquals(2, configureOperations.stream().map(UMLOperation::getPreprocessorBranch).filter(Objects::nonNull).distinct().count());
	}

	@Test
	void preservesCppFunctionTemplatesWhenNamedParametersOccupyDifferentPositions() {
		String filePath = "src/template-positions.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename T, typename>",
				"void configure(T value) {}",
				"#else",
				"template <typename, typename U>",
				"void configure(U value) {}",
				"#endif") + "\n";

		long configureCount = findClass(processCppModel(filePath, fileContent).getClassList(), "template-positions").getOperations().stream()
				.filter(operation -> operation.getName().equals("configure"))
				.count();

		assertEquals(2, configureCount);
	}

	@Test
	void preservesCppSiblingClassTemplatesWithDifferentTemplateParameters() {
		String filePath = "src/template-classes.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename T>",
				"struct Box { T inactiveOnly(); };",
				"#else",
				"template <int N>",
				"struct Box { int activeOnly(); };",
				"#endif") + "\n";

		List<UMLClass> boxes = processCppModel(filePath, fileContent).getClassList().stream()
				.filter(umlClass -> umlClass.getName().equals("Box") || umlClass.getName().endsWith(".Box"))
				.toList();

		assertEquals(2, boxes.size());
		assertTrue(boxes.stream().anyMatch(umlClass -> umlClass.getTypeParameterNames().contains("T")));
		assertTrue(boxes.stream().anyMatch(umlClass -> umlClass.getTypeParameterNames().contains("N")));
	}

	@Test
	void keepsAlphaRenamedCppClassTemplatesSeparateWithoutMixingMemberTypes() {
		String filePath = "src/renamed-template-classes.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"template <typename U>",
				"struct Box { U inactiveValue; };",
				"#else",
				"template <typename T>",
				"struct Box { T activeValue; };",
				"#endif") + "\n";

		List<UMLClass> boxes = processCppModel(filePath, fileContent).getClassList().stream()
				.filter(umlClass -> umlClass.getName().equals("Box") || umlClass.getName().endsWith(".Box"))
				.toList();

		assertEquals(2, boxes.size());
		assertEquals("U", findAttribute(boxes.stream()
				.filter(umlClass -> umlClass.getTypeParameterNames().contains("U"))
				.findFirst().orElseThrow().getAttributes(), "inactiveValue").getType().toString());
		assertEquals("T", findAttribute(boxes.stream()
				.filter(umlClass -> umlClass.getTypeParameterNames().contains("T"))
				.findFirst().orElseThrow().getAttributes(), "activeValue").getType().toString());
	}

	@Test
	void keepsVisibilityStateSeparateAcrossMergedCppClassAlternatives() {
		String filePath = "src/access.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"struct Access {",
				"public:",
				"  void inactiveOnly();",
				"};",
				"#else",
				"struct Access {",
				"private:",
				"  void activeOnly();",
				"};",
				"#endif") + "\n";

		UMLClass access = findClass(processCppModel(filePath, fileContent).getClassList(), "Access");

		assertEquals(Visibility.PRIVATE, findOperation(access.getOperations(), "activeOnly").getVisibility());
		assertEquals(Visibility.PUBLIC, findOperation(access.getOperations(), "inactiveOnly").getVisibility());
	}

	@Test
	void usesPrivateDefaultVisibilityForMergedCppClassAlternative() {
		String filePath = "src/class-access.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"class Access {",
				"  void inactiveOnly();",
				"};",
				"#else",
				"class Access {",
				"public:",
				"  void activeOnly();",
				"};",
				"#endif") + "\n";

		UMLClass access = findClass(processCppModel(filePath, fileContent).getClassList(), "Access");

		assertEquals(Visibility.PUBLIC, findOperation(access.getOperations(), "activeOnly").getVisibility());
		assertEquals(Visibility.PRIVATE, findOperation(access.getOperations(), "inactiveOnly").getVisibility());
	}

	@Test
	void keepsDefaultVisibilitySpecificToEachMergedCppCompositeAlternative() {
		String filePath = "src/mixed-composite-access.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"struct Access {",
				"  void inactiveOnly();",
				"};",
				"#else",
				"class Access {",
				"public:",
				"  void activeOnly();",
				"};",
				"#endif") + "\n";

		UMLClass access = findClass(processCppModel(filePath, fileContent).getClassList(), "Access");

		assertEquals(Visibility.PUBLIC, findOperation(access.getOperations(), "activeOnly").getVisibility());
		assertEquals(Visibility.PUBLIC, findOperation(access.getOperations(), "inactiveOnly").getVisibility());
	}

	@Test
	void preservesCppSiblingMemberFunctionsWithDifferentCvAndRefQualifiers() {
		String filePath = "src/qualified-members.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"struct Reader {",
				"  int value() const;",
				"  int access() &;",
				"};",
				"#else",
				"struct Reader {",
				"  int value();",
				"  int access() &&;",
				"};",
				"#endif") + "\n";

		UMLClass reader = findClass(processCppModel(filePath, fileContent).getClassList(), "Reader");

		assertEquals(2, reader.getOperations().stream().filter(operation -> operation.getName().equals("value")).count());
		assertEquals(2, reader.getOperations().stream().filter(operation -> operation.getName().equals("access")).count());
	}

	@Test
	void preservesCppSiblingDeclarationsWhenNoBranchIsActive() {
		String filePath = "src/unselected.cpp";
		String fileContent = String.join("\n",
				"#if defined(UNDEFINED_A)",
				"void mode();",
				"#elif defined(UNDEFINED_B)",
				"void mode();",
				"#endif") + "\n";

		long modeCount = findClass(processCppModel(filePath, fileContent).getClassList(), "unselected").getOperations().stream()
				.filter(operation -> operation.getName().equals("mode"))
				.count();

		assertEquals(2, modeCount);
	}

	@Test
	void recognizesCppOperationVariantsWhenOnlyDefaultArgumentDiffers() {
		String filePath = "src/defaults.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"void configure(int value = 1);",
				"#else",
				"void configure(int value = 2);",
				"#endif") + "\n";

		List<UMLOperation> configureOperations = findClass(processCppModel(filePath, fileContent).getClassList(), "defaults").getOperations().stream()
				.filter(operation -> operation.getName().equals("configure"))
				.toList();

		// Default arguments do not change the identity, so both declarations are variants of one function.
		assertEquals(2, configureOperations.size());
		assertEquals(2, configureOperations.stream().map(UMLOperation::getPreprocessorBranch).filter(Objects::nonNull).distinct().count());
		assertTrue(configureOperations.get(1).getActualSignature().contains("= 2"));
	}

	@Test
	void prefersActiveCppAliasOverInactiveSiblingBranch() {
		String filePath = "src/conditional-alias.cpp";
		String fileContent = String.join("\n",
				"#if 0",
				"using Size = int;",
				"#else",
				"using Size = long;",
				"#endif") + "\n";

		List<UMLTypeAlias> aliases = findClass(processCppModel(filePath, fileContent).getClassList(), "conditional-alias").getTypeAliasList().stream()
				.filter(alias -> alias.getName().equals("Size"))
				.toList();

		assertEquals(1, aliases.size());
		assertEquals("long", aliases.get(0).getRightType().toString());
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
		assertEquals("std.vector<T>", vec.getRightType().toString());
		assertEquals("typealias Vec<T> = std.vector<T>", vec.toString());
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
		assertEquals("public pi : T", pi.toString());

		UMLAttribute scaled = findAttribute(moduleClass.getAttributes(), "scaled");
		assertEquals("T", scaled.getType().toString());
		assertEquals(List.of("T", "N"), scaled.getTypeParameterNames());
		assertEquals("public scaled : T", scaled.toString());
	}

	@Test
	void processesCppStructuredBindingDeclarations() {
		String filePath = "src/bindings.cpp";
		String fileContent = String.join("\n",
				"struct Point {",
				"  int x;",
				"  int y;",
				"};",
				"Point point;",
				"auto [left, right] = point;") + "\n";

		UMLModel model = processCppModel(filePath, fileContent);
		UMLClass moduleClass = findClass(model.getClassList(), "bindings");

		UMLAttribute left = findAttribute(moduleClass.getAttributes(), "left");
		assertEquals("auto", left.getType().toString());
		assertEquals(CodeElementType.FIELD_DECLARATION, left.getLocationInfo().getCodeElementType());

		UMLAttribute right = findAttribute(moduleClass.getAttributes(), "right");
		assertEquals("auto", right.getType().toString());
		assertEquals(CodeElementType.FIELD_DECLARATION, right.getLocationInfo().getCodeElementType());
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

	@Test
	void preservesMacroArgumentLocationsDuringAstDiffProcessing() {
		String filePath = "src/assertions.cpp";
		String fileContent = String.join("\n",
				"#define ASSERT_EQ(lhs, rhs) if ((lhs) == (rhs)) {}",
				"void run() {",
				"  ASSERT_EQ(foo(), bar());",
				"}") + "\n";
		UMLModel model = new UMLModel(Set.of("src"));

		new CppFileProcessor(model).processCppFile(filePath, fileContent, true);

		UMLOperation run = findOperation(findClass(model.getClassList(), "assertions").getOperations(), "run");
		List<LocationInfo> invocationLocations = run.getAllOperationInvocations().stream()
				.map(AbstractCall::getLocationInfo)
				.toList();
		assertContainsLocation(invocationLocations, fileContent.indexOf("foo()"), "foo()".length());
		assertContainsLocation(invocationLocations, fileContent.indexOf("bar()"), "bar()".length());
	}

	@Test
	void excludesIncludedHeaderNodesFromSourceModel(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("fixture.h"), String.join("\n",
				"// HEADER_SENTINEL",
				"class HeaderOnly {",
				"public:",
				"  void hidden() {}",
				"};",
				"#define DECLARE_SOURCE_FUNCTION(name) void name() {}") + "\n");
		String fileContent = String.join("\n",
				"#include \"fixture.h\"",
				"// SOURCE_SENTINEL",
				"DECLARE_SOURCE_FUNCTION(sourceFunction)") + "\n";
		String filePath = tempDir.resolve("sample.cpp").toString();
		UMLModel model = new UMLModel(Set.of());

		new CppFileProcessor(model).processCppFile(filePath, fileContent, false);

		assertFalse(model.getClassList().stream()
				.anyMatch(umlClass -> umlClass.getName().equals("HeaderOnly") || umlClass.getName().endsWith(".HeaderOnly")));
		UMLClass moduleClass = findClass(model.getClassList(), "sample");
		assertNotNull(findOperation(moduleClass.getOperations(), "sourceFunction"));
		assertEquals(List.of(Directive.INCLUDE), moduleClass.getPreprocessorStatements().stream()
				.map(UMLPreprocessorStatement::getType)
				.toList());
		assertFalse(model.getCommentMap().values().stream()
				.flatMap(List::stream)
				.anyMatch(comment -> comment.getText().contains("HEADER_SENTINEL")));
		assertFalse(model.getClassList().stream()
				.flatMap(umlClass -> umlClass.getComments().stream())
				.anyMatch(comment -> comment.getText().contains("HEADER_SENTINEL")));
	}

	@Test
	void excludesIncludedHeaderNodesNestedInsideNamespace(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("nested.h"), String.join("\n",
				"class HeaderNested {",
				"public:",
				"  void hidden() {}",
				"};") + "\n");
		String fileContent = String.join("\n",
				"namespace Wrapper {",
				"#include \"nested.h\"",
				"void visible() {}",
				"}") + "\n";
		String filePath = tempDir.resolve("sample.cpp").toString();
		UMLModel model = new UMLModel(Set.of());

		new CppFileProcessor(model).processCppFile(filePath, fileContent, false);

		assertFalse(model.getClassList().stream()
				.anyMatch(umlClass -> umlClass.getName().equals("HeaderNested") ||
						umlClass.getName().endsWith(".HeaderNested")));
		assertNotNull(findOperation(findClass(model.getClassList(), "sample").getOperations(), "visible"));
	}

	@Test
	void preservesIncludedAccessLabelForFollowingSourceMember(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("public.inc"), "public:\n");
		String fileContent = String.join("\n",
				"class Container {",
				"private:",
				"#include \"public.inc\"",
				"void visible();",
				"};") + "\n";
		String filePath = tempDir.resolve("sample.cpp").toString();
		UMLModel model = new UMLModel(Set.of());

		new CppFileProcessor(model).processCppFile(filePath, fileContent, false);

		UMLOperation visible = findOperation(findClass(model.getClassList(), "Container").getOperations(), "visible");
		assertEquals(Visibility.PUBLIC, visible.getVisibility());
	}

	private static void assertContainsLocation(List<LocationInfo> locations, int startOffset, int length) {
		assertTrue(locations.stream().anyMatch(location ->
				location.getStartOffset() == startOffset && location.getLength() == length),
				() -> "Expected location [" + startOffset + ", " + (startOffset + length) + ") but found " +
						locations.stream()
								.map(location -> "[" + location.getStartOffset() + ", " + location.getEndOffset() + ")")
								.toList());
	}

	private static UMLOperation findOperation(List<UMLOperation> operations, String name) {
		return operations.stream()
				.filter(operation -> operation.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected operation: " + name));
	}

	@Test
	void namesCallsOfStringLiterals() {
		// Issue #1219: linux asm operands and macros expanding to string literals are calls of a string literal in CDT
		String filePath = "include/asm-x86/processor.c";
		String fileContent = String.join("\n",
				"#define IPW2100_FW_PREFIX \"ipw2100-\" __stringify(1) \".\" __stringify(3)",
				"#define IPW2100_FW_NAME(x) IPW2100_FW_PREFIX \"\" x \".fw\"",
				"static inline void prefetch(const void *x)",
				"{",
				"\talternative_input(\"prefetcht0 (%1)\", \"prefetchnta (%1)\", X86_FEATURE_XMM, \"r\" (x));",
				"}",
				"static int load_firmware(int mode)",
				"{",
				"\tchar *fw_name;",
				"\tfw_name = IPW2100_FW_NAME(\"-i\");",
				"\treturn 0;",
				"}") + "\n";

		UMLClass moduleClass = findClass(processCppModel(filePath, fileContent).getClassList(), "processor");
		List<String> prefetchCalls = findOperation(moduleClass.getOperations(), "prefetch").getAllOperationInvocations().stream().map(AbstractCall::getName).toList();
		assertTrue(prefetchCalls.contains("\"r\""), prefetchCalls.toString());
		List<String> loadFirmwareCalls = findOperation(moduleClass.getOperations(), "load_firmware").getAllOperationInvocations().stream().map(AbstractCall::getName).toList();
		assertTrue(loadFirmwareCalls.contains("IPW2100_FW_NAME(\"-i\")"), loadFirmwareCalls.toString());
	}

	@Test
	void parsesStringifiedMacroArgumentsAsStringLiterals() {
		// Issue #1219: with the linux stub, __stringify gives a string literal, so the firmware name is one assignment without calls
		String filePath = "drivers/net/wireless/ipw2100.c";
		String fileContent = String.join("\n",
				"#include <linux/kernel.h>",
				"#define IPW2100_FW_PREFIX \"ipw2100-\" __stringify(1) \".\" __stringify(3)",
				"#define IPW2100_FW_NAME(x) IPW2100_FW_PREFIX \"\" x \".fw\"",
				"static int load_firmware(int mode)",
				"{",
				"\tchar *fw_name;",
				"\tfw_name = IPW2100_FW_NAME(\"-i\");",
				"\treturn 0;",
				"}") + "\n";

		UMLOperation loadFirmware = findOperation(findClass(processCppModel(filePath, fileContent).getClassList(), "ipw2100").getOperations(), "load_firmware");
		assertEquals(List.of("char *fw_name;", "fw_name = IPW2100_FW_NAME(\"-i\");", "return 0;"),
				loadFirmware.getBody().getCompositeStatement().getLeaves().stream().map(leaf -> leaf.getString().trim()).toList());
		assertTrue(loadFirmware.getAllOperationInvocations().isEmpty());
	}

	@Test
	void keepsCppFunctionVariantsOfNestedConditionalGroups() {
		// Issue #1218: linux kernel/fork.c defines alloc_thread_stack_node in three branches of nested conditional groups
		String filePath = "kernel/fork.c";
		String fileContent = String.join("\n",
				"#ifdef CONFIG_VMAP_STACK",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn 1;",
				"}",
				"#else /* !CONFIG_VMAP_STACK */",
				"#if THREAD_SIZE >= PAGE_SIZE",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn 2;",
				"}",
				"#else /* !(THREAD_SIZE >= PAGE_SIZE) */",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn 3;",
				"}",
				"#endif",
				"#endif /* CONFIG_VMAP_STACK */") + "\n";

		List<UMLOperation> variants = siblingVariants(findClass(processCppModel(filePath, fileContent).getClassList(), "fork").getOperations(), "alloc_thread_stack_node", 3);
		assertEquals(List.of(2, 8, 13), variants.stream().map(operation -> operation.getLocationInfo().getStartLine()).toList());
		assertEquals("#ifdef CONFIG_VMAP_STACK[0]", variants.get(0).getPreprocessorBranch());
		assertEquals("#ifdef CONFIG_VMAP_STACK[1] > #if THREAD_SIZE >= PAGE_SIZE[0]", variants.get(1).getPreprocessorBranch());
		assertEquals("#ifdef CONFIG_VMAP_STACK[1] > #if THREAD_SIZE >= PAGE_SIZE[1]", variants.get(2).getPreprocessorBranch());
	}

	@Test
	void mapsCppFunctionVariantsToTheVariantOfTheSameBranch() throws Exception {
		String filePath = "kernel/fork.c";
		String before = String.join("\n",
				"#ifdef CONFIG_VMAP_STACK",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn allocate(node);",
				"}",
				"#else",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn 0;",
				"}",
				"#endif") + "\n";
		String after = String.join("\n",
				"#ifdef CONFIG_VMAP_STACK",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn allocate(node + 1);",
				"}",
				"#else",
				"static int alloc_thread_stack_node(int node)",
				"{",
				"\treturn 0;",
				"}",
				"#endif") + "\n";

		UMLModelDiff diff = processCppModel(filePath, before).diff(processCppModel(filePath, after));

		assertTrue(diff.getRefactorings().isEmpty());
		UMLClassBaseDiff classDiff = diff.getUMLClassDiff("fork");
		assertTrue(classDiff.getRemovedOperations().isEmpty());
		assertTrue(classDiff.getAddedOperations().isEmpty());
		List<UMLOperationBodyMapper> mappers = classDiff.getOperationBodyMapperList();
		assertEquals(2, mappers.size());
		for(UMLOperationBodyMapper mapper : mappers) {
			assertEquals(mapper.getOperation1().getPreprocessorBranch(), mapper.getOperation2().getPreprocessorBranch());
			assertEquals(mapper.getOperation1().getLocationInfo().getStartLine(), mapper.getOperation2().getLocationInfo().getStartLine());
		}
	}

	@Test
	void endsCppProblemFunctionDefinitionWithItsBodyAcrossConditionalDirectives() {
		// linux kernel/fork.c: CDT splits the definition of an unknown macro at the #ifdef in its body
		String filePath = "kernel/fork.c";
		String fileContent = String.join("\n",
				"SYSCALL_DEFINE0(fork)",
				"{",
				"#ifdef CONFIG_MMU",
				"\treturn kernel_clone(&args);",
				"#else",
				"\treturn -EINVAL;",
				"#endif",
				"}") + "\n";

		List<UMLProblemDeclaration> problems = findClass(processCppModel(filePath, fileContent).getClassList(), "fork").getProblemDeclarations();

		assertEquals(1, problems.size());
		assertEquals(0, problems.get(0).getLocationInfo().getStartOffset());
		assertEquals(fileContent.lastIndexOf('}') + 1, problems.get(0).getLocationInfo().getEndOffset());
	}

	@Test
	void endsCppProblemInitializerWithItsSemicolonAcrossConditionalDirectives() {
		// linux mm/mmap.c: CDT splits a declaration with an initializer list at the #ifdef in it
		String filePath = "mm/mmap.c";
		String fileContent = String.join("\n",
				"static const struct ctl_table mmap_table[] = {",
				"\t{",
				"\t\t.procname = \"max_map_count\",",
				"\t},",
				"#ifdef CONFIG_HAVE_ARCH_MMAP_RND_BITS",
				"\t{",
				"\t\t.procname = \"mmap_rnd_bits\",",
				"\t},",
				"#endif",
				"};") + "\n";

		List<UMLProblemDeclaration> problems = findClass(processCppModel(filePath, fileContent).getClassList(), "mmap").getProblemDeclarations();

		assertTrue(problems.size() <= 1);
		for(UMLProblemDeclaration problem : problems) {
			assertEquals(0, problem.getLocationInfo().getStartOffset());
			assertEquals(fileContent.lastIndexOf(';') + 1, problem.getLocationInfo().getEndOffset());
		}
	}

	//the variants of a function defined in sibling branches of conditional groups, each with its own branch identity
	private static List<UMLOperation> siblingVariants(List<UMLOperation> operations, String name, int count) {
		List<UMLOperation> variants = operations.stream().filter(operation -> operation.getName().equals(name)).toList();
		assertEquals(count, variants.size());
		assertEquals(count, variants.stream().map(UMLOperation::getPreprocessorBranch).filter(Objects::nonNull).distinct().count());
		return variants;
	}

	private static boolean bodyContains(UMLOperation operation, String text) {
		return operation.getBody().getCompositeStatement().getLeaves().stream().anyMatch(leaf -> leaf.getString().contains(text));
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
