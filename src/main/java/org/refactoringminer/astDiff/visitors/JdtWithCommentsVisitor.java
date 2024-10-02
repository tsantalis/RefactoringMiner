package org.refactoringminer.astDiff.visitors;

import com.github.gumtreediff.gen.jdt.JdtVisitor;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

/* Created by pourya on 2024-08-28*/
public class JdtWithCommentsVisitor extends JdtVisitor {

    private final IScanner scanner;

    public JdtWithCommentsVisitor(IScanner scanner) {
        super(scanner);
        this.scanner = scanner;
    }

    @Override
    public void endVisit(CompilationUnit node) {
        super.endVisit(node);
        for (Object o : node.getCommentList()) {
            ASTNode comment = (ASTNode) o;
            comment.accept(new CommentsVisitor());
        }
    }

    class CommentsVisitor extends ASTVisitor {
        public boolean visit(BlockComment node) {
            return visitComment(node);
        }

        public boolean visit(LineComment node) {
            return visitComment(node);
        }

        public boolean visit(Javadoc node) {
            //We have to check if the java doc is attached to any program element or not
            //The attached ones, have been already visited, and we do not want to add them twice.
            if (node.getParent() == null)
                //Then it is javadoc which is attached to any program element,
                //So it will be visited as same as the other comments but with JavaDoc label
                return visitComment(node);
            return true;
        }

        public boolean visitComment(Comment node) {
            int start = node.getStartPosition();
            int end = start + node.getLength();
            Tree parent = findMostInnerEnclosingParent(context.getRoot(), start, end);
            Tree t = context.createTree(nodeAsSymbol(node), new String(scanner.getSource(), start, end - start));
            t.setPos(start);
            t.setLength(node.getLength());
            insertChildProperly(parent, t);
            return true;
        }

        public void insertChildProperly(Tree parent, Tree newChild) {
            int position = 0;
            for (Tree child : parent.getChildren()) {
                if (child.getPos() < newChild.getPos()) {
                    position += 1;
                } else
                    break;
            }
            parent.insertChild(newChild, position);
        }

        private Tree findMostInnerEnclosingParent(Tree root, int start, int end) {
            Tree mostInnerParent = root;
            List<Tree> children = root.getChildren();

            for (Tree child : children) {
                if (child.getPos() <= start && child.getEndPos() >= end) {
                    Tree candidate = findMostInnerEnclosingParent(child, start, end);
                    if (candidate.getPos() >= mostInnerParent.getPos()
                            && candidate.getEndPos() <= mostInnerParent.getEndPos()) {
                        mostInnerParent = candidate;
                    }
                }
            }

            return mostInnerParent;
        }
    }
}