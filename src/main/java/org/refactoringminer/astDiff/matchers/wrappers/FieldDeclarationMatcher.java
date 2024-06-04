package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-05-22*/
public class FieldDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {


    private final UMLAttribute originalAttribute;
    private final UMLAttribute movedAttribute;

    public FieldDeclarationMatcher(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
    }

    public FieldDeclarationMatcher(OptimizationData optimizationData, UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
        super(optimizationData);
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processFieldDeclaration(srcTree, dstTree, originalAttribute, movedAttribute, mappingStore);
    }
    private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {

        Tree srcAttr = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo());
        Tree dstAttr = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo());
        if (srcAttr == null || dstAttr == null) return;
        Tree srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.FIELD_DECLARATION);
        Tree dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.FIELD_DECLARATION);
        if (srcFieldDeclaration == null) {
            srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.ENUM_CONSTANT_DECLARATION);
        }
        if (dstFieldDeclaration == null) {
            dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.ENUM_CONSTANT_DECLARATION);
        }
        //handle Record Components (SingleVariableDeclaration)
        if (srcFieldDeclaration == null) {
            srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.RECORD_COMPONENT);
        }
        if (dstFieldDeclaration == null) {
            dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.RECORD_COMPONENT);
        }
        //				|| srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
        if (srcFieldDeclaration != null && dstFieldDeclaration != null && srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash) {
            //IsoStructural can't be a good idea here, i.e. anonymous class
            mappingStore.addMappingRecursively(srcFieldDeclaration, dstFieldDeclaration);
            return;
        }
        if (srcAttr.getMetrics().hash == dstAttr.getMetrics().hash) {
            mappingStore.addMappingRecursively(srcAttr,dstAttr);
        }

        mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
        matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
        matchFieldAnnotations(srcFieldDeclaration, dstFieldDeclaration, mappingStore);
        if (srcUMLAttribute.getType().getLocationInfo() == null || dstUMLAttribute.getType().getLocationInfo() == null) {
            if (srcUMLAttribute instanceof UMLEnumConstant && dstUMLAttribute instanceof UMLEnumConstant) {
                //JavaDocs are mapped as well.
                new LeafMatcher().match(srcAttr,dstAttr,mappingStore);
                return;
            }
        }
        Tree srcType = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo());
        Tree dstType = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo());

        if (srcType != null && dstType != null && srcType.isIsoStructuralTo(dstType)) mappingStore.addMappingRecursively(srcType,dstType);
        else {
            new LeafMatcher().match(srcType,dstType,mappingStore);
        }
        Tree srcVarDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo());
        Tree dstVarDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo());
        mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
        new LeafMatcher().match(srcVarDeclaration,dstVarDeclaration,mappingStore);
        new JavaDocMatcher(optimizationData, srcUMLAttribute.getJavadoc(), dstUMLAttribute.getJavadoc())
                .match(srcTree, dstTree, mappingStore);
        if (srcVarDeclaration != null && dstVarDeclaration != null)
            mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));
    }

    private void matchFieldAllModifiers(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {
        matchModifiersForField(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute.getVisibility().toString(),dstUMLAttribute.getVisibility().toString(),mappingStore);
        if (srcUMLAttribute.isFinal() && dstUMLAttribute.isFinal())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.FINAL,mappingStore);
        if (srcUMLAttribute.isVolatile() && dstUMLAttribute.isVolatile())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.VOLATILE,mappingStore);
        if (srcUMLAttribute.isStatic() && dstUMLAttribute.isStatic())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.STATIC,mappingStore);
        if (srcUMLAttribute.isTransient() && dstUMLAttribute.isTransient())
            matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.TRANSIENT,mappingStore);
    }

    private void matchFieldAnnotations(Tree srcFieldDeclaration, Tree dstFieldDeclaration, ExtendedMultiMappingStore mappingStore) {
        Tree srcField = TreeUtilFunctions.findFirstByType(srcFieldDeclaration, Constants.MARKER_ANNOTATION);
        Tree dstField = TreeUtilFunctions.findFirstByType(dstFieldDeclaration, Constants.MARKER_ANNOTATION);
        new LeafMatcher().match(srcField, dstField, mappingStore);
    }

    private void matchModifiersForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String srcModifier, String dstModifier, ExtendedMultiMappingStore mappingStore) {
        Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, srcModifier);
        Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, dstModifier);
        if (srcModifierTree != null && dstModifierTree != null)
            mappingStore.addMapping(srcModifierTree,dstModifierTree);
    }

    private void matchModifierForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
        Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, modifier);
        Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, modifier);
        if (srcModifierTree != null && dstModifierTree != null)
            mappingStore.addMapping(srcModifierTree,dstModifierTree);
    }

    private Tree findAttributeModifierByLabel(Tree anyFieldDeclaration,String label) {
        if (!anyFieldDeclaration.getChildren().isEmpty()) {
            for (Tree child : anyFieldDeclaration.getChildren()) {
                if (child.getLabel().equals(label))
                    return child;
                if (child.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT))
                    break;
            }
        }
        return null;
    }
}
