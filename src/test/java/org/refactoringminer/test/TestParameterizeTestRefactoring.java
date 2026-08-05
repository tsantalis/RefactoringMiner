package org.refactoringminer.test;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.ObjectCreation;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.tuple.Pair;
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
import org.refactoringminer.api.ModelDiffRefactoringHandler;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.refactoringminer.utils.Assertions.assertHasSameElementsAs;

class TestParameterizeTestRefactoring {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
    private GitHistoryRefactoringMinerImpl miner;
    private List<String> actual;
    private List<String> expected;

    @BeforeEach
    void setUpMiner() {
        miner = new GitHistoryRefactoringMinerImpl();
        actual = new ArrayList<>();
        expected = new ArrayList<>();
    }

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
    @SafeVarargs
    private static List<RefactoringType> combine(List<RefactoringType>... lists) {
        return Arrays.stream(lists).flatMap(List::stream).collect(Collectors.toList());
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
                    Set.of("."), combine(repeat(RefactoringType.PARAMETERIZE_TEST, 6),repeat(RefactoringType.PARAMETERIZE_VARIABLE, 6))));
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
                    .statement("assertEquals(\"B\", null);")
                    .testMethod("testMethod_B")
                    .statement("assertNotEquals(\"C\", null);")
                    .statement("assertEquals(\"D\", null);")
                    .build();
            Path csvPath = dir.resolve("file.csv");
            String newSourceCode = new TestSrcCodeBuilder().testMethod("testMethod")
                    .parameterize()
                    .annotate(String.format("@CsvFileSource(files = \"%s\")",csvPath.toString()))
                    .parameter("String param1")
                    .parameter("String param2")
                    .statement("assertNotEquals(param1, null);")
                    .statement("assertEquals(param2, null);")
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
            List<VariableDeclaration> addedParameters = mapper.getOperationSignatureDiff().get().getAddedParameters();
            assertEquals(((Replacement) replacements[0]).getAfter(), addedParameters.get(0).getVariableName());
            assertEquals(((Replacement) replacements[1]).getType(), Replacement.ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
            assertEquals(param2, ((Replacement) replacements[1]).getBefore());
            assertEquals(((Replacement) replacements[1]).getAfter(), addedParameters.get(1).getVariableName());
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

    @ParameterizedTest
    @CsvSource({
            //Parameterize Test with Framework support
            ////Extract Common Logic from Multiple Test Methods
//            "https://github.com/aws/aws-sdk-java-v2.git, 4236a962dc0ca45149845317caa144a1ba768c5f, aws-sdk-java-v2-4236a962dc0ca45149845317caa144a1ba768c5f.txt", //FIXME: JUnit 4 parameterization not supported, Nikos: there is no refactoring, the parameters are for new test
            "https://github.com/Atrox/haikunatorjava.git, 42679988419b68dd51f0a7b3c045536b3c5ef37b, haikunatorjava-42679988419b68dd51f0a7b3c045536b3c5ef37b.txt",
            "https://github.com/opentripplanner/OpenTripPlanner.git, 1abed1191c2df7a747ef21cd3b669c14d54c3011, OpenTripPlanner-1abed1191c2df7a747ef21cd3b669c14d54c3011.txt",
//            "https://github.com/samtools/htsjdk.git, 1734eb99e5dcf16d92febead5e1b62323e0b6199, htsjdk-1734eb99e5dcf16d92febead5e1b62323e0b6199.txt", //FIXME: TestNG not supported, Nikos: 3 tests parameterized into testCheckTerminationForFiles + newly added tests
//            "https://github.com/apache/hbase.git, 2306820df8b41d9af5227465ee2cf9e18b8f0b5c, hbase-2306820df8b41d9af5227465ee2cf9e18b8f0b5c.txt", //FIXME: JUnit 4 parameterization not supported
            "https://github.com/spring-projects/spring-boot.git, 16439ad6e364267033b8b157f3608b46c654dffa, spring-boot-16439ad6e364267033b8b157f3608b46c654dffa.txt",
            ////Add Parameterized Test
//            "https://github.com/hapifhir/hapi-fhir/pull/5764.git, ad470cff726d800cbf9baa49abd6a9a536781ec0, hapi-fhir-pull-5764-ad470cff726d800cbf9baa49abd6a9a536781ec0.txt", //TODO: Should test addition of parameterized test be supported?
            ////Merge Data Provider
//            "https://github.com/samtools/htsjdk.git, 17c4b9d29dc0ee7573d32e7364d36fc92e4b2493, htsjdk-17c4b9d29dc0ee7573d32e7364d36fc92e4b2493.txt", //FIXME: Merge Data Provider not supported
            ////Multiple data and multiple algorithms become parameterized test with inheritance and fixture overrides
//            "https://github.com/apache/hadoop.git, 4d01dbda508691beb07a4c8bfe113ec568166ddc, hadoop-4d01dbda508691beb07a4c8bfe113ec568166ddc.txt", //FIXME: JUnit 4 parameterization not supported
    })
    public void testParameterizedTestMappings(String url, String commit, String testResultFileName) {
        miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), (ModelDiffRefactoringHandler) (commitId, refactoringsAtRevision, modelDiff) -> {
            for (Refactoring ref : refactoringsAtRevision) {
                if (ref instanceof ParameterizeTestRefactoring parameterizedTestRefactoring) {
                    mapperInfo(parameterizedTestRefactoring.getBodyMapper().getMappings(), parameterizedTestRefactoring.getRemovedOperation(), parameterizedTestRefactoring.getParameterizedTestOperation());
                }
            }
            checkDataProviderRowConsistency(modelDiff);
        });
        assertion(testResultFileName);
    }

    @ParameterizedTest
    @CsvSource({
            "https://github.com/conveyal/r5.git, 62a042e56b21d2e7c919552af39eca34357a82a7, r5-62a042e56b21d2e7c919552af39eca34357a82a7-dataprovider.txt",
            "https://github.com/apache/directory-ldap-api.git, 8965a541bbeefd49028a5405264e40aed69ac5d0, directory-ldap-api-8965a541bbeefd49028a5405264e40aed69ac5d0-dataprovider.txt",
            "https://github.com/greenjoe/lambdaFromString.git, 0cbf3774c6f508c21cbb789bfe285117499f1e31, lambdaFromString-0cbf3774c6f508c21cbb789bfe285117499f1e31-dataprovider.txt",
            "https://github.com/frosch95/SmartCSV.fx.git, f41979960b3844215175838de1cb6d215cd1cb47, SmartCSV.fx-f41979960b3844215175838de1cb6d215cd1cb47-dataprovider.txt",
    })
    public void testDataProviderLiteralMappings(String url, String commit, String testResultFileName) {
        miner.detectAtCommitWithGitHubAPI(url, commit, new File(REPOS), (ModelDiffRefactoringHandler) (commitId, refactoringsAtRevision, modelDiff) -> {
            refactoringsAtRevision.forEach(this::checkDataProviderLiteralMapping);
            checkDataProviderRowConsistency(modelDiff);
        });
        assertion(testResultFileName);
    }

    private void checkDataProviderRowConsistency(UMLModelDiff modelDiff) {
        try {
            DataProviderRowConsistency check = new DataProviderRowConsistency();
            check.assertMatchedParameterCountsConsistent(modelDiff);
            check.assertMatchedParameterIndexOrderConsistent(modelDiff);
        } catch (RefactoringMinerTimedOutException e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<Arguments> testDataProviderLiteralMappingsForFiles() {
        return Stream.of(Arguments.of(
                Map.of("DescriptorHandlerTestV2", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/handlers/v2/DescriptorHandlerTest.java",
                        "DescriptorHandlerTestV3", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/handlers/v3/DescriptorHandlerTest.java",
                        "DescriptorParserTestV2", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/handlers/v2/DescriptorParserTest.java",
                        "DescriptorParserTestV3", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/handlers/v3/DescriptorParserTest.java",
                        "PlatformMergerTest", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/mergers/PlatformMergerTest.java",
                        "SchemaValidatorTest", "multiapps-mta/src/test/java/org/cloudfoundry/multiapps/mta/schema/SchemaValidatorTest.java"),
                "multiapps-82c2cc85b8b7790470c8380b82aad27abffc290b-dataprovider.txt"
                ),
                Arguments.of(Map.of("PortAssignmentTest", "zookeeper-server/src/test/java/org/apache/zookeeper/PortAssignmentTest.java",
                        "RemoveWatchesTest", "zookeeper-server/src/test/java/org/apache/zookeeper/RemoveWatchesTest.java",
                        "EagerACLFilterTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/EagerACLFilterTest.java",
                        "QuorumRequestPipelineTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/QuorumRequestPipelineTest.java",
                        "ReconfigDuringLeaderSyncTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/ReconfigDuringLeaderSyncTest.java",
                        "UnifiedServerSocketModeDetectionTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/UnifiedServerSocketModeDetectionTest.java",
                        "WatchLeakTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/WatchLeakTest.java",
                        "WatchManagerTest", "zookeeper-server/src/test/java/org/apache/zookeeper/server/watch/WatchManagerTest.java",
                        "MultiOperationTest", "zookeeper-server/src/test/java/org/apache/zookeeper/test/MultiOperationTest.java",
                        "ObserverMasterTest", "zookeeper-server/src/test/java/org/apache/zookeeper/test/ObserverMasterTest.java"),
                "zookeeper-c42c8c94085ed1d94a22158fbdfe2945118a82bc-dataprovider.txt")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDataProviderLiteralMappingsForFiles(Map<String, String> filePathsByCacheName, String testResultFileName) throws Exception {
        Map<String, String> fileContentsBefore = new LinkedHashMap<>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : filePathsByCacheName.entrySet()) {
            String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + entry.getKey() + "-v1.txt"));
            String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + entry.getKey() + "-v2.txt"));
            fileContentsBefore.put(entry.getValue(), contentsV1);
            fileContentsCurrent.put(entry.getValue(), contentsV2);
        }
        UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<>());
        UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<>());
        UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
        for (Refactoring ref : modelDiff.getRefactorings()) {
            checkDataProviderLiteralMapping(ref);
        }
        checkDataProviderRowConsistency(modelDiff);
        assertion(testResultFileName);
    }

    private void checkDataProviderLiteralMapping(Refactoring ref) {
        if (ref instanceof ParameterizeTestRefactoring parameterizeTestRefactoring) {
            if (parameterizeTestRefactoring.getDataProviderAfter() != null) {
                Set<Pair<LeafExpression, LeafExpression>> pairs = new LinkedHashSet<>();
                for (LeafMapping leafMapping : parameterizeTestRefactoring.getData()) {
                    pairs.add(Pair.of((LeafExpression) leafMapping.getFragment1(), (LeafExpression) leafMapping.getFragment2()));
                }
                mapperInfo(pairs, parameterizeTestRefactoring.getDataProviderBefore(), parameterizeTestRefactoring.getDataProviderAfter());
            }
            for (ParameterizeTestRefactoring.DataProviderOverride override : parameterizeTestRefactoring.getDataProviderOverrides()) {
                Set<Pair<LeafExpression, LeafExpression>> pairs = new LinkedHashSet<>();
                for (LeafMapping leafMapping : override.getData()) {
                    pairs.add(Pair.of((LeafExpression) leafMapping.getFragment1(), (LeafExpression) leafMapping.getFragment2()));
                }
                mapperInfo(pairs, override.getDataProviderBefore(), override.getDataProviderAfter());
            }
        }
    }

    private void assertion(String testResultFileName) {
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    private <T, Y> void mapperInfo(Set<Y> mappings, T before, T after) {
        actual.add(before + " -> " + after);
        for (var mapping : mappings) {
            if (mapping instanceof AbstractCodeMapping ac)
                mapperInfo(ac);
            else if (mapping instanceof Pair p)
                mapperInfo(p);
            else if (mapping instanceof AbstractCodeFragment frag)
                mapperInfo(frag);
            else
                throw new IllegalArgumentException("Invalid mapping type: " + mapping.getClass());
        }
    }

    private void mapperInfo(Pair mapping) {
        if (mapping.getLeft() instanceof LocationInfoProvider && mapping.getRight() instanceof LocationInfoProvider)
            actual.add(((LocationInfoProvider) mapping.getLeft()).getLocationInfo() + "==" + ((LocationInfoProvider) mapping.getRight()).getLocationInfo());
        else
            actual.add(mapping.getLeft() + "==" + mapping.getRight());
    }

    private void mapperInfo(AbstractCodeMapping mapping) {
        if (mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression) {
            return;
        }
        actual.add(mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo());
    }

    private void mapperInfo(AbstractCodeFragment component) {
        actual.add(component.getLocationInfo().toString());
    }

    // Utility class to verify the row/column structure of ParameterizeTestRefactoring's DataProvider literal matching
    @Nested
    class DataProviderRowConsistency {

        private record MatchedPair(LeafMapping mapping, int colBefore, int colAfter) {}
        private record TestCaseGroup(String providerKey, int rowBeforeIndex, int rowAfterIndex, List<MatchedPair> matches) {}

        void assertMatchedParameterCountsConsistent(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
            List<TestCaseGroup> groups = collectTestCaseGroups(modelDiff);
            Map<List<Object>, Integer> rowAfterByBefore = new HashMap<>();
            Map<List<Object>, Integer> rowBeforeByAfter = new HashMap<>();
            for (TestCaseGroup group : groups) {
                Integer previousAfter = rowAfterByBefore.putIfAbsent(List.of(group.providerKey(), group.rowBeforeIndex()), group.rowAfterIndex());
                assertTrue(previousAfter == null || previousAfter.equals(group.rowAfterIndex()),
                        "before-row " + group.rowBeforeIndex() + " (" + group.providerKey()
                                + ") has matched literals split across more than one after test case");
                Integer previousBefore = rowBeforeByAfter.putIfAbsent(List.of(group.providerKey(), group.rowAfterIndex()), group.rowBeforeIndex());
                assertTrue(previousBefore == null || previousBefore.equals(group.rowBeforeIndex()),
                        "after-row " + group.rowAfterIndex() + " (" + group.providerKey()
                                + ") has matched literals split across more than one before test case");
            }
        }

        void assertMatchedParameterIndexOrderConsistent(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
            List<TestCaseGroup> groups = collectTestCaseGroups(modelDiff);
            for (TestCaseGroup group : groups) {
                List<MatchedPair> sortedByBeforeColumn = new ArrayList<>(group.matches());
                sortedByBeforeColumn.sort(Comparator.comparingInt(MatchedPair::colBefore));
                for (int i = 1; i < sortedByBeforeColumn.size(); i++) {
                    MatchedPair previous = sortedByBeforeColumn.get(i - 1);
                    MatchedPair current = sortedByBeforeColumn.get(i);
                    assertTrue(previous.colAfter() < current.colAfter(), "matched parameter order differs - "
                            + previous.mapping().getFragment1().getString() + "(" + previous.mapping().getFragment1().getLocationInfo() + ") -> "
                            + previous.mapping().getFragment2().getString() + "(" + previous.mapping().getFragment2().getLocationInfo() + ")"
                            + " should come before "
                            + current.mapping().getFragment1().getString() + "(" + current.mapping().getFragment1().getLocationInfo() + ") -> "
                            + current.mapping().getFragment2().getString() + "(" + current.mapping().getFragment2().getLocationInfo() + ")");
                }
            }
        }

        private List<List<LeafExpression>> computeBeforeRows(UMLOperation dataProviderBefore, UMLClassBaseDiff classDiff) {
            OperationBody body = dataProviderBefore.getBody();
            if (body != null) {
                List<ObjectCreation> arrayCreations = new ArrayList<>();
                for (AbstractCall creation : body.getAllCreations()) {
                    if (creation instanceof ObjectCreation && ((ObjectCreation) creation).isArray()) {
                        arrayCreations.add((ObjectCreation) creation);
                    }
                }
                List<List<LeafExpression>> rows = new ArrayList<>();
                for (ObjectCreation creation : arrayCreations) {
                    boolean nestedInAnotherArrayCreation = arrayCreations.stream()
                            .anyMatch(other -> other != creation && other.getLocationInfo().subsumes(creation.getLocationInfo()));
                    if (!nestedInAnotherArrayCreation) {
                        rows.addAll(creation.getArrayInitializerRows());
                    }
                }
                if (!rows.isEmpty()) {
                    return rows;
                }
            }
            return classDiff.getParameterValuesAsLeafExpressions(dataProviderBefore);
        }

        private List<List<LeafExpression>> computeAfterRows(UMLClassBaseDiff classDiff, UMLOperation parameterizedTestOperation) {
            return classDiff.getParameterValuesAsLeafExpressions(parameterizedTestOperation);
        }

        private int[] locate(LeafExpression target, List<List<LeafExpression>> rows) {
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<LeafExpression> row = rows.get(rowIndex);
                for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                    if (Objects.equals(row.get(colIndex).getLocationInfo(), target.getLocationInfo())) {
                        return new int[]{rowIndex, colIndex};
                    }
                }
            }
            return null;
        }

        private List<TestCaseGroup> groupByTestCase(String providerKey, List<LeafMapping> data, List<List<LeafExpression>> rowsBefore, List<List<LeafExpression>> rowsAfter) {
            record RowKey(int rowBeforeIndex, int rowAfterIndex) {}
            Map<RowKey, List<MatchedPair>> grouped = new LinkedHashMap<>();
            for (LeafMapping mapping : data) {
                int[] before = locate((LeafExpression) mapping.getFragment1(), rowsBefore);
                int[] after = locate((LeafExpression) mapping.getFragment2(), rowsAfter);
                assertNotNull(before, "Could not locate matched literal " + mapping.getFragment1().getLocationInfo() + " in recomputed before-rows");
                assertNotNull(after, "Could not locate matched literal " + mapping.getFragment2().getLocationInfo() + " in recomputed after-rows");
                grouped.computeIfAbsent(new RowKey(before[0], after[0]), key -> new ArrayList<>())
                        .add(new MatchedPair(mapping, before[1], after[1]));
            }
            List<TestCaseGroup> groups = new ArrayList<>();
            for (Map.Entry<RowKey, List<MatchedPair>> entry : grouped.entrySet()) {
                groups.add(new TestCaseGroup(providerKey, entry.getKey().rowBeforeIndex(), entry.getKey().rowAfterIndex(), entry.getValue()));
            }
            return groups;
        }

        private List<TestCaseGroup> collectTestCaseGroups(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
            List<TestCaseGroup> groups = new ArrayList<>();
            for (Refactoring refactoring : modelDiff.getRefactorings()) {
                if (!(refactoring instanceof ParameterizeTestRefactoring)) {
                    continue;
                }
                ParameterizeTestRefactoring parameterizeTestRefactoring = (ParameterizeTestRefactoring) refactoring;
                UMLOperation parameterizedTestOperation = parameterizeTestRefactoring.getParameterizedTestOperation();
                if (parameterizeTestRefactoring.getDataProviderAfter() != null) {
                    UMLOperation dataProviderBefore = parameterizeTestRefactoring.getDataProviderBefore();
                    UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff(parameterizedTestOperation.getClassName());
                    if (classDiff != null) {
                        List<List<LeafExpression>> rowsBefore = computeBeforeRows(dataProviderBefore, classDiff);
                        List<List<LeafExpression>> rowsAfter = computeAfterRows(classDiff, parameterizedTestOperation);
                        String providerKey = dataProviderBefore.getLocationInfo() + "->" + parameterizeTestRefactoring.getDataProviderAfter().getLocationInfo();
                        groups.addAll(groupByTestCase(providerKey, parameterizeTestRefactoring.getData(), rowsBefore, rowsAfter));
                    }
                }
                for (ParameterizeTestRefactoring.DataProviderOverride override : parameterizeTestRefactoring.getDataProviderOverrides()) {
                    UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff(override.getDataProviderAfter().getClassName());
                    if (classDiff != null) {
                        List<List<LeafExpression>> rowsBefore = computeBeforeRows(override.getDataProviderBefore(), classDiff);
                        List<List<LeafExpression>> rowsAfter = computeAfterRows(classDiff, parameterizedTestOperation);
                        String providerKey = override.getDataProviderBefore().getLocationInfo() + "->" + override.getDataProviderAfter().getLocationInfo();
                        groups.addAll(groupByTestCase(providerKey, override.getData(), rowsBefore, rowsAfter));
                    }
                }
            }
            return groups;
        }
    }

    private static UMLModel createUmlModel(String sourceCode) {
        CompilationUnit cu = parse(sourceCode.toCharArray());
        Map<String, String> javaFileContents = Map.of("TestClass.java", sourceCode);
        UMLModel model = new UMLModelASTReader(javaFileContents, Set.of("."), false).getUmlModel();
        ASTNode node = (ASTNode) cu;
        assertNotNull(node);
        LocationInfo location = new LocationInfo(cu, "", "TestClass.java", node, LocationInfo.CodeElementType.TYPE_DECLARATION);
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