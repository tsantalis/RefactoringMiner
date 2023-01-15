package org.refactoringminer.test;

import com.github.gumtreediff.tree.TreeContext;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import gr.uom.java.xmi.PsiFactoryManager;
import gr.uom.java.xmi.PsiVisitor;
import org.junit.Test;

public class PsiVisitorTest extends LightJavaCodeInsightFixtureTestCase {

    @Test
    public void testTreeContext() {
        String code = "public class A {\n" +
                "    public String testField = \"test\";\n" +
                "    private int testMethod() {\n" +
                "        return 1 + 2;\n" +
                "    }\n" +
                "}";

        String codeAsTreeString = "PsiJavaFileImpl [0,115]\n" +
                "    PsiClassImpl [0,115]\n" +
                "        PsiKeywordImpl: public [0,6]\n" +
                "        PsiKeywordImpl: class [7,12]\n" +
                "        PsiIdentifierImpl: A [13,14]\n" +
                "        PsiFieldImpl [21,54]\n" +
                "            PsiKeywordImpl: public [21,27]\n" +
                "            PsiTypeElementImpl [28,34]\n" +
                "                PsiJavaCodeReferenceElementImpl [28,34]\n" +
                "                    PsiIdentifierImpl: String [28,34]\n" +
                "            PsiIdentifierImpl: testField [35,44]\n" +
                "            PsiLiteralExpressionImpl: \"test\" [47,53]\n" +
                "        PsiMethodImpl [59,113]\n" +
                "            PsiKeywordImpl: private [59,66]\n" +
                "            PsiTypeElementImpl [67,70]\n" +
                "                PsiKeywordImpl: int [67,70]\n" +
                "            PsiIdentifierImpl: testMethod [71,81]\n" +
                "            PsiParameterListImpl [81,83]\n" +
                "            PsiCodeBlockImpl [84,113]\n" +
                "                PsiReturnStatementImpl [94,107]\n" +
                "                    PsiKeywordImpl: return [94,100]\n" +
                "                    PsiBinaryExpressionImpl [101,106]\n" +
                "                        PsiLiteralExpressionImpl: 1 [101,102]\n" +
                "                        PsiLiteralExpressionImpl: 2 [105,106]";

        PsiFile psiFile = PsiFactoryManager.getFactory().createFileFromText(JavaLanguage.INSTANCE, code);
        PsiVisitor visitor = new PsiVisitor();
        psiFile.accept(visitor);
        TreeContext treeContext = visitor.getTreeContext();

        assertEquals(codeAsTreeString, treeContext.getRoot().toTreeString());
    }
}
