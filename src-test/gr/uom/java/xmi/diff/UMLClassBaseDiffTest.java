package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UMLClassBaseDiffTest {
    private CompilationUnit parse(char[] sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(sourceCode);
        return  (CompilationUnit) parser.createAST(null);
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

    private UMLModel createUmlModel(String sourceCode) {
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