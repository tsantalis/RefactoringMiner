package gr.uom.java.xmi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiUtil;
import gr.uom.java.xmi.decomposition.PsiUtils;
import org.jetbrains.annotations.NotNull;

public class TypeUtils {
    public static UMLType extractType(PsiFile file, String filePath, PsiNewExpression newExpression) {
        PsiJavaCodeReferenceElement typeElement = newExpression.getClassReference();
        if (typeElement != null) {
            return UMLTypePsiParser.extractTypeObject(file, filePath, typeElement, newExpression.getType());
        } else {
            PsiElement qualifyingElement = PsiUtils.findFirstForwardSibling(newExpression.getFirstChild(),
                element -> element instanceof PsiAnonymousClass || PsiUtils.isTypeKeyword(element));
            // No more cases expected
            assert qualifyingElement != null;
            if (qualifyingElement instanceof PsiKeyword) {
                // array of primitives
                return UMLTypePsiParser.extractTypeObject(file, filePath, (PsiKeyword) qualifyingElement);
            } else {
                // anonymous class
                return extractType(file, filePath, (PsiAnonymousClass) qualifyingElement);
            }
        }
    }

    @NotNull
    public static UMLType extractType(PsiFile file, String filePath, PsiAnonymousClass anonymousClass) {
        return UMLTypePsiParser.extractTypeObject(file, filePath, anonymousClass.getBaseClassReference());
    }

    public static UMLType extractType(PsiFile file, String filePath, PsiVariable variable) {
        PsiTypeElement typeElement = variable.getTypeElement();
        if (typeElement.isInferredType()) {
            return UMLTypePsiParser.extractVarType(file, filePath, typeElement);
        } else {
            return UMLTypePsiParser.extractTypeObject(file, filePath, typeElement, variable.getType());
        }
    }

    public static int arrayDimensions(PsiElement element) {
        int arrayDimensions = 0;
        PsiElement next = element.getNextSibling();
        while (next != null) {
            if (PsiUtil.isJavaToken(next, JavaTokenType.LBRACKET)) {
                arrayDimensions++;
            }
            next = next.getNextSibling();
        }
        return arrayDimensions;
    }

    public static String clearArrayLength(String typeString) {
        StringBuilder sb = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < typeString.length(); i++) {
            switch (typeString.charAt(i)) {
                case '[': {
                    if (sum == 0) {
                        sb.append('[');
                    }
                    sum++;
                }
                break;
                case ']': {
                    sum--;
                    if (sum == 0) {
                        sb.append(']');
                    }
                }
                break;
                default:
                    if (sum == 0) {
                        sb.append(typeString.charAt(i));
                    }
            }
        }
        return sb.toString();
    }
}
