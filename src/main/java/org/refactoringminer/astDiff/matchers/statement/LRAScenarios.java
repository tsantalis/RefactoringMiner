package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;

public enum LRAScenarios {
    VARIABLE_DECLARATION_STATEMENT(
            (src, dst, LANG1, LANG2) ->  TreeUtilFunctions.areBothFromThisType(src, dst, LANG1.VARIABLE_DECLARATION_STATEMENT, LANG2.VARIABLE_DECLARATION_STATEMENT)) {
        @Override
        List<Pair<Tree, Tree>> makePairs(Tree src, Tree dst, MappingStore match, Constants LANG1, Constants LANG2) {
            List<Pair<Tree,Tree>> pairs = new ArrayList<>();
            Tree srcVarDeclFragment = TreeUtilFunctions.findChildByType(src, LANG1.VARIABLE_DECLARATION_FRAGMENT);
            if (srcVarDeclFragment == null) return pairs;
            int indexOfSrcVarDeclFragment = srcVarDeclFragment.positionInParent();

            Tree dstVarDeclFragment = TreeUtilFunctions.findChildByType(dst, LANG2.VARIABLE_DECLARATION_FRAGMENT);
            if (dstVarDeclFragment == null) return pairs;
            int indexOfDstVarDeclFragment = dstVarDeclFragment.positionInParent();

            FakeTree fake_src1 = new FakeTree();
            FakeTree fake_src2 = new FakeTree();
            FakeTree fake_dst1 = new FakeTree();
            FakeTree fake_dst2 = new FakeTree();

            makeSegments(src, indexOfSrcVarDeclFragment, fake_src1, fake_src2);
            makeSegments(dst, indexOfDstVarDeclFragment, fake_dst1, fake_dst2);
            pairs.add(new Pair<>(fake_src2, fake_dst2));
            pairs.add(new Pair<>(fake_src1, fake_dst1));
            match.addMapping(src, dst);
            return pairs;
        }
    },
    ExpressionStatementWithAssignment(
            (src, dst, LANG1, LANG2) ->  TreeUtilFunctions.areBothFromThisType(src, dst, LANG1.EXPRESSION_STATEMENT, LANG2.EXPRESSION_STATEMENT)
            //And both having first child (if they have)
            && (!src.getChildren().isEmpty() && !dst.getChildren().isEmpty())
            //and che first child be the assignment
            && (TreeUtilFunctions.areBothFromThisType(src.getChild(0), dst.getChild(0), LANG1.ASSIGNMENT, LANG2.ASSIGNMENT))
    ) {
        @Override
        List<Pair<Tree, Tree>> makePairs(Tree src, Tree dst, MappingStore match, Constants LANG1, Constants LANG2) {
            Tree srcAssignment = src.getChild(0);
            Tree dstAssignment = dst.getChild(0);
            if (srcAssignment.getChildren().size() == 3 && dstAssignment.getChildren().size() == 3) {
                match.addMapping(srcAssignment, dstAssignment);
                Tree sa2 = srcAssignment.getChild(2);
                Tree da2 = dstAssignment.getChild(2);
//                if (sa2.getType().name.equals(da2.getType().name))
//                    match.addMapping(sa2,da2);
                return List.of(
                        new Pair<>(srcAssignment.getChild(0), dstAssignment.getChild(0)), // left-hand side
                        new Pair<>(srcAssignment.getChild(1), dstAssignment.getChild(1)), // =
                        new Pair<>(sa2, da2) // right-hand side
                );
            }
            else
                return List.of(new Pair<>(src, dst));
        }
    };

	@FunctionalInterface
	interface Predicate {
		boolean test(Tree src, Tree dst, Constants LANG1, Constants LANG2);
	}
    final Predicate condition;
    abstract List<Pair<Tree,Tree>> makePairs(Tree src, Tree dst, MappingStore match, Constants LANG1, Constants LANG2);
    
    LRAScenarios(Predicate condition) {
        this.condition = condition;
    }

    private static void makeSegments(Tree src, int indexOfSrcVarDeclFragment, FakeTree fake_src1, FakeTree fake_src2) {
        for(int i = 0; i < src.getChildren().size(); i++) {
            if (i < indexOfSrcVarDeclFragment) {
                Tree child = src.getChild(i);
                fake_src1.addChild(child);
            } else {
                Tree child = src.getChild(i);
                fake_src2.addChild(child);
            }
        }
    }
}
