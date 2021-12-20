package gr.uom.java.xmi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor for building string representation of PsiElement
 * 1. Without commentaries
 * 2. With formatting similar to Eclipse format
 */
public class FormattingVisitor extends PsiRecursiveElementWalkingVisitor {
    private static final TokenSet noSpaces = TokenSet.create(
        JavaTokenType.EQ, JavaTokenType.LT, JavaTokenType.GT,
        JavaTokenType.DOT, JavaTokenType.COMMA,
        JavaTokenType.RBRACKET, JavaTokenType.LBRACKET,
        JavaTokenType.LPARENTH, JavaTokenType.RPARENTH,
        JavaTokenType.SEMICOLON, JavaTokenType.DOUBLE_COLON
    );
    private static final TokenSet noSpaceAfter = TokenSet.create(
        JavaTokenType.AT, JavaTokenType.LBRACE, JavaTokenType.RBRACE
    );
    private static final TokenSet noSpaceBefore = TokenSet.create(
        JavaTokenType.ELLIPSIS, JavaTokenType.RBRACE
    );
    private static final TokenSet endLineAfter = TokenSet.create(
        //JavaTokenType.LBRACE, JavaTokenType.RBRACE,
            JavaTokenType.SEMICOLON
    );

    private final StringBuilder sb = new StringBuilder();
    private boolean previousNeedSpaceAfter = false;

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element.getFirstChild() == null) {
            // Add text from leaves. Excluding comments and user-specific whitespaces
            if (!(element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
                String text = element.getText();
                if (!text.isEmpty()) {
                    if (needSpaceBefore(element) && previousNeedSpaceAfter) {
                        sb.append(' ');
                    }
                    sb.append(element.getText());
                    if (needEndLineAfter(element)) {
                        sb.append('\n');
                    }
                    previousNeedSpaceAfter = needSpaceAfter(element);
                }
            }
        } else {
            super.visitElement(element);
        }
    }

    private boolean needEndLineAfter(@NotNull PsiElement element) {
        return PsiUtil.isJavaToken(element, endLineAfter);
    }

    private static boolean needSpaceBefore(PsiElement element) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element, noSpaceBefore));
    }

    private static boolean needSpaceAfter(PsiElement element) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element, noSpaceAfter));
    }

    public String getText() {
        return sb.toString();
    }
}
