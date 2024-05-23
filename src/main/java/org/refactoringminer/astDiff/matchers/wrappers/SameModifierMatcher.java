package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-05-22*/
public class SameModifierMatcher implements TreeMatcher {

    private final String modifier;

    public SameModifierMatcher(String modifier) {
        this.modifier = modifier;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        matchModifier(srcTree,dstTree, modifier, mappingStore);
    }
    public static void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
        String type = Constants.MODIFIER;
        Tree srcTree = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration,type,modifier);
        Tree dstTree = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration,type,modifier);
        if (srcTree != null && dstTree != null){
            mappingStore.addMapping(srcTree,dstTree);
        }
    }
}
