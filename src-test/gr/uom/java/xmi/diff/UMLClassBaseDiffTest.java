package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UMLClassBaseDiffTest {
    private static CompilationUnit parse(char[] sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(sourceCode);
        return  (CompilationUnit) parser.createAST(null);
    }
    static boolean isVictorParameterizedTestEnabled() {
        return UMLClassBaseDiff.ENABLE_VICTOR_PARAMETERIZED_TEST_DETECTION;
    }
    static boolean isVictorParameterizedTestDisabled() {
        return !UMLClassBaseDiff.ENABLE_VICTOR_PARAMETERIZED_TEST_DETECTION;
    }
    @EnabledIf("isVictorParameterizedTestDisabled")
    @Nested
    class TestCsvFileSource_OtherPathFormats {
        @TempDir static Path dir;
        private static UMLModel originalModel;

        static boolean isVictorParameterizedTestDisabled() {
            return UMLClassBaseDiffTest.isVictorParameterizedTestDisabled();
        }

        @BeforeAll
        static void setUp() throws RefactoringMinerTimedOutException {
            String originalSourceCode = """
                    public class TestClass {
                        @Test
                        public void testMethod_A() {
                            assertNotEquals("A", null);
                            assertNotEquals("B", null);
                        }
                        @Test
                        public void testMethod_B() {
                            assertNotEquals("C", null);
                            assertNotEquals("D", null);
                        }
                    }
                """;
            Path csvPath = dir.resolve("src/test/resources/file.csv");
            Path testPath = Paths.get("").toAbsolutePath().relativize(dir.resolve("src/test/java/TestClass.java"));
            try {
                Files.createDirectories(testPath.getParent());
                Files.createDirectories(csvPath.getParent());
                FileWriter fileWriter = new FileWriter(csvPath.toFile());
                fileWriter.write("A,B\nC,D");
                fileWriter.close();
            } catch (IOException e) {
                fail("Could not write to file");
            }
//            originalModel = createUmlModel(originalSourceCode,testPath.toString());
//            throw new RuntimeException(testPath.toString());
            originalModel = createUmlModel(originalSourceCode);
        }

        @Test
        void testTestFileRelativePath() throws RefactoringMinerTimedOutException {
            Path csvPath = dir.resolve("src/test/java/TestClass.java").toAbsolutePath().relativize(dir.resolve("src/test/resources/file.csv"));
            assertFalse(csvPath.isAbsolute());
            String newSourceCode = """
                public class TestClass {
                    @ParameterizedTest
                    @CsvFileSource(files = \"""" + csvPath.toString() + """
                    \")
                    public void testMethod(String param1, String param2) {
                        assertNotEquals(param1, null);
                        assertNotEquals(param2, null);
                    }
                }
                """;
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testRelativePath() throws RefactoringMinerTimedOutException {
            Path csvPath = Paths.get("").toAbsolutePath().relativize(dir.resolve("src/test/resources/file.csv"));
            assertFalse(csvPath.isAbsolute());
            String newSourceCode = """
                public class TestClass {
                    @ParameterizedTest
                    @CsvFileSource(files = \"""" + csvPath.toString() + """
                    \")
                    public void testMethod(String param1, String param2) {
                        assertNotEquals(param1, null);
                        assertNotEquals(param2, null);
                    }
                }
                """;
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testProjectRelativePath() throws RefactoringMinerTimedOutException {
            String newSourceCode = """
                public class TestClass {
                    @ParameterizedTest
                    @CsvFileSource(files = \"src/test/resources/file.csv\")
                    public void testMethod(String param1, String param2) {
                        assertNotEquals(param1, null);
                        assertNotEquals(param2, null);
                    }
                }
                """;
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testProjectRelativeResource() throws RefactoringMinerTimedOutException {
            String newSourceCode = """
                public class TestClass {
                    @ParameterizedTest
                    @CsvFileSource(resources = \"file.csv\")
                    public void testMethod(String param1, String param2) {
                        assertNotEquals(param1, null);
                        assertNotEquals(param2, null);
                    }
                }
                """;
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

    }
    @EnabledIf("isVictorParameterizedTestDisabled")
    @Nested
    class TestCsvFileSource_AbsolutePath {
        @TempDir static Path dir;
        private static UMLModelDiff diff;
        static boolean isVictorParameterizedTestDisabled() {
            return UMLClassBaseDiffTest.isVictorParameterizedTestDisabled();
        }

        @BeforeAll
        static void setUp() throws RefactoringMinerTimedOutException {
            String originalSourceCode = """
                    public class TestClass {
                        @Test
                        public void testMethod_A() {
                            assertNotEquals("A", null);
                            assertNotEquals("B", null);
                        }
                        @Test
                        public void testMethod_B() {
                            assertNotEquals("C", null);
                            assertNotEquals("D", null);
                        }
                    }
                """;
            Path csvPath = dir.resolve("file.csv");
            String newSourceCode = """
                public class TestClass {
                    @ParameterizedTest
                    @CsvFileSource(files = \"""" + csvPath.toString() + """
                    \")
                    public void testMethod(String param1, String param2) {
                        assertNotEquals(param1, null);
                        assertNotEquals(param2, null);
                    }
                }
                """;
            try {
                FileWriter fileWriter = new FileWriter(csvPath.toFile());
                fileWriter.write("A,B\nC,D");
                fileWriter.close();
            } catch (IOException e) {
                fail("Could not write to file");
            }
            UMLModel originalModel = createUmlModel(originalSourceCode);
            UMLModel newModel = createUmlModel(newSourceCode);
            diff = originalModel.diff(newModel);
        }

        @Test
        void testDetectedRefactorings() throws RefactoringMinerTimedOutException {
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @ParameterizedTest
        @CsvSource({"0,public testMethod_A() : void",
                    "1,public testMethod_B() : void"})
        void testCodeRange(int refactoringIdx, String codeElementBefore) throws RefactoringMinerTimedOutException {
            ParameterizeTestRefactoring refactoring = (ParameterizeTestRefactoring) diff.getRefactorings().get(refactoringIdx);
            List<CodeRange> leftSide = refactoring.leftSide();
            assertEquals(1, leftSide.size());
            assertEquals(LocationInfo.CodeElementType.METHOD_DECLARATION, leftSide.get(0).getCodeElementType());
            assertEquals(codeElementBefore, leftSide.get(0).getCodeElement());
            List<CodeRange> rightSide = refactoring.rightSide();
            assertEquals(1, rightSide.size());
            assertEquals(LocationInfo.CodeElementType.METHOD_DECLARATION, rightSide.get(0).getCodeElementType());
            assertEquals("public testMethod(param1 String, param2 String) : void", rightSide.get(0).getCodeElement());
        }
        @ParameterizedTest
        @ValueSource(ints = {0, 1})
        void testBodyMapper(int refactoringIdx) throws RefactoringMinerTimedOutException {
            ParameterizeTestRefactoring refactoring = (ParameterizeTestRefactoring) diff.getRefactorings().get(refactoringIdx);
            UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
            assertTrue(mapper.getOperationSignatureDiff().isPresent());
            assertEquals(0, mapper.getOperationSignatureDiff().get().getRemovedParameters().size());
            assertEquals(2, mapper.getOperationSignatureDiff().get().getAddedParameters().size());
            assertEquals(2, mapper.getOperationSignatureDiff().get().getAnnotationListDiff().getAddedAnnotations().size());
            assertEquals(1, mapper.getOperationSignatureDiff().get().getAnnotationListDiff().getRemovedAnnotations().size());
        }
        @ParameterizedTest
        @CsvSource({"0,\"A\",\"B\"",
                    "1,\"C\",\"D\""})
        void testReplacements(int refactoringIdx, String param1, String param2) throws RefactoringMinerTimedOutException {
            ParameterizeTestRefactoring refactoring = (ParameterizeTestRefactoring) diff.getRefactorings().get(refactoringIdx);
            UMLOperationBodyMapper mapper = refactoring.getBodyMapper();
            Object[] replacements = mapper.getReplacements().toArray();
            assertEquals(((Replacement) replacements[0]).getType(), Replacement.ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
            assertEquals(param1, ((Replacement) replacements[0]).getBefore());
            List<UMLParameter> addedParameters = mapper.getOperationSignatureDiff().get().getAddedParameters();
            assertEquals(((Replacement) replacements[0]).getAfter(), addedParameters.get(0).getName());
            assertEquals(((Replacement) replacements[1]).getType(), Replacement.ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
            assertEquals(param2, ((Replacement) replacements[1]).getBefore());
            assertEquals(((Replacement) replacements[1]).getAfter(), addedParameters.get(1).getName());
        }

    }

    @EnabledIf("isVictorParameterizedTestDisabled")
    @ParameterizedTest
    @ValueSource(strings = {"""
            public class TestClass {
                @ParameterizedTest
                @ValueSource(strings = {"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource({"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource({"value",
                "value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource(value={"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource(value = {"value",
                "value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            """})
    void testCheckForTestParameterizations_OneStringParam_Plugin(String newSourceCode) throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                    public class TestClass {
                        @Test
                        public void testMethod() {
                            assertEquals(null, null);
                        }
                    }
                """;
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(newSourceCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        UMLClassBaseDiff umlClassBaseDiff = diff.getUMLClassDiff("TestClass");
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(4, refactorings.size());
        assertArrayEquals(new String[]{"Add Parameter","Add Method Annotation","Add Method Annotation","Remove Method Annotation"}, refactorings.stream().map(Refactoring::getName).toArray());
        AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) refactorings.stream().filter(r -> r.getName().equals("Add Method Annotation") && ((AddMethodAnnotationRefactoring) r).getAnnotation().getTypeName().endsWith("Source")).findFirst().get();
        if (addMethodAnnotationRefactoring.getAnnotation().isNormalAnnotation()) {
            assertEquals("{\"value\",\"value2\"}", addMethodAnnotationRefactoring.getAnnotation().getMemberValuePairs().values().stream().findAny().get().getExpression(), "Normal annotation has incorrect value");
        } else if (addMethodAnnotationRefactoring.getAnnotation().isSingleMemberAnnotation()) {
            assertEquals("{\"value\",\"value2\"}", addMethodAnnotationRefactoring.getAnnotation().getValue().getExpression(), "Member value pair annotation has incorrect value");
        } else {
            fail("Annotation is not normal or single member");
        }
    }
    @EnabledIf("isVictorParameterizedTestEnabled")
    @ParameterizedTest
    @ValueSource(strings = {"""
            public class TestClass {
                @ParameterizedTest
                @ValueSource(strings = {"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource({"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource({"value",
                "value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource(value={"value","value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            ""","""
            public class TestClass {
                @ParameterizedTest
                @CsvSource(value = {"value",
                "value2"})
                public void testMethod(String parameter) {
                    assertEquals(parameter, null);
                }
            }
            """})
    void testCheckForTestParameterizations_OneStringParam(String newSourceCode) throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                    public class TestClass {
                        @Test
                        public void testMethod() {
                            assertEquals(null, null);
                        }
                    }
                """;
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(newSourceCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        UMLClassBaseDiff umlClassBaseDiff = diff.getUMLClassDiff("TestClass");
        assertEquals("{\"value\",\"value2\"}", umlClassBaseDiff.addedParameter.getExpression());
        assertEquals(Set.of("value","value2"), umlClassBaseDiff.newValue);
    }

    private static Stream<Arguments> multipleParamsDataProvider() {
        return Stream.of(Arguments.of("CsvSource", "({\"value,value2\"})", "String a, String b", Set.of("value","value2"), "{\"value,value2\"}", "@ParameterizedTest"),
                Arguments.of("Test", "(dataProvider=getParameters)", "String a, String b", Set.of("getParameters"), "getParameters", ""));
    }

    @EnabledIf("isVictorParameterizedTestEnabled")
    @ParameterizedTest
    @MethodSource("multipleParamsDataProvider")
    void testCheckForTestParameterizations_MultipleParams(String annotation, String sourceParameters, String parameters, Set newValues, String addedParameterExpression, String parameterizedAnnotation) throws RefactoringMinerTimedOutException {
        String baseSourceCode = """
                    public class TestClass {
                        %s
                        @%s%s
                        public void testMethod(%s) {
                            assertEquals(null, null);
                        }
                    }
                """;
        String originalSourceCode = String.format(baseSourceCode, "", "Test", "", "");
        String newSourceCode = String.format(baseSourceCode, parameterizedAnnotation, annotation, sourceParameters, parameters);
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(newSourceCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        UMLClassBaseDiff umlClassBaseDiff = diff.getUMLClassDiff("TestClass");
        assertEquals(addedParameterExpression, umlClassBaseDiff.addedParameter.getExpression());
        assertEquals(newValues.size(), umlClassBaseDiff.newValue.size());
        umlClassBaseDiff.newValue.removeAll(newValues);
        assertIterableEquals(Collections.emptySet(), umlClassBaseDiff.newValue);
    }

    private static UMLModel createUmlModel(String sourceCode) {
        return createUmlModel(sourceCode, "TestClass.java");
    }

    private static UMLModel createUmlModel(String sourceCode, String filePath) {
        CompilationUnit cu = parse(sourceCode.toCharArray());
        Map<String, String> javaFileContents = Map.of(filePath, sourceCode);
        UMLModel model = new UMLModelASTReader(javaFileContents, Set.of("."), false).getUmlModel();
        ASTNode node = (ASTNode) cu;
        assertNotNull(node);
        LocationInfo location = new LocationInfo(cu, filePath, node, LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass aClass = new UMLClass("org.refactoringminer.test", "TestClass", location, true, Collections.emptyList());
        aClass.setVisibility(Visibility.PUBLIC);
        return model;
    }
}