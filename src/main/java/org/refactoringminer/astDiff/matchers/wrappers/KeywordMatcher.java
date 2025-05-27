package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
public class KeywordMatcher implements TreeMatcher {

    private final String label;
    private final String type;

    public KeywordMatcher(String type, String label) {
        this.label = label;
        this.type = type;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        matchKeyword(srcTree,dstTree, type, label, mappingStore);
    }
    private void matchKeyword(Tree srcTree, Tree dstTree, String type, String label, ExtendedMultiMappingStore mappingStore) {
        Tree srcKeyword = TreeUtilFunctions.findChildByTypeAndLabel(srcTree, type, label);
        Tree dstKeyword = TreeUtilFunctions.findChildByTypeAndLabel(dstTree, type, label);
        if (srcKeyword != null && dstKeyword != null) {
            mappingStore.addMappingRecursively(srcKeyword, dstKeyword);
        }
    }
}
