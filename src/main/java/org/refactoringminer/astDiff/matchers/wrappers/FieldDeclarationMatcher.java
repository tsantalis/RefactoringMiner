package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.refactoringminer.util.PathFileUtils;

import java.util.List;
import java.util.Optional;

/* Created by pourya on 2024-05-22*/
public class FieldDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {


    private final UMLAttribute originalAttribute;
    private final UMLAttribute movedAttribute;
    private final Optional<UMLJavadocDiff> umlJavadocDiff;
    private final UMLCommentListDiff umlCommentListDiff;
    private final Constants LANG1;
    private final Constants LANG2;

    public FieldDeclarationMatcher(UMLAttribute originalAttribute, UMLAttribute movedAttribute, Optional<UMLJavadocDiff> umlJavadocDiff, UMLCommentListDiff umlCommentListDiff, Constants LANG1, Constants LANG2) {
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
        this.umlJavadocDiff = umlJavadocDiff;
        this.umlCommentListDiff = umlCommentListDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public FieldDeclarationMatcher(OptimizationData optimizationData, UMLAttribute originalAttribute, UMLAttribute movedAttribute, Optional<UMLJavadocDiff> umlJavadocDiff, UMLCommentListDiff umlCommentListDiff, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
        this.umlJavadocDiff = umlJavadocDiff;
        this.umlCommentListDiff = umlCommentListDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processFieldDeclaration(srcTree, dstTree, originalAttribute, movedAttribute, mappingStore);
    }
    private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {

        Tree srcAttr = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo(), LANG1);
        Tree dstAttr = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo(), LANG2);
        if (srcAttr == null || dstAttr == null) return;
        Tree srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, LANG1.FIELD_DECLARATION);
        Tree dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, LANG2.FIELD_DECLARATION);
        if (srcFieldDeclaration == null) {
            srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, LANG1.ENUM_CONSTANT_DECLARATION);
        }
        if (dstFieldDeclaration == null) {
            dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, LANG2.ENUM_CONSTANT_DECLARATION);
        }
        //handle Record Components (SingleVariableDeclaration)
        if (srcFieldDeclaration == null) {
            srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, LANG1.RECORD_COMPONENT);
        }
        if (dstFieldDeclaration == null) {
            dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, LANG2.RECORD_COMPONENT);
        }
        if (srcFieldDeclaration == null || srcFieldDeclaration.getType().name.endsWith("_comment")) {
            srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo(), LANG1, LANG1.FIELD_DECLARATION);
        }
        if (dstFieldDeclaration == null || dstFieldDeclaration.getType().name.endsWith("_comment")) {
            dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo(), LANG2, LANG2.FIELD_DECLARATION);
        }
        if (srcFieldDeclaration == null || srcFieldDeclaration.getType().name.endsWith("_comment")) {
            srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo(), LANG1, LANG1.CLASS_PARAMETER);
        }
        if (dstFieldDeclaration == null || dstFieldDeclaration.getType().name.endsWith("_comment")) {
            dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo(), LANG2, LANG2.CLASS_PARAMETER);
        }
        new CommentMatcher(optimizationData, umlCommentListDiff, LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        if (srcFieldDeclaration != null && dstFieldDeclaration != null && srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash) {
            //IsoStructural can't be a good idea here, i.e. anonymous class
            mappingStore.addMappingRecursively(srcFieldDeclaration, dstFieldDeclaration);
            return;
        }
        if (srcAttr.getMetrics().hash == dstAttr.getMetrics().hash) {
            mappingStore.addMappingRecursively(srcAttr,dstAttr);
        }

        if(srcFieldDeclaration != null && dstFieldDeclaration != null && PathFileUtils.isKotlinFile(srcUMLAttribute.getLocationInfo().getFilePath())) {
            mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
        }
        else if(Constants.isCrossLanguage(LANG1, LANG2)) {
            JavaToKotlinMigration.handleFieldDeclarationMapping(mappingStore, srcAttr, dstAttr, srcFieldDeclaration, dstFieldDeclaration, LANG1, LANG2);
        }
        mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
        matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
        matchFieldAnnotations(srcFieldDeclaration, dstFieldDeclaration, mappingStore);
        if (srcUMLAttribute.getType().getLocationInfo() == null || dstUMLAttribute.getType().getLocationInfo() == null) {
            if (srcUMLAttribute instanceof UMLEnumConstant && dstUMLAttribute instanceof UMLEnumConstant) {
                //JavaDocs are mapped as well.
                new LeafMatcher(LANG1, LANG2).match(srcAttr,dstAttr,mappingStore);
                new JavaDocMatcher(optimizationData, srcUMLAttribute.getJavadoc(), dstUMLAttribute.getJavadoc(), umlJavadocDiff, LANG1, LANG2)
                .match(srcTree, dstTree, mappingStore);
                return;
            }
        }
        if(!Constants.isCrossLanguage(LANG1, LANG2)) {
            Tree srcType = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo(),LANG1);
            Tree dstType = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo(),LANG2);

            if (srcType != null && dstType != null && srcType.isIsoStructuralTo(dstType)) {
                mappingStore.addMappingRecursively(srcType,dstType);
            }
            else {
                new LeafMatcher(LANG1, LANG2).match(srcType,dstType,mappingStore);
            }
            Tree srcVarDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo(),LANG1);
            Tree dstVarDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo(),LANG2);
            mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
            new LeafMatcher(LANG1, LANG2).match(srcVarDeclaration,dstVarDeclaration,mappingStore);
            new JavaDocMatcher(optimizationData, srcUMLAttribute.getJavadoc(), dstUMLAttribute.getJavadoc(), umlJavadocDiff, LANG1, LANG2)
                    .match(srcTree, dstTree, mappingStore);
            if (srcVarDeclaration != null && dstVarDeclaration != null)
                if (!srcVarDeclaration.getChildren().isEmpty() && !dstVarDeclaration.getChildren().isEmpty())
                    mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));
        }
    }

    private void matchFieldAllModifiers(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {
        if (srcFieldDeclaration == null || dstFieldDeclaration == null) return;
        matchModifiersForField(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute.getVisibility().toString(),dstUMLAttribute.getVisibility().toString(),mappingStore);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isFinal())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,LANG1.FINAL,mappingStore);
        if (srcUMLAttribute.isVolatile() && dstUMLAttribute.isVolatile())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,LANG1.VOLATILE,mappingStore);
        if (srcUMLAttribute.isStatic() && dstUMLAttribute.isStatic())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,LANG1.STATIC,mappingStore);
        if (srcUMLAttribute.isTransient() && dstUMLAttribute.isTransient())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,LANG1.TRANSIENT,mappingStore);
    }

    private void matchFieldAnnotations(Tree srcFieldDeclaration, Tree dstFieldDeclaration, ExtendedMultiMappingStore mappingStore) {
        //TODO: add test for all the annotations
        Pair<Tree, Tree> srcAndDst = TreeUtilFunctions.populateLeftAndRightBasedOnTheFirstChildOfType(
                srcFieldDeclaration, dstFieldDeclaration,
                List.of(
                        Pair.of(LANG1.MARKER_ANNOTATION, LANG2.MARKER_ANNOTATION),
                        Pair.of(LANG1.SINGLE_MEMBER_ANNOTATION, LANG2.SINGLE_MEMBER_ANNOTATION),
                        Pair.of(LANG1.NORMAL_ANNOTATION, LANG2.NORMAL_ANNOTATION)
                )
        );
        new LeafMatcher(LANG1, LANG2).match(srcAndDst.getLeft(), srcAndDst.getRight(), mappingStore);
    }

    private void matchModifiersForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String srcModifier, String dstModifier, ExtendedMultiMappingStore mappingStore) {
        Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, srcModifier, LANG1);
        if(srcModifierTree == null) {
            srcModifierTree = TreeUtilFunctions.findChildByTypeAndLabel(srcFieldDeclaration, LANG1.MODIFIER, srcModifier, LANG1);
        }
        Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, dstModifier, LANG2);
        if(dstModifierTree == null) {
            dstModifierTree = TreeUtilFunctions.findChildByTypeAndLabel(dstFieldDeclaration, LANG2.MODIFIER, dstModifier, LANG2);
        }
        if (srcModifierTree != null && dstModifierTree != null) {
            if(Constants.isCrossLanguage(LANG1, LANG2)) {
                JavaToKotlinMigration.handleModifierMapping(mappingStore, srcModifierTree, dstModifierTree, LANG1, LANG2);
            }
            else {
                mappingStore.addMapping(srcModifierTree,dstModifierTree);
            }
        }
    }

    private void matchModifierForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
        Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, modifier, LANG1);
        Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, modifier, LANG2);
        if (srcModifierTree != null && dstModifierTree != null)
            mappingStore.addMapping(srcModifierTree,dstModifierTree);
    }

    private Tree findAttributeModifierByLabel(Tree anyFieldDeclaration,String label, Constants LANG) {
        if (!anyFieldDeclaration.getChildren().isEmpty()) {
            for (Tree child : anyFieldDeclaration.getChildren()) {
                if (child.getLabel().equals(label))
                    return child;
                if (child.getType().name.equals(LANG.VARIABLE_DECLARATION_FRAGMENT))
                    break;
            }
        }
        return null;
    }
}
