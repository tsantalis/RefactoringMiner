package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

import java.util.List;

/* Created by pourya on 2024-05-22*/
public class ModuleDeclarationMatcher implements TreeMatcher {

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcModuleDeclaration = findModuleDeclaration(srcTree);
        Tree dstModuleDeclaration = findModuleDeclaration(dstTree);
        if (srcModuleDeclaration != null && dstModuleDeclaration != null) {
            mappingStore.addMappingRecursively(srcModuleDeclaration,dstModuleDeclaration);
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

    private Tree findModuleDeclaration(Tree inputTree) {
        String searchingType = Constants.MODULE_DECLARATION;
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
