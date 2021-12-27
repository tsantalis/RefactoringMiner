package gr.uom.java.xmi;

import com.intellij.psi.*;
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
        JavaTokenType.AT, JavaTokenType.LBRACE, JavaTokenType.RBRACE, JavaTokenType.EXCL
    );
    private static final TokenSet noSpaceBefore = TokenSet.create(
            JavaTokenType.ELLIPSIS
    );
    private static final TokenSet endLineAfter = TokenSet.create(
            JavaTokenType.LBRACE, JavaTokenType.RBRACE, JavaTokenType.SEMICOLON
    );
    private static final TokenSet mustSpaceBefore = TokenSet.create(
            JavaTokenType.OROR, JavaTokenType.ANDAND, JavaTokenType.EQEQ, JavaTokenType.PLUS
    );
    private static final TokenSet annotationNoSpaceBefore = TokenSet.create(
        JavaTokenType.ELLIPSIS, JavaTokenType.RBRACE
    );
    private static final TokenSet annotationEndLineAfter = TokenSet.create(
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
                    if ((needSpaceBefore(element) && previousNeedSpaceAfter) || mustHaveSpaceBefore(element)) {
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

    private static boolean needEndLineAfter(@NotNull PsiElement element) {
        return PsiUtil.isJavaToken(element,
                findParentAnnotation(element) != null ? annotationEndLineAfter : endLineAfter);
    }

    private static boolean needSpaceBefore(PsiElement element) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element,
                findParentAnnotation(element) != null ? annotationNoSpaceBefore : noSpaceBefore));
    }

    private static boolean needSpaceAfter(PsiElement element) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element, noSpaceAfter));
    }

    private static boolean mustHaveSpaceBefore(PsiElement element) {
        return PsiUtil.isJavaToken(element, mustSpaceBefore);
    }

    private static PsiElement findParentAnnotation(PsiElement node) {
        PsiElement parent = node.getParent();
        while(parent != null) {
            if(parent instanceof PsiAnnotation || parent instanceof PsiArrayInitializerExpression) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public String getText() {
        return sb.toString();
    }
}
