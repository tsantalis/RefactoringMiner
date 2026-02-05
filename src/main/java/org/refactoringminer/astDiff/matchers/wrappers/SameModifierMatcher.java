package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-05-22*/
public class SameModifierMatcher implements TreeMatcher {

    private final String modifier;
    private final Constants LANG1;
    private final Constants LANG2;

    public SameModifierMatcher(Constants LANG1, Constants LANG2, String modifier) {
        this.modifier = modifier;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        matchModifier(srcTree,dstTree, modifier, mappingStore);
    }
    public void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
        Tree srcTree = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration,LANG1.MODIFIER,modifier,LANG1);
        Tree dstTree = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration,LANG2.MODIFIER,modifier,LANG2);
        if (srcTree != null && dstTree != null){
            mappingStore.addMappingRecursively(srcTree,dstTree);
        }
    }
}
