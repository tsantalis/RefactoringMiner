package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
    void testCheckForTestParameterizations(String newSourceCode) throws RefactoringMinerTimedOutException {
        String originalSourceCode = """
                    public class TestClass {
                        @Test
                        public void testMethod() {
                            assertEquals(null, null);
                        }
                    }
                """;
        CompilationUnit originalCu = parse(originalSourceCode.toCharArray());
        Map<String, String> originalJavaFileContents = Map.of("TestClass.java", originalSourceCode);
        UMLModel originalModel = new UMLModelASTReader(originalJavaFileContents, Set.of("."), false).getUmlModel();
        ASTNode originalNode = (ASTNode) originalCu;
        assertNotNull(originalNode);
        LocationInfo originalLocation = new LocationInfo(originalCu, "TestClass.java", originalNode, LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass originalClass = new UMLClass("org.refactoringminer.test", "TestClass", originalLocation, true, Collections.emptyList());
        originalClass.setVisibility(Visibility.PUBLIC);
        Map<String, String> newJavaFileContents = Map.of("TestClass.java", newSourceCode);
        UMLModel newModel = new UMLModelASTReader(newJavaFileContents, Set.of("."), false).getUmlModel();
        CompilationUnit newCu = parse(newSourceCode.toCharArray());
        ASTNode newNode = (ASTNode) newCu;
        assertNotNull(newNode);
        LocationInfo newLocation = new LocationInfo(newCu, "TestClass.java", newNode, LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass newClass = new UMLClass("org.refactoringminer.test", "TestClass", newLocation, true, Collections.emptyList());
        newClass.setVisibility(Visibility.PUBLIC);
        UMLModelDiff diff = originalModel.diff(newModel);
        UMLClassBaseDiff umlClassBaseDiff = diff.getUMLClassDiff("TestClass");
        assertEquals("{\"value\",\"value2\"}", umlClassBaseDiff.addedParameter.getExpression());
        assertEquals(Set.of("value","value2"),umlClassBaseDiff.newValue);
    }
}