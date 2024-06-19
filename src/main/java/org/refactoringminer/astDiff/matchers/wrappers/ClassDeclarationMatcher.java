package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.UMLTypeParameter;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;

import static org.refactoringminer.astDiff.utils.Helpers.findPairOfType;

/* Created by pourya on 2024-05-22*/
public class ClassDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLClassBaseDiff baseClassDiff;

    public ClassDeclarationMatcher(UMLClassBaseDiff baseClassDiff) {
        this.baseClassDiff = baseClassDiff;
    }

    public ClassDeclarationMatcher(OptimizationData optimizationData, UMLClassBaseDiff baseClassDiff) {
        super(optimizationData);
        this.baseClassDiff = baseClassDiff;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processClassDeclarationMapping(srcTree,dstTree,baseClassDiff,mappingStore);
    }

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        String AST_type = Constants.TYPE_DECLARATION;
        if (classDiff.getOriginalClass().isEnum())
            AST_type = Constants.ENUM_DECLARATION;
        else if (classDiff.getOriginalClass().isAnnotation())
            AST_type = Constants.ANNOTATION_TYPE_DECLARATION;
        else if (classDiff.getOriginalClass().isRecord())
            AST_type = Constants.RECORD_DECLARATION;
        Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
        Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
        if (srcTypeDeclaration == null || dstTypeDeclaration == null) return;
        if (srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null) {
            if (
                    srcTypeDeclaration.getParent().getType().name.equals(Constants.TYPE_DECLARATION_STATEMENT)
                            &&
                            dstTypeDeclaration.getParent().getType().name.equals(Constants.TYPE_DECLARATION_STATEMENT)
            )
                mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
        }

        mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);

        String v1 = classDiff.getOriginalClass().getVisibility().toString();
        String v2 = classDiff.getNextClass().getVisibility().toString();
        Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, Constants.MODIFIER, v1);
        Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, Constants.MODIFIER, v2);
        if (tree1 != null && tree2 != null)
            mappingStore.addMappingRecursively(tree1,tree2);

        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add(Constants.SIMPLE_NAME);
        searchingTypes.add(Constants.TYPE_DECLARATION_KIND);
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
            new SameModifierMatcher(Constants.STATIC).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
            new SameModifierMatcher(Constants.FINAL).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
            new SameModifierMatcher(Constants.ABSTRACT).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);

        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        processSuperClasses(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processInterfaceToSuperclassOrOpposite(classDiff, mappingStore, srcTypeDeclaration, dstTypeDeclaration);
        new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getJavadoc(), classDiff.getNextClass().getJavadoc())
                .match(srcTree, dstTree, mappingStore);
        processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);
    }

    private static void processInterfaceToSuperclassOrOpposite(UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore, Tree srcTypeDeclaration, Tree dstTypeDeclaration) {
        if (classDiff.getImplementedInterfaceBecomesSuperclass().isPresent()) {
            org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> umlTypeUMLTypePair = classDiff.getImplementedInterfaceBecomesSuperclass().get();
            processLocationInfoProvidersRecursively(srcTypeDeclaration, dstTypeDeclaration, mappingStore,umlTypeUMLTypePair.getLeft(),umlTypeUMLTypePair.getRight());
        }
        if (classDiff.getSuperclassBecomesImplementedInterface().isPresent()) {
            org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> umlTypeUMLTypePair = classDiff.getSuperclassBecomesImplementedInterface().get();
            processLocationInfoProvidersRecursively(srcTypeDeclaration, dstTypeDeclaration, mappingStore,umlTypeUMLTypePair.getLeft(),umlTypeUMLTypePair.getRight());
        }
    }

    private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getInterfaceListDiff().getCommonInterfaces())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getInterfaceListDiff().getChangedInterfaces())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
    }

    private static void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo());
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo());
        if (srcSubTree == null || dstSubTree == null) return;
        if (srcSubTree.isIsoStructuralTo(dstSubTree))
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
    }
    private void processSuperClasses(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        UMLType srcParentUML = classDiff.getOldSuperclass();
        UMLType dstParentUML = classDiff.getNewSuperclass();
        if (srcParentUML != null && dstParentUML != null) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, srcParentUML, dstParentUML);
        }
    }
    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
    }
}
