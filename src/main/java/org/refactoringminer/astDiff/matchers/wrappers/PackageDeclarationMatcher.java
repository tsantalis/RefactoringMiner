package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

import java.util.List;

/* Created by pourya on 2024-05-22*/
public class PackageDeclarationMatcher implements TreeMatcher {
    private final Constants LANG1;
    private final Constants LANG2;
    public PackageDeclarationMatcher(Constants LANG1, Constants LANG2) {
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }
    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcPackageDeclaration = findPackageDeclaration(srcTree, LANG1);
        Tree dstPackageDeclaration = findPackageDeclaration(dstTree, LANG2);
        if (srcPackageDeclaration != null && dstPackageDeclaration != null) {
            mappingStore.addMappingRecursively(srcPackageDeclaration,dstPackageDeclaration);
            if(Constants.isCrossLanguage(LANG1, LANG2)) {
                JavaToKotlinMigration.handlePackageDeclarationMapping(mappingStore, srcPackageDeclaration, dstPackageDeclaration, LANG1, LANG2);
            }
        }
    }

    private Tree findPackageDeclaration(Tree inputTree, Constants LANG) {
        String searchingType = LANG.PACKAGE_DECLARATION;
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
