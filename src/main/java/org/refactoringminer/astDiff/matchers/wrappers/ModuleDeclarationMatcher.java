package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

import java.util.List;

/* Created by pourya on 2024-05-22*/
public class ModuleDeclarationMatcher implements TreeMatcher {
    private final Constants LANG1;
    private final Constants LANG2;

    public ModuleDeclarationMatcher(Constants LANG1, Constants LANG2) {
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcModuleDeclaration = findModuleDeclaration(srcTree, LANG1);
        Tree dstModuleDeclaration = findModuleDeclaration(dstTree, LANG2);
        if (srcModuleDeclaration != null && dstModuleDeclaration != null) {
            mappingStore.addMapping(srcModuleDeclaration,dstModuleDeclaration);
            matchModuleNames(mappingStore, srcModuleDeclaration, dstModuleDeclaration);
        }
    }

    private static void matchModuleNames(ExtendedMultiMappingStore mappingStore, Tree srcModuleDeclaration, Tree dstModuleDeclaration) {
        // The name of the module declaration always the first child
        Tree srcName = srcModuleDeclaration.getChild(0);
        Tree dstName = dstModuleDeclaration.getChild(0);
        if (srcName != null && dstName != null) {
            if (srcName.isIsomorphicTo(dstName)) {
                mappingStore.addMappingRecursively(srcName, dstName);
            } else {
                mappingStore.addMapping(srcName, dstName);
            }
        }
    }

    private Tree findModuleDeclaration(Tree inputTree, Constants LANG) {
        String searchingType = LANG.MODULE_DECLARATION;
        if (!inputTree.getChildren().isEmpty()) {
            List<Tree> children = inputTree.getChildren();
            for(Tree child: children) {
                if (child.getType().name.equals(searchingType))
                    return child;
            }
        }
        return null;
    }
}
