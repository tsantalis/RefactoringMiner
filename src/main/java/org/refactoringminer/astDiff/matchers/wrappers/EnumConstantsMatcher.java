package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.Optional;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class EnumConstantsMatcher extends OptimizationAwareMatcher {

    private final Set<Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants;

    public EnumConstantsMatcher(OptimizationData optimizationData, Set<Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants) {
        super(optimizationData);
        this.commonEnumConstants = commonEnumConstants;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processEnumConstants(srcTree,dstTree,commonEnumConstants,mappingStore);

    }


    private void processEnumConstants(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant> commonEnumConstant : commonEnumConstants) {
            LocationInfo locationInfo1 = commonEnumConstant.getLeft().getLocationInfo();
            LocationInfo locationInfo2 = commonEnumConstant.getRight().getLocationInfo();
            Tree srcEnumConstant = TreeUtilFunctions.findByLocationInfo(srcTree,locationInfo1);
            Tree dstEnumConstant = TreeUtilFunctions.findByLocationInfo(dstTree,locationInfo2);
            new LeafMatcher().match(srcEnumConstant,dstEnumConstant,mappingStore);
            new FieldDeclarationMatcher(
                    optimizationData,
                    commonEnumConstant.getLeft(), commonEnumConstant.getRight(),
                    (commonEnumConstant.getLeft().getJavadoc() != null && commonEnumConstant.getRight().getJavadoc() != null) ?
                            Optional.of(new UMLJavadocDiff(commonEnumConstant.getLeft().getJavadoc(), commonEnumConstant.getRight().getJavadoc()))
                            : Optional.empty(),
                    new UMLCommentListDiff(commonEnumConstant.getLeft().getComments(), commonEnumConstant.getRight().getComments()))
                    .match(srcEnumConstant,dstEnumConstant,mappingStore);
        }
    }
}
