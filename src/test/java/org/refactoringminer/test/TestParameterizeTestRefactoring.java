package org.refactoringminer.test;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.builder.Builder;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TestParameterizeTestRefactoring {
    private static CompilationUnit parse(char[] sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(sourceCode);
        return  (CompilationUnit) parser.createAST(null);
    }
    private static List<RefactoringType> repeat(RefactoringType r, int times) {
        ArrayList<RefactoringType> result = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            result.add(r);
        }
        return result;
    }
    private static Collection<Arguments> testParameterizeTest() {
        List<Arguments> arguments = new ArrayList<>();
        {
            TestSrcCodeBuilder originalCodeBuilder = new TestSrcCodeBuilder();
            originalCodeBuilder.testMethod("testTestFileRelativePath")
                    .statement("assertNotNull(\"\");")
                    .statement("assertTrue(\"\".length() >= 0);")
                    .statement("assertTrue(\"\".length() < 2);");
            for (int i = 1; i <= 5; i++) {
                originalCodeBuilder.testMethod(String.format("testTestFileRelativePath_%d",i))
                        .statement(String.format("assertNotNull(\"%d\");",i))
                        .statement(String.format("assertTrue(\"%d\".length() >= 0);",i))
                        .statement(String.format("assertTrue(\"%d\".length() < 2);",i));
            }
            TestSrcCodeBuilder newCodeBuilder = new TestSrcCodeBuilder();
            newCodeBuilder.parameterize()
                    .testMethod("testTestFileRelativePath")
                    .annotate("@EmptySource")
                    .annotate("@ValueSource(strings={\"1\",\"2\",\"3\",\"4\",\"5\"})")
                    .parameter("String s")
                    .statement("assertNotNull(s);")
                    .statement("assertTrue(s.length() >= 0);")
                    .statement("assertTrue(s.length() < 2);");
            arguments.add(Arguments.of(Map.of("src/test/java/com/test/TestClass.java", originalCodeBuilder.build()),
                                    Map.of("src/test/java/com/test/TestClass.java", newCodeBuilder.build()),
                    Set.of("."), repeat(RefactoringType.PARAMETERIZE_TEST, 6)));
            originalCodeBuilder = new TestSrcCodeBuilder();
            originalCodeBuilder.testMethod("testTestFileRelativePath")
                    .statement("String s = \"\";")
                    .statement("assertNotNull(\"\");")
                    .statement("assertTrue(\"\".length() >= 0);")
                    .statement("assertTrue(\"\".length() < 2);");
            for (int i = 1; i <= 5; i++) {
                originalCodeBuilder.testMethod(String.format("testTestFileRelativePath_%d",i))
                        .statement(String.format("String s = \"%d\";",i))
                        .statement(String.format("assertNotNull(\"%d\");",i))
                        .statement(String.format("assertTrue(\"%d\".length() >= 0);",i))
                        .statement(String.format("assertTrue(\"%d\".length() < 2);",i));
            }
            arguments.add(Arguments.of(Map.of("src/test/java/com/test/TestClass.java", originalCodeBuilder.build()),
                    Map.of("src/test/java/com/test/TestClass.java", newCodeBuilder.build()),
                    Set.of("."), repeat(RefactoringType.PARAMETERIZE_TEST, 6)));
        }
        Function<String, String> enumDeclaration = (String methoDeclaration) -> String.format("public enum TestEnum {TEST1, TEST2, TEST3, TEST4, TEST5;\n%s}",methoDeclaration);
        {
            Set<String> dirSet = Set.of("src/", "src/main/", "src/main/java", "src/main/java/test");
            TestSrcCodeBuilder newCodeBuilder = new TestSrcCodeBuilder().testPackage("com.test")
                    .importStatement("org.junit.jupiter.params.ParameterizedTest")
                    .importStatement("org.junit.jupiter.params.provider.EnumSource")
                    .importStatement("static org.junit.jupiter.api.Assertions.assertTrue")
                    .testMethod("testEnum")
                    .parameterize()
                    .annotate("@EnumSource")
                    .parameter("TestEnum te")
                    .statement("assertTrue(te.number() >= 1 && te.number() <= 5);");
            Map<String, String> files = new HashMap<>(Map.of(
                    "src/main/java/com/test/TestEnum.java",
                    "package com.test;\n" + enumDeclaration.apply("int number() {return Integer.parseInt(name().substring(name().length() - 1));}"),
                    "src/test/java/com/test/TestClass.java", newCodeBuilder.build()));
            HashMap<String, String> oldFiles = new HashMap<>(files);
            TestSrcCodeBuilder oldCodeBuilder = new TestSrcCodeBuilder().testPackage("com.test")
                    .importStatement("org.junit.jupiter.api.Test")
                    .importStatement("static org.junit.jupiter.api.Assertions.assertTrue");
            for (int i = 1; i <= 5; i++) {
                oldCodeBuilder.testMethod(String.format("testEnum_%d",i))
                        .statement(String.format("assertTrue(TestEnum.TEST%d.number() >= 1 && TestEnum.TEST%d.number() <= 5);",i,i));
            }
            oldFiles.replace("src/test/java/com/test/TestClass.java", oldCodeBuilder.build());
            arguments.add(Arguments.of(oldFiles, files, dirSet, repeat(RefactoringType.PARAMETERIZE_TEST, 5)));
            files.replace("src/test/java/com/test/TestClass.java",new TestSrcCodeBuilder().testPackage("com.test")
                    .importStatement("org.junit.jupiter.params.ParameterizedTest")
                    .importStatement("org.junit.jupiter.params.provider.EnumSource")
                    .importStatement("static org.junit.jupiter.api.Assertions.assertTrue")
                    .testMethod("testEnum")
                    .parameterize()
                    .annotate("@EnumSource(TestEnum.class)")
                    .parameter("TestEnum te")
                    .statement("assertTrue(te.number() >= 1 && te.number() <= 5);").build());
            arguments.add(Arguments.of(oldFiles, files, dirSet, repeat(RefactoringType.PARAMETERIZE_TEST, 5)));
        }
        {
            String prefix = enumDeclaration.apply("int number() {\n" +
                    "                            if (name() == \"TEST1\") {\n" +
                    "                                throw new NullPointerException();\n" +
                    "                            }\n" +
                    "                            return Integer.parseInt(name().substring(name().length() - 1));\n" +
                    "                         }");
            arguments.add(Arguments.of(
                    Map.of("src/test/java/com/test/TestClass.java",new TestSrcCodeBuilder()
                        .testPackage("com.test")
                        .importStatement("static org.junit.jupiter.api.Assertions.*")
                        .prefix(()->prefix)
                        .testMethod("testNullEnum_TEST1")
                            .statement("assertThrows(NullPointerException.class,()->TestEnum.TEST1.number());")
                        .testMethod("testNullEnum_null")
                            .statement("TestEnum te = null;")
                            .statement("assertThrows(NullPointerException.class,()->te.number());")
                        .build()),
                    Map.of("src/test/java/com/test/TestClass.java",new TestSrcCodeBuilder()
                        .testPackage("com.test")
                        .importStatement("static org.junit.jupiter.api.Assertions.*")
                        .importStatement("org.junit.jupiter.params.ParameterizedTest")
                        .importStatement("org.junit.jupiter.params.provider.NullSource")
                        .importStatement("org.junit.jupiter.params.provider.EnumSource")
                        .prefix(()->prefix)
                        .testMethod("testNullEnum")
                                .parameterize()
                                .annotate("@NullSource")
                                .annotate("@EnumSource(value=TestEnum.class,names={\"TEST1\"})")
                                .parameter("TestEnum en")
                            .statement("assertThrows(NullPointerException.class,()->en.number());")
                        .build()),
                    Set.of("."),
                    repeat(RefactoringType.PARAMETERIZE_TEST, 2)));
        }
        {

            arguments.add(Arguments.of(
                            Map.of("src/test/java/com/test/TestClass.java", new TestSrcCodeBuilder()
                                .testMethod("testSrcFileRelativePath")
                                    .statement("try{new MyClass(null);fail();}catch(IllegalArgumentException e){}")
                                .testMethod("testSrcFileRelativePath_empty")
                                    .statement("try{new MyClass(\"\");fail();}catch(IllegalArgumentException e){}")
                                .build()),
                            Map.of("src/test/java/com/test/TestClass.java", new TestSrcCodeBuilder()
                                .testMethod("testSrcFileRelativePath")
                                    .parameterize()
                                    .annotate("@NullAndEmptySource")
                                    .parameter("String s")
                                    .statement("try{new MyClass(s);fail();}catch(IllegalArgumentException e){}").build()),
                            Set.of("."),repeat(RefactoringType.PARAMETERIZE_TEST, 2)));
        }
        {
            arguments.add(Arguments.of(
                    Map.of("src/test/java/com/test/TestClass.java",new TestSrcCodeBuilder()
                            .testMethod("testTestFileRelativePath_null")
                                .statement("assertThrows(InvalidParameterException.class,()->{\nthrow new InvalidParameterException();\n});\n")
                            .testMethod("testTestFileRelativePath_empty")
                                .statement("assertThrows(IllegalArgumentException.class,()->{\nthrow new InvalidParameterException();\n});\n")
                            .build()),
                    Map.of("src/test/java/com/test/TestClass.java",new TestSrcCodeBuilder()
                            .testMethod("testTestFileRelativePath")
                                .parameterize()
                                .annotate("@ValueSource(classes = {InvalidParameterException.class, IllegalArgumentException.class})")
                                .parameter("Class<? extends Exception> aClass")
                                .statement("assertThrows(aClass,()->{throw new InvalidParameterException();});")
                            .build()),
                    Set.of("."),
                    repeat(RefactoringType.PARAMETERIZE_TEST, 2)));
        }
        {
            TestSrcCodeBuilder commonCode = new TestSrcCodeBuilder().testMethod("testMethod").parameterize().parameter("String parameter").statement("assertEquals(parameter, null);");

            List<String> sourceCodeAfter = List.of(
                    TestSrcCodeBuilder.copy(commonCode).annotate("@ValueSource(strings = {\"value\",\"value2\"})").build(),
                    TestSrcCodeBuilder.copy(commonCode).annotate("@CsvSource({\"value\",\"value2\"})").build(),
                    TestSrcCodeBuilder.copy(commonCode).annotate("@CsvSource({\"value\",\n\"value2\"})").build(),
                    TestSrcCodeBuilder.copy(commonCode).annotate("@CsvSource(value={\"value\",\"value2\"})").build(),
                    TestSrcCodeBuilder.copy(commonCode).annotate("@CsvSource(value = {\"value\",\n\"value2\"})").build());

            for (String refactoredCode : sourceCodeAfter) {
                arguments.add(Arguments.of(Map.of("src/test/java/com/test/TestClass.java",
                        new TestSrcCodeBuilder()
                                .testMethod("testMethod")
                                .statement("assertEquals(null, null);").build()),
                        Map.of("src/test/java/com/test/TestClass.java",refactoredCode),
                        Set.of("."),new ArrayList<>(Arrays.asList(
                            RefactoringType.ADD_PARAMETER,
                            RefactoringType.ADD_METHOD_ANNOTATION,
                            RefactoringType.ADD_METHOD_ANNOTATION,
                            RefactoringType.REMOVE_METHOD_ANNOTATION))));
            }
        }
        return arguments;
    }
    @ParameterizedTest
    @MethodSource
    void testParameterizeTest(Map<String, String> filesBefore, Map<String, String> filesAfter, Set<String> dirs, List<RefactoringType> expectedRefactorings) throws RefactoringMinerTimedOutException {
        UMLModel cuBefore = new UMLModelASTReader(filesBefore, dirs, true).getUmlModel();
        UMLModel cuAfter = new UMLModelASTReader(filesAfter, dirs, true).getUmlModel();
        UMLModelDiff modelDiff = cuBefore.diff(cuAfter);
        List<RefactoringType> refactoringsFound = new ArrayList<>();
        for (Refactoring refactoring : modelDiff.getRefactorings()) {
            refactoringsFound.add(refactoring.getRefactoringType());
        }
        assertEquals(expectedRefactorings.size(), refactoringsFound.size());
        HashSet<RefactoringType> uniqueExpectedRefactorings = new HashSet<>(expectedRefactorings);
        uniqueExpectedRefactorings.removeAll(new HashSet<RefactoringType>(refactoringsFound));
        assertEquals(0, uniqueExpectedRefactorings.size(), "Not all expected refactorings types were found:" + uniqueExpectedRefactorings.stream().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals(expectedRefactorings.size(), matchRefactorings(expectedRefactorings, refactoringsFound).size(), "Not all refactorings matched");
        assertEquals(0, expectedRefactorings.size(), "Not all expected refactoring instances were found:" + expectedRefactorings.stream().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals(0, refactoringsFound.size(), "Not all found refactoring instances were expected:" + refactoringsFound.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    private static List<RefactoringType> matchRefactorings(List<RefactoringType> expectedRefactorings, List<RefactoringType> refactoringsFound) {
        List<RefactoringType> matchedRefactorings = new ArrayList<>();
        for (Iterator<RefactoringType> i = expectedRefactorings.iterator(); i.hasNext(); ) {
            RefactoringType expectedRefactoring = i.next();
            for (Iterator<RefactoringType> j = refactoringsFound.iterator(); j.hasNext(); ) {
                RefactoringType refactoringFound = j.next();
                if (refactoringFound.equals(expectedRefactoring)) {
                    matchedRefactorings.add(expectedRefactoring);
                    i.remove();
                    j.remove();
                    break;
                }
            }
        }
        return matchedRefactorings;
    }

    @Disabled("TODO: Add support for Modify Method Annotation with additional parameter")
    @Test
    void testEnumParameterMerged() throws RefactoringMinerTimedOutException {
        Supplier<String> prefix = () -> "enum TestEnum {\n" +
                "                         TEST1, TEST2, TEST3, TEST4, TEST5;\n" +
                "                         int number() {\n" +
                "                            if (name() == \"TEST1\") {\n" +
                "                                throw new NullPointerException();\n" +
                "                            }\n" +
                "                            return Integer.parseInt(name().substring(name().length() - 1));\n" +
                "                         }\n" +
                "                    }";
        String originalSourceCode = new TestSrcCodeBuilder()
                .prefix(prefix)
                .testMethod("testEnum")
                    .parameterize()
                    .parameter("TestEnum te")
                    .annotate("@EnumSource(value = TestEnum.class, names = {\"TEST3\", \"TEST4\", \"TEST5\"})")
                    .statement("assertDoesNotThrow(() -> te.number());")
                .testMethod("testNullEnum_TEST2")
                    .statement("assertDoesNotThrow(() -> TestEnum.TEST2.number());")
                .build();
        assertDoesNotThrow(() -> createUmlModel(originalSourceCode));
        String parameterizedTestCode = new TestSrcCodeBuilder()
                .prefix(prefix)
                .testMethod("testEnum")
                    .parameterize()
                    .parameter("TestEnum te")
                    .annotate("@EnumSource(value = TestEnum.class, names = {\"TEST2\", \"TEST3\", \"TEST4\", \"TEST5\"})")
                    .statement("assertDoesNotThrow(() -> te.number());")
                .build();
        UMLModel originalModel = createUmlModel(originalSourceCode);
        UMLModel newModel = createUmlModel(parameterizedTestCode);
        UMLModelDiff diff = originalModel.diff(newModel);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertEquals(2, refactorings.size());
        assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION)));
        assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST)));
    }

    @Disabled("TODO: Replicate testEnumSource use of UMLModelASTReader with fileMap and add support for CSV files")
    @Nested
    class TestCsvFileSource_OtherPathFormats {
        @TempDir Path dir;
        private UMLModel originalModel;
        private Path csvPath;

        @BeforeEach
        void setUp() throws RefactoringMinerTimedOutException {
            String originalSourceCode = new TestSrcCodeBuilder().testMethod("testMethod_A")
                    .statement("assertNotEquals(\"A\", null);")
                    .statement("assertNotEquals(\"B\", null);")
                    .testMethod("testMethod_B")
                    .statement("assertNotEquals(\"C\", null);")
                    .statement("assertNotEquals(\"D\", null);")
                    .build();
            csvPath = dir.resolve("src/test/resources/file.csv");
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
            originalModel = createUmlModel(originalSourceCode);
        }

        @Test
        void testTestFileRelativePath() throws RefactoringMinerTimedOutException {
            csvPath = dir.resolve("src/test/java/TestClass.java").toAbsolutePath().relativize(dir.resolve("src/test/resources/file.csv"));
            assertFalse(csvPath.isAbsolute());
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate(String.format("@CsvFileSource(files = \"%s\")",csvPath.toString()))
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertNotEquals(param2, null);")
                    .build();
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testRelativePath() throws RefactoringMinerTimedOutException {
            assertFalse(csvPath.isAbsolute());
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate(String.format("@CsvFileSource(files = \"%s\")",csvPath.toString()))
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertNotEquals(param2, null);")
                    .build();
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testProjectRelativePath() throws RefactoringMinerTimedOutException {
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate("@CsvFileSource(files = \"src/test/resources/file.csv\")")
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertNotEquals(param2, null);")
                    .build();
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            assertEquals(2, refactorings.size());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(0).getName());
            assertEquals(RefactoringType.PARAMETERIZE_TEST.getDisplayName(), refactorings.get(1).getName());
        }

        @Test
        void testProjectRelativeResource() throws RefactoringMinerTimedOutException {
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate("@CsvFileSource(files = \"file.csv\")")
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertNotEquals(param2, null);")
                    .build();
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
        @TempDir Path dir;
        private UMLModelDiff diff;

        @BeforeEach
        void setUp() throws RefactoringMinerTimedOutException {
            String originalSourceCode = new TestSrcCodeBuilder().testMethod("testMethod_A")
                    .statement("assertNotEquals(\"A\", null);")
                    .statement("assertNotEquals(\"B\", null);")
                    .testMethod("testMethod_B")
                    .statement("assertNotEquals(\"C\", null);")
                    .statement("assertNotEquals(\"D\", null);")
                    .build();
            Path csvPath = dir.resolve("file.csv");
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate(String.format("@CsvFileSource(files = \"%s\")",csvPath.toString()))
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertNotEquals(param2, null);")
                    .build();
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
    @Nested
    class TestCheckForTestParameterizations_OneStringParam_Plugin {

        private String originalSourceCode;
        private TestSrcCodeBuilder parameterizedTestBuilder;

        @BeforeEach
        void setUp() {
            originalSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .statement("assertEquals(null, null);")
                    .build();
            parameterizedTestBuilder = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .parameter("String parameter")
                    .statement("assertEquals(parameter, null);");
        }

        @Test
        void test_SingleMemberAnnotation() throws RefactoringMinerTimedOutException {
            String newSourceCode = parameterizedTestBuilder
                    .annotate("@CsvSource({\"value\",\"value2\"})")
                    .build();
            AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) extractRefactorings(newSourceCode).stream()
                    .filter(r -> r.getName().equals("Add Method Annotation") && ((AddMethodAnnotationRefactoring) r).getAnnotation().getTypeName().endsWith("Source"))
                    .findFirst().get();
            assertEquals("{\"value\",\"value2\"}", addMethodAnnotationRefactoring.getAnnotation().getValue().getExpression(), "Member value pair annotation has incorrect value");
        }

        @Test
        void testCheckForTestParameterizations_OneStringParam_Plugin_NormalAnnotation() throws RefactoringMinerTimedOutException {
            String newSourceCode = parameterizedTestBuilder
                    .annotate("@ValueSource(strings = {\"value\",\"value2\"})")
                    .build();
            AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) extractRefactorings(newSourceCode).stream()
                    .filter(r -> r.getName().equals("Add Method Annotation") && ((AddMethodAnnotationRefactoring) r).getAnnotation().getTypeName().endsWith("Source"))
                    .findFirst().get();
            assertEquals("{\"value\",\"value2\"}", addMethodAnnotationRefactoring.getAnnotation().getMemberValuePairs().values().stream().findAny().get().getExpression(), "Normal annotation has incorrect value");
        }

        private List<Refactoring> extractRefactorings(String newSourceCode) throws RefactoringMinerTimedOutException {
            UMLModel originalModel = createUmlModel(originalSourceCode);
            UMLModel newModel = createUmlModel(newSourceCode);
            UMLModelDiff diff = originalModel.diff(newModel);
            List<Refactoring> refactorings = diff.getRefactorings();
            return refactorings;
        }
    }

    private static UMLModel createUmlModel(String sourceCode) {
        CompilationUnit cu = parse(sourceCode.toCharArray());
        Map<String, String> javaFileContents = Map.of("TestClass.java", sourceCode);
        UMLModel model = new UMLModelASTReader(javaFileContents, Set.of("."), false).getUmlModel();
        ASTNode node = (ASTNode) cu;
        assertNotNull(node);
        LocationInfo location = new LocationInfo(cu, "TestClass.java", node, LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass aClass = new UMLClass("org.refactoringminer.test", "TestClass", location, true, Collections.emptyList());
        aClass.setVisibility(Visibility.PUBLIC);
        return model;
    }
}
class TestSrcCodeBuilder implements Builder<String> {
    enum MethodComponent {STATEMENT, ANNOTATION, PARAMETER;};
    private String className;
    private boolean parameterized;
    private String lastAddedMethod;
    private String pkg;
    private List<String> imports;

    private Supplier<String> prefix;

    private Supplier<String> suffix;
    private Map<String, Map<MethodComponent, List<String>>> methods;
    public TestSrcCodeBuilder prefix(Supplier<String> prefix) {
        this.prefix = prefix;
        return this;
    }

    public TestSrcCodeBuilder suffix(Supplier<String> suffix) {
        this.suffix = suffix;
        return this;
    }
    public TestSrcCodeBuilder() {
        this.imports = new ArrayList<>();
        parameterized = false;
        methods = new HashMap<>();
    }
    public static TestSrcCodeBuilder copy(TestSrcCodeBuilder other) {
        TestSrcCodeBuilder self = new TestSrcCodeBuilder();
        self.pkg = other.pkg != null ? new String(other.pkg) : null;
        self.imports = new ArrayList<>(other.imports);
        self.className = other.className != null ? new String(other.className) : null;
        self.parameterized = other.parameterized;
        self.lastAddedMethod = other.lastAddedMethod != null ? new String(other.lastAddedMethod) : null;
        self.methods = new HashMap<>(other.methods);
        for (String methodName : other.methods.keySet()) {
            self.methods.put(methodName, new HashMap<>(other.methods.get(methodName)));
            for (MethodComponent componentType : self.methods.get(methodName).keySet()) {
                self.methods.get(methodName).put(componentType, new ArrayList<>(self.methods.get(methodName).get(componentType)));
            }
        }
        self.prefix = other.prefix;
        self.suffix = other.suffix;
        return self;
    }
    public TestSrcCodeBuilder testPackage(String pkg) {
        this.pkg = String.format("package %s;",pkg);
        return this;
    }
    public TestSrcCodeBuilder importStatement(String importStmt) {
        this.imports.add(String.format("import %s;",importStmt));
        return this;
    }
    public TestSrcCodeBuilder testClass(String name) {
        className = name;
        return this;
    }
    public TestSrcCodeBuilder testMethod(String name) {
        assert !methods.containsKey(name) : String.format("Conflict: another method is already named (%s)",name);
        lastAddedMethod = name;
        methods.put(name, Map.of(MethodComponent.PARAMETER,new ArrayList<>(),
                                MethodComponent.STATEMENT,new ArrayList<>(),
                                MethodComponent.ANNOTATION,new ArrayList<>()));
        return this;
    }
    public TestSrcCodeBuilder parameter(String param) {
        methods.get(lastAddedMethod).get(MethodComponent.PARAMETER).add(param);
        return this;
    }
    public TestSrcCodeBuilder annotate(String name) {
        methods.get(lastAddedMethod).get(MethodComponent.ANNOTATION).add(name);
        return this;
    }
    public TestSrcCodeBuilder statement(String stmt) {
        methods.get(lastAddedMethod).get(MethodComponent.STATEMENT).add(stmt);
        return this;
    }
    public TestSrcCodeBuilder parameterize() {
        parameterized = true;
        return this;
    }
    @Override
    public String build() {
        if (className == null || className.isEmpty()) {
            className = "TestClass";
        }
        String headerStatements = pkg != null ? String.format("%s\n",pkg) : "";
        for (String importStmt : imports) {
            headerStatements = headerStatements.concat(String.format("%s\n",importStmt));
        }
        String methodDeclarations = "";
        for (String m : methods.keySet()) {
            methodDeclarations = methodDeclarations.concat(String.format("%s %s public void %s(%s){%s}\n",
                    parameterized ? "@ParameterizedTest" : "@Test",
                    getAsString(m, MethodComponent.ANNOTATION),
                    m,
                    getAsString(m, MethodComponent.PARAMETER),
                    getAsString(m, MethodComponent.STATEMENT)
            ));
        }
        return String.format("%spublic class %s {%s%s%s}",headerStatements,
                className,
                this.prefix != null ? this.prefix.get() : "",
                methodDeclarations,
                this.suffix != null ? this.suffix.get() : "");
    }
    private String getAsString(String m, MethodComponent component, CharSequence delimiter) {
        return String.join(delimiter,methods.get(m).get(component));
    }
    private String getAsString(String m, MethodComponent component) {
        return component == MethodComponent.PARAMETER ? getAsString(m,component,", ") : getAsString(m,component," ");
    }
}