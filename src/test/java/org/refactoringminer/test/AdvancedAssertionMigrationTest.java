package org.refactoringminer.test;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class AdvancedAssertionMigrationTest {
	private static final Path ORACLE_COMMITS = Path.of(System.getProperty("user.dir"), "src", "test", "resources", "oracle", "commits");
	private static final String MOCKITO_BEFORE_COMMIT = "mockito-4c397ef3c50705522bd637141060faf24b666e9c";
	private static final String MOCKITO_AFTER_COMMIT = "mockito-6b818baa1006338501a5fde5ba368593475cee9c";
	private static final String FLINK_RETRY_BEFORE_COMMIT = "flink-acd3e7ab66c07f2fca7ea3647aeb9eda25ea69ab";
	private static final String FLINK_RETRY_AFTER_COMMIT = "flink-2c5bc580e6c10fb3a2724a945847b5cc6b28df27";
	private static final String FLINK_RETRY_SECOND_BEFORE_COMMIT = "flink-7eaa5db30244b8b5d9cdc6ab0cb327e255d6fadc";
	private static final String FLINK_RETRY_SECOND_AFTER_COMMIT = "flink-95c3499ea80d07c448c297e36fa5a1b5b4caea2b";
	private static final String FLINK_BEFORE_COMMIT = "flink-875a431950da066aa696f2c3a335f3267b85194a";
	private static final String FLINK_AFTER_COMMIT = "flink-62f44e0118539c1ed0dedf47099326f97c9d0427";
	private static final String SPRING_BEFORE_COMMIT = "spring-framework-3d85ec23545f455d6d055c2e786c2ab0b4850f5f";
	private static final String SPRING_AFTER_COMMIT = "spring-framework-bed36890019a1764f1ed846ec1c94325a2ac5c72";

	private static final String ASSERTJ_TYPE_NARROWING_BEFORE =
			"package p;\n" +
			"\n" +
			"import static org.assertj.core.api.Assertions.assertThat;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        Object result = names();\n" +
			"        String[] expected = new String[]{\"a\", \"b\"};\n" +
			"        boolean condition = result instanceof String[];\n" +
			"        assertThat(condition).isTrue();\n" +
			"        assertThat((String[]) result).isEqualTo(expected);\n" +
			"    }\n" +
			"\n" +
			"    Object names() {\n" +
			"        return new String[]{\"a\", \"b\"};\n" +
			"    }\n" +
			"}\n";

	private static final String ASSERTJ_TYPE_NARROWING_AFTER =
			"package p;\n" +
			"\n" +
			"import static org.assertj.core.api.Assertions.assertThat;\n" +
			"import static org.assertj.core.api.InstanceOfAssertFactories.array;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        Object result = names();\n" +
			"        String[] expected = new String[]{\"a\", \"b\"};\n" +
			"        assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);\n" +
			"    }\n" +
			"\n" +
			"    Object names() {\n" +
			"        return new String[]{\"a\", \"b\"};\n" +
			"    }\n" +
			"}\n";

	private static final String EXPECTED_EXCEPTION_RULE_BEFORE =
			"package p;\n" +
			"\n" +
			"import org.junit.Rule;\n" +
			"import org.junit.Test;\n" +
			"import org.junit.rules.ExpectedException;\n" +
			"\n" +
			"class A {\n" +
			"    @Rule public ExpectedException thrown = ExpectedException.none();\n" +
			"\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        thrown.expect(IllegalArgumentException.class);\n" +
			"        thrown.expectMessage(\"bad\");\n" +
			"        boom();\n" +
			"    }\n" +
			"\n" +
			"    void boom() {\n" +
			"        throw new IllegalArgumentException(\"bad\");\n" +
			"    }\n" +
			"}\n";

	private static final String EXPECTED_EXCEPTION_RULE_AFTER =
			"package p;\n" +
			"\n" +
			"import static org.assertj.core.api.Assertions.assertThatThrownBy;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        assertThatThrownBy(() -> boom())\n" +
			"                .isInstanceOf(IllegalArgumentException.class)\n" +
			"                .hasMessageContaining(\"bad\");\n" +
			"    }\n" +
			"\n" +
			"    void boom() {\n" +
			"        throw new IllegalArgumentException(\"bad\");\n" +
			"    }\n" +
			"}\n";

	private static final String ASSERTJ_TYPE_NARROWING_NEGATIVE_AFTER =
			"package p;\n" +
			"\n" +
			"import static org.assertj.core.api.Assertions.assertThat;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        Object result = names();\n" +
			"        String[] expected = new String[]{\"a\", \"b\"};\n" +
			"        assertThat(result).isEqualTo(expected);\n" +
			"    }\n" +
			"\n" +
			"    Object names() {\n" +
			"        return new String[]{\"a\", \"b\"};\n" +
			"    }\n" +
			"}\n";

	private static final String ASSERT_THAT_THROWN_BY_NEGATIVE_BEFORE =
			"package p;\n" +
			"\n" +
			"import static org.junit.Assert.assertThrows;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        Throwable t = assertThrows(IllegalArgumentException.class, this::boom);\n" +
			"        ExceptionUtils.assertThrowable(t, IllegalStateException.class);\n" +
			"    }\n" +
			"\n" +
			"    void boom() {\n" +
			"        throw new IllegalArgumentException();\n" +
			"    }\n" +
			"}\n" +
			"\n" +
			"class ExceptionUtils {\n" +
			"    static void assertThrowable(Throwable t, Class<?> type) {\n" +
			"    }\n" +
			"}\n";

	private static final String ASSERT_THAT_THROWN_BY_NEGATIVE_AFTER =
			"package p;\n" +
			"\n" +
			"import static org.assertj.core.api.Assertions.assertThatThrownBy;\n" +
			"import org.junit.Test;\n" +
			"\n" +
			"class A {\n" +
			"    @Test\n" +
			"    public void m() {\n" +
			"        assertThatThrownBy(this::boom).isInstanceOf(IllegalArgumentException.class);\n" +
			"    }\n" +
			"\n" +
			"    void boom() {\n" +
			"        throw new IllegalArgumentException();\n" +
			"    }\n" +
			"}\n" +
			"\n" +
			"class ExceptionUtils {\n" +
			"    static void assertThrowable(Throwable t, Class<?> type) {\n" +
			"    }\n" +
			"}\n";

	@Test
	public void testAssertJTypeNarrowingKeepsCollapsedAssertionsRelated() throws RefactoringMinerTimedOutException {
		UMLModelDiff modelDiff = buildModelDiff(ASSERTJ_TYPE_NARROWING_BEFORE, ASSERTJ_TYPE_NARROWING_AFTER);
		UMLOperationBodyMapper mapper = mapperForMethod(modelDiff, "p.A", "m");

		Assertions.assertTrue(hasMapping(mapper, "assertThat((String[])result).isEqualTo(expected);",
				"assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);"));
		Assertions.assertTrue(hasLeafMapping(mapper, "result instanceof String[]",
				"assertThat(result).asInstanceOf(array(String[].class))"));
		Assertions.assertEquals(0, mapper.getNonMappedLeavesT2().size());
	}

	@Test
	public void testExpectedExceptionRuleToAssertThatThrownByKeepsTypeAndMessageMappings() throws RefactoringMinerTimedOutException {
		UMLModelDiff modelDiff = buildModelDiff(EXPECTED_EXCEPTION_RULE_BEFORE, EXPECTED_EXCEPTION_RULE_AFTER);
		AssertThrowsRefactoring refactoring = modelDiff.getRefactorings().stream()
				.filter(AssertThrowsRefactoring.class::isInstance)
				.map(AssertThrowsRefactoring.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("AssertThrowsRefactoring not detected"));

		Assertions.assertTrue(hasLeafMapping(refactoring.getSubExpressionMappings(),
				"IllegalArgumentException.class", "IllegalArgumentException.class"));
		Assertions.assertTrue(hasLeafMapping(refactoring.getSubExpressionMappings(), "\"bad\"", "\"bad\""));
	}

	@Test
	public void testAssertJTypeNarrowingDoesNotOvermatchWithoutAsInstanceOf() throws RefactoringMinerTimedOutException {
		UMLModelDiff modelDiff = buildModelDiff(ASSERTJ_TYPE_NARROWING_BEFORE, ASSERTJ_TYPE_NARROWING_NEGATIVE_AFTER);
		modelDiff.getRefactorings();
		UMLOperationBodyMapper mapper = mapperForMethod(modelDiff, "p.A", "m");

		Assertions.assertTrue(containsUnmappedLeaf(mapper, "result instanceof String[]"));
		Assertions.assertTrue(containsUnmappedLeaf(mapper, "assertThat(condition).isTrue();"));
	}

	@Test
	public void testAssertThrowableDoesNotOvermatchWhenExceptionTypeDiffers() throws RefactoringMinerTimedOutException {
		UMLModelDiff modelDiff = buildModelDiff(ASSERT_THAT_THROWN_BY_NEGATIVE_BEFORE, ASSERT_THAT_THROWN_BY_NEGATIVE_AFTER);
		modelDiff.getRefactorings();
		UMLOperationBodyMapper mapper = mapperForMethod(modelDiff, "p.A", "m");

		Assertions.assertTrue(containsUnmappedLeaf(mapper, "ExceptionUtils.assertThrowable(t, IllegalStateException.class);"));
	}

	@Test
	public void testSpringIssueCommitAdvancedAssertJMappingsAreFullyCovered() throws Exception {
		UMLModelDiff modelDiff = buildSpringIssueModelDiff();
		assertAdvancedMappersFullyCovered(modelDiff, this::containsAdvancedAssertJTypeNarrowing,
				"Spring fixture to contain advanced AssertJ narrowing mappers");
	}

	@Test
	public void testSpringIssueCommitDetectsConditionInlineVariables() throws Exception {
		UMLModelDiff modelDiff = buildSpringIssueModelDiff();
		Set<String> descriptions = new TreeSet<>();
		modelDiff.getRefactorings().forEach(refactoring -> descriptions.add(normalizeForComparison(refactoring.toString())));

		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveMultipartFileArray() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolvePartArray() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveStringArrayWithEmptyArraySuffix() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveStringArray() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveStringArrayWithEmptyArraySuffix() : void from class " +
						"org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertTrue(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveStringArray() : void from class " +
						"org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveMultipartFile() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveMultipartFileList() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveMultipartFileListNotannot() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolveMultipartFileNotAnnot() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolvePart() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolvePartList() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
		Assertions.assertFalse(descriptions.contains(normalizeForComparison(
				"Inline Variable condition : boolean in method package resolvePartNotAnnot() : void from class " +
						"org.springframework.web.method.annotation.RequestParamMethodArgumentResolverTests")));
	}

	@Test
	public void testMockitoIssueCommitAssertThatThrownByMappingsAreFullyCovered() throws Exception {
		UMLModelDiff modelDiff = buildChangedFixtureModelDiff(MOCKITO_BEFORE_COMMIT, MOCKITO_AFTER_COMMIT);
		assertAdvancedMappersFullyCovered(modelDiff, this::containsAssertThatThrownBy,
				"Mockito fixture to contain assertThatThrownBy mappers");
	}

	@Test
	public void testFlinkIssueCommitAssertThatThrownByMappingsAreFullyCovered() throws Exception {
		UMLModelDiff modelDiff = buildChangedFixtureModelDiff(FLINK_BEFORE_COMMIT, FLINK_AFTER_COMMIT);
		assertAdvancedMappersFullyCovered(modelDiff, this::containsAssertThatThrownBy,
				"Flink fixture to contain assertThatThrownBy mappers");
	}

	@Test
	public void testFlinkRetryFixtureMappingsAreFullyCovered() throws Exception {
		UMLModelDiff modelDiff = buildChangedFixtureModelDiff(FLINK_RETRY_BEFORE_COMMIT, FLINK_RETRY_AFTER_COMMIT);
		assertAssertThrowsRefactoringDetected(modelDiff, "IllegalStateException.class");
	}

	@Test
	public void testSecondFlinkRetryFixtureMappingsAreFullyCovered() throws Exception {
		UMLModelDiff modelDiff = buildChangedFixtureModelDiff(FLINK_RETRY_SECOND_BEFORE_COMMIT, FLINK_RETRY_SECOND_AFTER_COMMIT);
		assertAssertThrowsRefactoringDetected(modelDiff, "IllegalStateException.class");
	}

	private UMLModelDiff buildModelDiff(String beforeCode, String afterCode) throws RefactoringMinerTimedOutException {
		UMLModel before = new UMLModelASTReader(Map.of("A.java", beforeCode), Set.of(), false).getUmlModel();
		UMLModel after = new UMLModelASTReader(Map.of("A.java", afterCode), Set.of(), false).getUmlModel();
		return before.diff(after);
	}

	private UMLModelDiff buildSpringIssueModelDiff() throws Exception {
		UMLModel before = new UMLModelASTReader(readFixtureFiles(SPRING_BEFORE_COMMIT), Set.of(), false).getUmlModel();
		UMLModel after = new UMLModelASTReader(readFixtureFiles(SPRING_AFTER_COMMIT), Set.of(), false).getUmlModel();
		return before.diff(after);
	}

	private UMLModelDiff buildChangedFixtureModelDiff(String beforeCommit, String afterCommit) throws Exception {
		FixtureFilePair files = readChangedFixtureFiles(beforeCommit, afterCommit);
		UMLModel before = new UMLModelASTReader(files.before(), Set.of(), false).getUmlModel();
		UMLModel after = new UMLModelASTReader(files.after(), Set.of(), false).getUmlModel();
		return before.diff(after);
	}

	private Map<String, String> readFixtureFiles(String commit) throws IOException {
		Path root = ORACLE_COMMITS.resolve(commit);
		Map<String, String> files = new LinkedHashMap<>();
		try(var paths = Files.walk(root)) {
			paths.filter(Files::isRegularFile).forEach(path -> {
				try {
					files.put(root.relativize(path).toString().replace('\\', '/'), Files.readString(path));
				}
				catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
		return files;
	}

	private FixtureFilePair readChangedFixtureFiles(String beforeCommit, String afterCommit) throws IOException {
		Map<String, String> beforeFiles = readFixtureFiles(beforeCommit);
		Map<String, String> afterFiles = readFixtureFiles(afterCommit);
		Map<String, String> changedBeforeFiles = new LinkedHashMap<>();
		Map<String, String> changedAfterFiles = new LinkedHashMap<>();
		Set<String> allPaths = new TreeSet<>();
		allPaths.addAll(beforeFiles.keySet());
		allPaths.addAll(afterFiles.keySet());
		for(String path : allPaths) {
			String before = beforeFiles.get(path);
			String after = afterFiles.get(path);
			if(!Objects.equals(before, after)) {
				if(before != null) {
					changedBeforeFiles.put(path, before);
				}
				if(after != null) {
					changedAfterFiles.put(path, after);
				}
			}
		}
		return new FixtureFilePair(changedBeforeFiles, changedAfterFiles);
	}

	private void assertAdvancedMappersFullyCovered(UMLModelDiff modelDiff,
			java.util.function.Predicate<UMLOperationBodyMapper> filter, String emptyMessage) throws RefactoringMinerTimedOutException {
		modelDiff.getRefactorings();
		List<UMLOperationBodyMapper> matchingMappers = new ArrayList<>();
		for(UMLClassBaseDiff classDiff : modelDiff.getCommonClassDiffList()) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1() != null && mapper.getContainer2() != null &&
						mapper.getContainer1().getName().equals(mapper.getContainer2().getName()) &&
						filter.test(mapper)) {
					matchingMappers.add(mapper);
				}
			}
		}

		Assertions.assertFalse(matchingMappers.isEmpty(), emptyMessage);
		for(UMLOperationBodyMapper mapper : matchingMappers) {
			Assertions.assertEquals(0, mapper.getNonMappedLeavesT1().size(),
					() -> mapper.getContainer1().getClassName() + "#" + mapper.getContainer1().getName() +
							" still has unmatched old statements: " + mapper.getNonMappedLeavesT1());
			Assertions.assertEquals(0, mapper.getNonMappedLeavesT2().size(),
					() -> mapper.getContainer2().getClassName() + "#" + mapper.getContainer2().getName() +
							" still has unmatched new statements: " + mapper.getNonMappedLeavesT2());
		}
	}

	private void assertAssertThrowsRefactoringDetected(UMLModelDiff modelDiff, String typeLiteral) throws RefactoringMinerTimedOutException {
		AssertThrowsRefactoring refactoring = modelDiff.getRefactorings().stream()
				.filter(AssertThrowsRefactoring.class::isInstance)
				.map(AssertThrowsRefactoring.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("AssertThrowsRefactoring not detected"));
		Assertions.assertTrue(hasLeafMapping(refactoring.getSubExpressionMappings(), typeLiteral, typeLiteral),
				() -> "Expected sub-expression mapping for " + typeLiteral);
	}

	private boolean containsAdvancedAssertJTypeNarrowing(UMLOperationBodyMapper mapper) {
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.getFragment2().getString().contains(".asInstanceOf(")) {
				return true;
			}
		}
		return false;
	}

	private boolean containsAssertThatThrownBy(UMLOperationBodyMapper mapper) {
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.getFragment2().getString().contains("assertThatThrownBy(")) {
				return true;
			}
		}
		return false;
	}

	private UMLOperationBodyMapper mapperForMethod(UMLModelDiff modelDiff, String className, String methodName) {
		UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff(className);
		Assertions.assertNotNull(classDiff);
		return classDiff.getOperationBodyMapperList().stream()
				.filter(mapper -> mapper.getOperation1().getName().equals(methodName))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Mapper not found for " + methodName));
	}

	private boolean hasMapping(UMLOperationBodyMapper mapper, String before, String after) {
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.getFragment1().getString().strip().equals(before) &&
					mapping.getFragment2().getString().strip().equals(after)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasLeafMapping(Iterable<LeafMapping> mappings, String before, String after) {
		for(LeafMapping mapping : mappings) {
			if(mapping.getFragment1().getString().strip().equals(before) &&
					mapping.getFragment2().getString().strip().equals(after)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasLeafMapping(UMLOperationBodyMapper mapper, String before, String after) {
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping instanceof LeafMapping &&
					mapping.getFragment1().getString().strip().equals(before) &&
					mapping.getFragment2().getString().strip().equals(after)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsUnmappedLeaf(UMLOperationBodyMapper mapper, String text) {
		String normalizedText = normalizeForComparison(text);
		return mapper.getNonMappedLeavesT1().stream()
				.anyMatch(fragment -> normalizeForComparison(fragment.getString()).contains(normalizedText));
	}

	private String normalizeForComparison(String text) {
		return text.replace(" ", "").replace("\n", "").replace("\t", "");
	}

	private record FixtureFilePair(Map<String, String> before, Map<String, String> after) {
	}
}
