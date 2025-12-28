package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.UMLTypeParameter;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.refactoringminer.astDiff.utils.Helpers.findPairOfType;

/* Created by pourya on 2024-05-22*/
public class ClassDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLClassBaseDiff baseClassDiff;
    private final String IMPLEMENTS_KEYWORD_LABEL = "implements";
    private final String EXTENDS_KEYWORD_LABEL = "extends";
    private final String PERMITS_KEYWORD_LABEL = "permits";

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
        String AST_type = Constants.get().TYPE_DECLARATION;
        if (classDiff.getOriginalClass().isEnum())
            AST_type = Constants.get().ENUM_DECLARATION;
        else if (classDiff.getOriginalClass().isAnnotation())
            AST_type = Constants.get().ANNOTATION_TYPE_DECLARATION;
        else if (classDiff.getOriginalClass().isRecord())
            AST_type = Constants.get().RECORD_DECLARATION;
        else if (classDiff.getOriginalClass().isModule())
        	AST_type = Constants.get().MODULE;
        else if (classDiff.getOriginalClass().isObject())
        	AST_type = Constants.get().COMPANION_OBJECT;
        Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
        Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isObject()) {
        	srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),Constants.get().OBJECT_DECLARATION);
        	dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),Constants.get().OBJECT_DECLARATION);
        }
        if (srcTypeDeclaration == null || dstTypeDeclaration == null) return;
        if (srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null) {
            if (
                    srcTypeDeclaration.getParent().getType().name.equals(Constants.get().TYPE_DECLARATION_STATEMENT)
                            &&
                            dstTypeDeclaration.getParent().getType().name.equals(Constants.get().TYPE_DECLARATION_STATEMENT)
            )
                mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
        }

        mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);

        String v1 = classDiff.getOriginalClass().getVisibility().toString();
        String v2 = classDiff.getNextClass().getVisibility().toString();
        Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, Constants.get().MODIFIER, v1);
        Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, Constants.get().MODIFIER, v2);
        if (tree1 != null && tree2 != null)
            mappingStore.addMappingRecursively(tree1,tree2);

        List<String> searchingTypes = new ArrayList<>();
        searchingTypes.add(Constants.get().SIMPLE_NAME);
        searchingTypes.add(Constants.get().TYPE_IDENTIFIER);
        searchingTypes.add(Constants.get().TYPE_DECLARATION_KIND);
        searchingTypes.add(Constants.get().MODIFIERS);
        for (String type : searchingTypes) {
            Pair<Tree,Tree> matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
            new SameModifierMatcher(Constants.get().STATIC).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
            new SameModifierMatcher(Constants.get().FINAL).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
            new SameModifierMatcher(Constants.get().ABSTRACT).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isSealed() && classDiff.getNextClass().isSealed())
            new SameModifierMatcher(Constants.get().SEALED).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isStrictfp() && classDiff.getNextClass().isStrictfp())
            new SameModifierMatcher(Constants.get().STRICTFP).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(Constants.get().ANNOTATION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(Constants.get().ENUM).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(Constants.get().OPEN).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(Constants.get().COMPANION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);

        if (classDiff.getTypeParameterDiffList() != null)
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
            if (srcTypeParam.getParent().getType().name.equals(Constants.get().TYPE_PARAMETERS) && dstTypeParam.getParent().getType().name.equals(Constants.get().TYPE_PARAMETERS)) {
                mappingStore.addMapping(srcTypeParam.getParent(), dstTypeParam.getParent());
            }
        }
        
        if (classDiff.getPrimaryConstructorParameterListDiff().isPresent()) {
            Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> pairs = classDiff.getPrimaryConstructorParameterListDiff().get().getCommonParameters();
            for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> pair : pairs) {
                processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, pair.getLeft(), pair.getRight());
            }
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, classDiff.getOriginalClass().getPrimaryConstructor().get().getLocationInfo());
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, classDiff.getNextClass().getPrimaryConstructor().get().getLocationInfo());
            if (srcSubTree != null && dstSubTree != null) {
                mappingStore.addMapping(srcSubTree, dstSubTree);
                new SameModifierMatcher(Constants.get().INTERNAL).match(srcSubTree,dstSubTree,mappingStore);
                new SameModifierMatcher(Constants.get().PRIVATE).match(srcSubTree,dstSubTree,mappingStore);
                Pair<Tree,Tree> matched = findPairOfType(srcSubTree,dstSubTree,Constants.get().MODIFIERS);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
                matched = findPairOfType(srcSubTree,dstSubTree,Constants.get().CONSTRUCTOR_KEYWORD);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
            }
        }
        
        processSuperClasses(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassPermittedTypes(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processInterfaceToSuperclassOrOpposite(classDiff, mappingStore, srcTypeDeclaration, dstTypeDeclaration);
        new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getJavadoc(), classDiff.getNextClass().getJavadoc(), classDiff.getJavadocDiff())
                .match(srcTree, dstTree, mappingStore);
        new CommentMatcher(optimizationData, classDiff.getPackageDeclarationCommentListDiff()).match(srcTree, dstTree, mappingStore);
        if (classDiff.getPackageDeclarationJavadocDiff().isPresent()) {
        	new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getPackageDeclarationJavadoc(), classDiff.getNextClass().getPackageDeclarationJavadoc(), classDiff.getPackageDeclarationJavadocDiff())
            .match(srcTree, dstTree, mappingStore);
        }
        processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);
        processClassBlock(srcTypeDeclaration, dstTypeDeclaration, mappingStore);
    }

    private void processClassBlock(Tree srcTypeDeclaration, Tree dstTypeDeclaration, ExtendedMultiMappingStore mappingStore) {
        Tree srcBlock = TreeUtilFunctions.findFirstByType(srcTypeDeclaration, Constants.get().CLASS_BLOCK);
        Tree dstBlock = TreeUtilFunctions.findFirstByType(dstTypeDeclaration, Constants.get().CLASS_BLOCK);
        if (srcBlock == null || dstBlock == null) return;
        mappingStore.addMapping(srcBlock, dstBlock);

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
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getInterfaceListDiff().getCommonTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getInterfaceListDiff().getChangedTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
        String keyword = IMPLEMENTS_KEYWORD_LABEL;
        if (classDiff.getOriginalClass().isInterface())
            keyword = EXTENDS_KEYWORD_LABEL;
        new KeywordMatcher(Constants.get().TYPE_INHERITANCE_KEYWORD, keyword).match(srcTree, dstTree, mappingStore);

    }


    private void processClassPermittedTypes(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getPermittedTypeListDiff().getCommonTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getPermittedTypeListDiff().getChangedTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
        new KeywordMatcher(Constants.get().PERMITS_KEYWORD, PERMITS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
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
            processArgumentList(srcTree, dstTree, mappingStore, srcParentUML, dstParentUML);

        }
        new KeywordMatcher(Constants.get().TYPE_INHERITANCE_KEYWORD, EXTENDS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
    }

    private void processArgumentList(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, UMLType left, UMLType right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo());
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo());
        if (srcSubTree == null || dstSubTree == null) return;
        Tree src_argumentList = srcSubTree.getParent();
        Tree dst_argumentList = dstSubTree.getParent();
        if (src_argumentList != null && dst_argumentList != null &&
                (src_argumentList.getType().name.equals(Constants.get().ARGUMENT_LIST) || src_argumentList.getType().name.equals(Constants.get().CONSTRUCTOR_INVOCATION)) &&
                (dst_argumentList.getType().name.equals(Constants.get().ARGUMENT_LIST) || dst_argumentList.getType().name.equals(Constants.get().CONSTRUCTOR_INVOCATION))) {
            if(src_argumentList.isIsomorphicTo(dst_argumentList))
                mappingStore.addMappingRecursively(src_argumentList,dst_argumentList);
            else
                mappingStore.addMapping(src_argumentList,dst_argumentList);
        }
        if (src_argumentList.getParent().getType().name.equals(Constants.get().DELEGATION_SPECIFIER) && dst_argumentList.getParent().getType().name.equals(Constants.get().DELEGATION_SPECIFIER)) {
            mappingStore.addMapping(src_argumentList.getParent(),dst_argumentList.getParent());
        }
    }

    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
    }
}
