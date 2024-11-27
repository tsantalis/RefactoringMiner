package org.refactoringminer.astDiff.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.refactoringminer.astDiff.utils.UtilMethods.getRefactoringMinerParsedTree;

/* Created by pourya on 2024-11-26*/
public class TreeFromParserTest {
    @Test
    public void testRecordWithAMethodWithinAClass(){
        String myCode = """
            class C {
                record R(int a) {
                    void f() { System.out.println(a); }
                }
            }""";
        String treeAsString = getRefactoringMinerParsedTree(myCode).toTreeString();
        String expected  = """
                CompilationUnit [0,83]
                    TypeDeclaration [0,83]
                        TYPE_DECLARATION_KIND: class [0,5]
                        SimpleName: C [6,7]
                        RecordDeclaration [14,81]
                            SimpleName: R [21,22]
                            SingleVariableDeclaration [23,28]
                                PrimitiveType: int [23,26]
                                SimpleName: a [27,28]
                            MethodDeclaration [40,75]
                                PrimitiveType: void [40,44]
                                SimpleName: f [45,46]
                                Block [49,75]
                                    ExpressionStatement [51,73]
                                        MethodInvocation [51,72]
                                            METHOD_INVOCATION_RECEIVER [51,61]
                                                QualifiedName: System.out [51,61]
                                            SimpleName: println [62,69]
                                            METHOD_INVOCATION_ARGUMENTS [70,71]
                                                SimpleName: a [70,71]""";
        Assertions.assertEquals(expected, treeAsString);
    }
}
