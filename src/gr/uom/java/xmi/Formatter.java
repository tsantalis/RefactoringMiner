package gr.uom.java.xmi;

import com.intellij.psi.PsiElement;

public class Formatter {

    public static String format(PsiElement element) {
        if (element.getFirstChild() == null) {
            // Optimize for leaves
            return element.getText();
        }
        FormattingVisitor formatter = new FormattingVisitor();
        element.accept(formatter);
        return formatter.getText();
    }

    /**
     * Formats elements between siblings.
     * From begin inclusively to the end exclusively.
     */
    public static String format(PsiElement beginSibling, PsiElement endSibling) {
        FormattingVisitor formatter = new FormattingVisitor();
        do {
            beginSibling.accept(formatter);
            beginSibling = beginSibling.getNextSibling();
        } while (beginSibling != endSibling);
        return formatter.getText();
    }
}
