package org.refactoringminer.test;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestParameterizeTestRefactoring {
    private static CompilationUnit parse(char[] sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(sourceCode);
        return  (CompilationUnit) parser.createAST(null);
    }
    @ParameterizedTest
    @ValueSource(strings = {"""
                public class TestClass {
                    @Test
                    void testTestFileRelativePath() {
                        assertNotNull("");
                        assertTrue("".length() >= 0);
                        assertTrue("".length() < 2);
                    }
                    @Test
                    void testTestFileRelativePath_1() {
                        assertNotNull("1");
                        assertTrue("1".length() >= 0);
                        assertTrue("1".length() < 2);
                    }
                    @Test
                    void testTestFileRelativePath_2() {
                        assertNotNull("2");
                        assertTrue("2".length() >= 0);
                        assertTrue("2".length() < 2);
                    }
                    @Test
                    void testTestFileRelativePath_3() {
                        assertNotNull("3");
                        assertTrue("3".length() >= 0);
                        assertTrue("3".length() < 2);
                    }
                    @Test
                    void testTestFileRelativePath_4() {
                        assertNotNull("4");
                        assertTrue("4".length() >= 0);
                        assertTrue("4".length() < 2);
                    }
                    @Test
                    void testTestFileRelativePath_5() {
                        assertNotNull("5");
                        assertTrue("5".length() >= 0);
                        assertTrue("5".length() < 2);
                    }
                }
                """,
                """
                    public class TestClass {
                        @Test
                        void testTestFileRelativePath() {
                            String s = "";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                        @Test
                        void testTestFileRelativePath_1() {
                            String s = "1";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                        @Test
                        void testTestFileRelativePath_2() {
                            String s = "2";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                        @Test
                        void testTestFileRelativePath_3() {
                            String s = "3";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                        @Test
                        void testTestFileRelativePath_4() {
                            String s = "4";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                        @Test
                        void testTestFileRelativePath_5() {
                            String s = "5";
                            assertNotNull(s);
                            assertTrue(s.length() >= 0);
                            assertTrue(s.length() < 2);
                        }
                    }
                """})
    void testParameterizedTestWithMultipleSources(String originalSourceCode) throws RefactoringMinerTimedOutException {
        String parameterizedTestCode = """
                public class TestClass {
                    @ParameterizedTest
                    @EmptySource
                    @ValueSource(strings={"1","2","3","4","5"})
                    void testTestFileRelativePath(String s) {
                        assertNotNull(s);
                        assertTrue(s.length() >= 0);
                        assertTrue(s.length() < 2);
                    }
                }
                """;
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(6, refactorings.size());
        assertTrue(refactorings.stream().allMatch(r -> r.getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST)), refactorings.stream().filter(r -> !r.getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST)).map(Refactoring::getName).collect(Collectors.joining()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "(TestEnum.class)"})
    void testEnumSource(String sourceParameter) throws RefactoringMinerTimedOutException {
        Set<String> dirs = Set.of("src/", "src/main/", "src/main/java", "src/main/java/test");
        Map<String, String> files = Map.of(
                "src/main/java/com/test/TestEnum.java", """
                        package com.test;
                        public enum TestEnum {
                            TEST1, TEST2, TEST3, TEST4, TEST5;
                            int number() {
                                return Integer.parseInt(name().substring(name().length() - 1));
                            }
                        }
                        """,
                "src/test/java/com/test/TestClass.java", """
                        package com.test;
                        import org.junit.jupiter.params.ParameterizedTest;
                        import org.junit.jupiter.params.provider.EnumSource;
                        import static org.junit.jupiter.api.Assertions.assertTrue;
                        public class TestClass {
                            @ParameterizedTest
                            @EnumSource%s
                            void testEnum(TestEnum te) {
                                assertTrue(te.number() >= 1 && te.number() <= 5);
                            }
                        }
                """.formatted(sourceParameter));
        HashMap<String, String> oldFiles = new HashMap<>(files);
        oldFiles.replace("src/test/java/com/test/TestClass.java", """
                        package com.test;
                        import org.junit.jupiter.api.Test;
                        import static org.junit.jupiter.api.Assertions.assertTrue;
                        public class TestClass {
                            @Test
                            void testEnum_1() {
                                assertTrue(TestEnum.TEST1.number() >= 1 && TestEnum.TEST1.number() <= 5);
                            }
                            @Test
                            void testEnum_2() {
                                assertTrue(TestEnum.TEST2.number() >= 1 && TestEnum.TEST2.number() <= 5);
                            }
                            @Test
                            void testEnum_3() {
                                assertTrue(TestEnum.TEST3.number() >= 1 && TestEnum.TEST3.number() <= 5);
                            }
                            @Test
                            void testEnum_4() {
                                assertTrue(TestEnum.TEST4.number() >= 3 && TestEnum.TEST4.number() <= 5);
                            }
                            @Test
                            void testEnum_5() {
                                assertTrue(TestEnum.TEST5.number() >= 3 && TestEnum.TEST5.number() <= 5);
                            }
                        }
                """);
        UMLModel newModel = new UMLModelASTReader(files, dirs, false).getUmlModel();
        UMLModel originalModel = new UMLModelASTReader(oldFiles, dirs, false).getUmlModel();
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(5, refactorings.size());
    }

    @Test
    void testNullSrc() throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                public class TestClass {
                    enum TestEnum {
                         TEST1, TEST2, TEST3, TEST4, TEST5;
                         int number() {
                            if (name() == "TEST1") {
                                throw new NullPointerException();
                            }
                            return Integer.parseInt(name().substring(name().length() - 1));
                         }
                    }
                    @Test
                    void testNullEnum_TEST1() {
                        assertThrows(NullPointerException.class, () -> TestEnum.TEST1.number());
                    }
                    @Test
                    void testNullEnum_null() {
                        TestEnum te = null;
                        assertThrows(NullPointerException.class, () -> te.number());
                    }
                    @Test
                    void testNullEnum_nullLiteral() {
                        assertThrows(NullPointerException.class, () -> null.number());
                    }
                }
                """;
        assertDoesNotThrow(() -> createUmlModel(originalSourceCode));
        String parameterizedTestCode = """
                    import org.junit.jupiter.params.ParameterizedTest;
                    public class TestClass {
                        enum TestEnum {
                             TEST1, TEST2, TEST3, TEST4, TEST5;
                             int number() {
                                if (name() == "TEST1") {
                                    throw new NullPointerException();
                                }
                                return Integer.parseInt(name().substring(name().length() - 1));
                             }
                        }
                        @ParameterizedTest
                        @NullSource
                        @EnumSource(value = TestEnum.class, names = {"TEST1"})
                        void testNullEnum(TestEnum te) {
                            assertThrows(NullPointerException.class, () -> te.number());
                        }
                    }""";
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(3, refactorings.size());
        assertEquals(3, refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST)).count());
    }

    @Disabled("TODO: Add support for Modify Method Annotation with additional parameter")
    @Test
    void testEnumParameterMerged() throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                public class TestClass {
                    enum TestEnum {
                         TEST1, TEST2, TEST3, TEST4, TEST5;
                         int number() {
                            if (name() == "TEST1") {
                                throw new NullPointerException();
                            }
                            return Integer.parseInt(name().substring(name().length() - 1));
                         }
                    }
                    @ParameterizedTest
                    @EnumSource(value = TestEnum.class, names = {"TEST3", "TEST4", "TEST5"})
                    void testEnum(TestEnum te) {
                        assertDoesNotThrow(() -> te.number());
                    }
                    @Test
                    void testNullEnum_TEST2() {
                        assertDoesNotThrow(() -> TestEnum.TEST2.number());
                    }
                }
                """;
        assertDoesNotThrow(() -> createUmlModel(originalSourceCode));
        String parameterizedTestCode = """
                    import org.junit.jupiter.params.ParameterizedTest;
                    public class TestClass {
                        enum TestEnum {
                             TEST1, TEST2, TEST3, TEST4, TEST5;
                             int number() {
                                if (name() == "TEST1") {
                                    throw new NullPointerException();
                                }
                                return Integer.parseInt(name().substring(name().length() - 1));
                             }
                        }
                        @ParameterizedTest
                        @EnumSource(value = TestEnum.class, names = {"TEST2", "TEST3", "TEST4", "TEST5"})
                        void testEnum(TestEnum te) {
                            assertDoesNotThrow(() -> te.number());
                        }
                    }""";
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(2, refactorings.size());
        assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION)));
        assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST)));
    }

    @Test
    void testNullAndEmptySource() throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                public class TestClass {
                    @Test
                    void testTestFileRelativePath() {
                         try {
                             new MyClass(null);
                             fail();
                         } catch (IllegalArgumentException e) {}
                    }
                    @Test
                    void testTestFileRelativePath_empty() {
                         try {
                             new MyClass("");
                             fail();
                         } catch (IllegalArgumentException e) {}
                    }
                }
                """;
        String parameterizedTestCode = """
                public class TestClass {
                    @ParameterizedTest
                    @NullAndEmptySource
                    void testTestFileRelativePath(String s) {
                         try {
                             new MyClass(s);
                             fail();
                         } catch (IllegalArgumentException e) {}
                    }
                }
                """;
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(2, refactorings.size());
        assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
        assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
    }

    @Test
    void testValueSource() throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                public class TestClass {
                    @Test
                    void testTestFileRelativePath_null() {
                        assertThrows(InvalidParameterException.class, () -> {
                            throw new InvalidParameterException();
                        });
                    }
                    @Test
                    void testTestFileRelativePath_empty() {
                        assertThrows(IllegalArgumentException.class, () -> {
                            throw new InvalidParameterException();
                        });
                    }
                }
                """;
        String parameterizedTestCode = """
                public class TestClass {
                    @ParameterizedTest
                    @ValueSource(classes = {InvalidParameterException.class, IllegalArgumentException.class})
                    void testTestFileRelativePath(Class<? extends Exception> aClass) {
                        assertThrows(aClass, () -> {
                            throw new InvalidParameterException();
                        });
                    }
                }
                """;
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(2, refactorings.size());
        assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
        assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
    }
    @Disabled("TODO: Replicate testEnumSource use of UMLModelASTReader with fileMap and add support for CSV files")
    @Nested
    class TestCsvFileSource_OtherPathFormats {
        @TempDir static Path dir;
        private static UMLModel originalModel;

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
    @Nested
    class TestCsvFileSource_AbsolutePath {
        @TempDir static Path dir;
        private static UMLModelDiff diff;

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