package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
public class KeywordMatcher implements TreeMatcher {
    private final Constants LANG1;
    private final Constants LANG2;
    private final String label;
    private final String type1;
    private final String type2;

    public KeywordMatcher(Constants LANG1, Constants LANG2, String type1, String type2, String label) {
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
        this.label = label;
        this.type1 = type1;
        this.type2 = type2;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        matchKeyword(srcTree,dstTree, type1, type2, label, mappingStore);
    }
    private void matchKeyword(Tree srcTree, Tree dstTree, String type1, String type2, String label, ExtendedMultiMappingStore mappingStore) {
        Tree srcKeyword = TreeUtilFunctions.findChildByTypeAndLabel(srcTree, type1, label, LANG1);
        Tree dstKeyword = TreeUtilFunctions.findChildByTypeAndLabel(dstTree, type2, label, LANG2);
        if (srcKeyword != null && dstKeyword != null) {
            mappingStore.addMappingRecursively(srcKeyword, dstKeyword);
        }
    }
}
