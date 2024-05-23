package org.refactoringminer.astDiff.matchers.atomic;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.Constants;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;

import java.util.List;

/* Created by pourya on 2024-05-22*/
public class PackageDeclarationMatcher implements IExtendedMatcher {

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcPackageDeclaration = findPackageDeclaration(srcTree);
        Tree dstPackageDeclaration = findPackageDeclaration(dstTree);
        if (srcPackageDeclaration != null && dstPackageDeclaration != null)
            mappingStore.addMappingRecursively(srcPackageDeclaration,dstPackageDeclaration);
    }
    private Tree findPackageDeclaration(Tree inputTree) {
        String searchingType = Constants.PACKAGE_DECLARATION;
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
