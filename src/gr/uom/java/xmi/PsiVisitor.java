package gr.uom.java.xmi;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TypeSet;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiModifierListImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Collectors;

import static com.intellij.util.ArrayUtil.getLastElement;

public class PsiVisitor extends PsiRecursiveElementWalkingVisitor {
    private final TreeContext context;
    private final Deque<Tree> trees;

    public PsiVisitor() {
        this.context = new TreeContext();
        this.trees = new ArrayDeque<>();
    }

    public TreeContext getTreeContext() {
        return context;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        process(element);
        super.visitElement(element);
    }

    @Override
    protected void elementFinished(PsiElement element) {
        if (!skipElement(element) && trees.size() > 0) {
            trees.pop();
        }
    }

    private boolean skipElement(PsiElement node) {
        return node instanceof PsiWhiteSpace ||
                node instanceof PsiModifierListImpl ||
                node instanceof PsiImportList ||
                node instanceof PsiTypeParameterList ||
                node instanceof PsiReferenceList ||
                node instanceof PsiReferenceParameterList ||
                node instanceof PsiExpressionList ||
                (node instanceof PsiJavaToken && !(node instanceof PsiIdentifier || node instanceof PsiKeyword));
    }

    private void process(PsiElement node) {
        if (skipElement(node)) {
            return;
        }

        String label = "";
        if (node instanceof PsiIdentifier) {
            label = node.getText();
        } else if (node instanceof PsiLiteralExpression) {
            label = node.getText();
        } else if (node instanceof PsiKeyword) {
            label = node.getText();
        }

        TextRange textRange = node.getTextRange();
        push(node, nodeAsSymbol(node), label, textRange.getStartOffset(), textRange.getLength());
    }

    private Type nodeAsSymbol(PsiElement n) {
        return TypeSet.type(n.getClass().getSimpleName());
    }

    private void push(PsiElement element, Type type, String label, int startPosition, int length) {
        Tree tree = context.createTree(type, label);
        tree.setPos(startPosition);
        tree.setLength(length);

        if (trees.isEmpty()) {
            context.setRoot(tree);
        } else {
            Tree parent = trees.peek();
            tree.setParentAndUpdateChildren(parent);
        }

        if (element instanceof PsiClass) {
            tree.setMetadata("id", "Type " + ((PsiClass) element).getName());
        } else if (element instanceof PsiMethod) {
            tree.setMetadata("id", "Method " + ((PsiMethod) element).getName() + "(" +
                    Arrays.stream(((PsiMethod) element).getParameters())
                            .map(param -> getLastElement(param.getType().toString().split(":")))
                            .collect(Collectors.joining(" ")) + ")");
        } else if (element instanceof PsiField) {
            tree.setMetadata("id", "Field " + ((PsiField) element).getName());
        }

        trees.push(tree);
    }
}
