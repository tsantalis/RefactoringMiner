package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.UMLTypeAlias;
import gr.uom.java.xmi.UMLTypeParameter;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLTypeAliasListDiff;

import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.Set;

import static org.refactoringminer.astDiff.utils.Helpers.findPairOfType;

/* Created by pourya on 2024-05-22*/
public class ClassDeclarationMatcher extends OptimizationAwareMatcher implements TreeMatcher {

    private final UMLClassBaseDiff baseClassDiff;
    private final String IMPLEMENTS_KEYWORD_LABEL = "implements";
    private final String EXTENDS_KEYWORD_LABEL = "extends";
    private final String PERMITS_KEYWORD_LABEL = "permits";
    private final Constants LANG1;
    private final Constants LANG2;

    public ClassDeclarationMatcher(UMLClassBaseDiff baseClassDiff, Constants LANG1, Constants LANG2) {
        this.baseClassDiff = baseClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public ClassDeclarationMatcher(OptimizationData optimizationData, UMLClassBaseDiff baseClassDiff, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.baseClassDiff = baseClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processClassDeclarationMapping(srcTree,dstTree,baseClassDiff,mappingStore);
    }

    private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        String AST_type1 = astType(classDiff.getOriginalClass(), LANG1);
        String AST_type2 = astType(classDiff.getNextClass(), LANG2);
        Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,AST_type1);
        Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,AST_type2);
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isObject() && classDiff.getNextClass().isObject()) {
        	srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT_DECLARATION);
        	dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT_DECLARATION);
        }
        if(classDiff.getOriginalClass().isObject() && !classDiff.getNextClass().isObject()) {
        	srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT_DECLARATION);
        	dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.TYPE_DECLARATION);
        }
        else if(!classDiff.getOriginalClass().isObject() && classDiff.getNextClass().isObject()) {
        	srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.TYPE_DECLARATION);
        	dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT_DECLARATION);
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null && classDiff.getOriginalClass().isModule() && classDiff.getNextClass().isModule()) {
        	srcTypeDeclaration = srcTree;
        	dstTypeDeclaration = dstTree;
        }
        if (srcTypeDeclaration == null && dstTypeDeclaration == null) {
        	srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.ERROR);
        	dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),LANG2,LANG2.ERROR);
        }
        if(classDiff.getTypeAliasListDiff().isPresent()) {
        	processTypeAliasList(srcTree, dstTree, classDiff.getTypeAliasListDiff().get(), mappingStore);
        }
        if (srcTypeDeclaration == null || dstTypeDeclaration == null) return;
        if (srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null) {
            if (srcTypeDeclaration.getParent().getType().name.equals(LANG1.TYPE_DECLARATION_STATEMENT)
                    && dstTypeDeclaration.getParent().getType().name.equals(LANG2.TYPE_DECLARATION_STATEMENT)) {
                mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
            }
        }

        if(classDiff.getOriginalClass().isFunctionalInterface() && classDiff.getNextClass().isFunctionalInterface()) {
            new MethodMatcher(optimizationData, classDiff.getOperationBodyMapperList().get(0), LANG1, LANG2).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
            return;
        }
        mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);

        String v1 = classDiff.getOriginalClass().getVisibility().toString();
        String v2 = classDiff.getNextClass().getVisibility().toString();
        Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, LANG1.MODIFIER, v1, LANG1);
        Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, LANG2.MODIFIER, v2, LANG2);
        if (tree1 != null && tree2 != null)
            mappingStore.addMappingRecursively(tree1,tree2);

        Pair<Tree,Tree> matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.TYPE_IDENTIFIER,LANG2.TYPE_IDENTIFIER);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.TYPE_DECLARATION_KIND,LANG2.TYPE_DECLARATION_KIND);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        matched = findPairOfType(srcTypeDeclaration,dstTypeDeclaration,LANG1.MODIFIERS,LANG2.MODIFIERS);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);

        if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
            new SameModifierMatcher(LANG1, LANG2, LANG1.STATIC).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
            new SameModifierMatcher(LANG1, LANG2, LANG1.FINAL).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
            new SameModifierMatcher(LANG1, LANG2, LANG1.ABSTRACT).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isSealed() && classDiff.getNextClass().isSealed())
            new SameModifierMatcher(LANG1, LANG2, LANG1.SEALED).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        if (classDiff.getOriginalClass().isStrictfp() && classDiff.getNextClass().isStrictfp())
            new SameModifierMatcher(LANG1, LANG2, LANG1.STRICTFP).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.ANNOTATION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.ENUM).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.OPEN).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.DATA).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.COMPANION).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);
        new SameModifierMatcher(LANG1, LANG2, LANG1.INNER).match(srcTypeDeclaration,dstTypeDeclaration,mappingStore);

        if (classDiff.getTypeParameterDiffList() != null)
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo(), LANG1);
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo(), LANG2);
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
            if (srcTypeParam.getParent().getType().name.equals(LANG1.TYPE_PARAMETERS) && dstTypeParam.getParent().getType().name.equals(LANG2.TYPE_PARAMETERS)) {
                mappingStore.addMapping(srcTypeParam.getParent(), dstTypeParam.getParent());
            }
        }
        
        if (classDiff.getPrimaryConstructorParameterListDiff().isPresent()) {
            Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> pairs = classDiff.getPrimaryConstructorParameterListDiff().get().getCommonParameters();
            for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> pair : pairs) {
                Tree srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, pair.getLeft().getLocationInfo(), LANG1);
                Tree dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, pair.getRight().getLocationInfo(), LANG2);
                if (srcFieldDeclaration == null || srcFieldDeclaration.getType().name.endsWith("_comment")) {
                    srcFieldDeclaration = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, pair.getLeft().getLocationInfo(), LANG1, LANG1.CLASS_PARAMETER);
                }
                if (dstFieldDeclaration == null || dstFieldDeclaration.getType().name.endsWith("_comment")) {
                    dstFieldDeclaration = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, pair.getRight().getLocationInfo(), LANG2, LANG2.CLASS_PARAMETER);
                }
                if (srcFieldDeclaration != null && dstFieldDeclaration != null && srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
                    mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
            }
            Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, classDiff.getOriginalClass().getPrimaryConstructor().get().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, classDiff.getNextClass().getPrimaryConstructor().get().getLocationInfo(), LANG2);
            if (srcSubTree != null && dstSubTree != null) {
                mappingStore.addMapping(srcSubTree, dstSubTree);
                new SameModifierMatcher(LANG1, LANG2, LANG1.INTERNAL).match(srcSubTree,dstSubTree,mappingStore);
                new SameModifierMatcher(LANG1, LANG2, LANG1.PRIVATE).match(srcSubTree,dstSubTree,mappingStore);
                matched = findPairOfType(srcSubTree,dstSubTree,LANG1.MODIFIERS,LANG2.MODIFIERS);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
                matched = findPairOfType(srcSubTree,dstSubTree,LANG1.CONSTRUCTOR_KEYWORD,LANG2.CONSTRUCTOR_KEYWORD);
                if (matched != null)
                    mappingStore.addMapping(matched.first,matched.second);
            }
        }
        
        processSuperClasses(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processClassPermittedTypes(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
        processInterfaceToSuperclassOrOpposite(classDiff, mappingStore, srcTypeDeclaration, dstTypeDeclaration);
        new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getJavadoc(), classDiff.getNextClass().getJavadoc(), classDiff.getJavadocDiff(), LANG1, LANG2)
                .match(srcTree, dstTree, mappingStore);
        new CommentMatcher(optimizationData, classDiff.getPackageDeclarationCommentListDiff(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        if (classDiff.getPackageDeclarationJavadocDiff().isPresent()) {
            new JavaDocMatcher(optimizationData, classDiff.getOriginalClass().getPackageDeclarationJavadoc(), classDiff.getNextClass().getPackageDeclarationJavadoc(), classDiff.getPackageDeclarationJavadocDiff(), LANG1, LANG2)
            .match(srcTree, dstTree, mappingStore);
        }
        processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);
        processClassBlock(srcTypeDeclaration, dstTypeDeclaration, mappingStore);
    }

    private String astType(UMLClass umlClass, Constants LANG) {
        String type = LANG.TYPE_DECLARATION;
        if (umlClass.isEnum())
            type = LANG.ENUM_DECLARATION;
        else if (umlClass.isAnnotation())
            type = LANG.ANNOTATION_TYPE_DECLARATION;
        else if (umlClass.isRecord())
            type = LANG.RECORD_DECLARATION;
        else if (umlClass.isModule())
        	type = LANG.MODULE;
        else if (umlClass.isObject())
            type = LANG.COMPANION_OBJECT;
        else if (umlClass.isFunctionalInterface())
            type = LANG.METHOD_DECLARATION;
        return type;
    }

    private void processClassBlock(Tree srcTypeDeclaration, Tree dstTypeDeclaration, ExtendedMultiMappingStore mappingStore) {
        Tree srcBlock = TreeUtilFunctions.findFirstByType(srcTypeDeclaration, LANG1.CLASS_BLOCK);
        Tree dstBlock = TreeUtilFunctions.findFirstByType(dstTypeDeclaration, LANG2.CLASS_BLOCK);
        if (srcBlock == null && dstBlock == null) {
            srcBlock = TreeUtilFunctions.findFirstByType(srcTypeDeclaration, LANG1.DELEGATION_SPECIFIER);
            dstBlock = TreeUtilFunctions.findFirstByType(dstTypeDeclaration, LANG2.DELEGATION_SPECIFIER);
            if (srcBlock != null && dstBlock != null) {
                mappingStore.addMappingRecursively(srcBlock, dstBlock);
                return;
            }
        }
        if (srcBlock == null || dstBlock == null) return;
        mappingStore.addMapping(srcBlock, dstBlock);

    }

    private void processInterfaceToSuperclassOrOpposite(UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore, Tree srcTypeDeclaration, Tree dstTypeDeclaration) {
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
        new KeywordMatcher(LANG1, LANG2, LANG1.TYPE_INHERITANCE_KEYWORD, LANG2.TYPE_INHERITANCE_KEYWORD, keyword).match(srcTree, dstTree, mappingStore);

    }


    private void processClassPermittedTypes(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getPermittedTypeListDiff().getCommonTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
        for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getPermittedTypeListDiff().getChangedTypes())
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
        new KeywordMatcher(LANG1, LANG2, LANG1.PERMITS_KEYWORD, LANG2.PERMITS_KEYWORD, PERMITS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
    }

    private void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo(), LANG1);
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo(), LANG2);
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
        new KeywordMatcher(LANG1, LANG2, LANG1.TYPE_INHERITANCE_KEYWORD, LANG2.TYPE_INHERITANCE_KEYWORD, EXTENDS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
    }

    private void processArgumentList(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, UMLType left, UMLType right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo(), LANG1);
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo(), LANG2);
        if (srcSubTree == null || dstSubTree == null) return;
        Tree src_argumentList = srcSubTree.getParent();
        Tree dst_argumentList = dstSubTree.getParent();
        if (src_argumentList != null && dst_argumentList != null &&
                (src_argumentList.getType().name.equals(LANG1.ARGUMENT_LIST) || src_argumentList.getType().name.equals(LANG1.CONSTRUCTOR_INVOCATION)) &&
                (dst_argumentList.getType().name.equals(LANG2.ARGUMENT_LIST) || dst_argumentList.getType().name.equals(LANG2.CONSTRUCTOR_INVOCATION))) {
            if(src_argumentList.isIsomorphicTo(dst_argumentList))
                mappingStore.addMappingRecursively(src_argumentList,dst_argumentList);
            else
                mappingStore.addMapping(src_argumentList,dst_argumentList);
        }
        if (src_argumentList.getParent().getType().name.equals(LANG1.DELEGATION_SPECIFIER) && dst_argumentList.getParent().getType().name.equals(LANG2.DELEGATION_SPECIFIER)) {
            mappingStore.addMapping(src_argumentList.getParent(),dst_argumentList.getParent());
        }
    }

    private void processTypeAliasList(Tree srcTree, Tree dstTree, UMLTypeAliasListDiff typeAliasListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeAlias, UMLTypeAlias> typeAliasPair : typeAliasListDiff.getCommonTypeAliases()) {
        	Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, typeAliasPair.getLeft().getLocationInfo(), LANG1);
            Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, typeAliasPair.getRight().getLocationInfo(), LANG2);
            if (srcSubTree == null || dstSubTree == null) return;
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        }
    }

    private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo(), LANG1);
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo(), LANG2);
            if(annotationListDiff.getCommonAnnotations().size() > 1 &&
            		srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(LANG1.MODIFIERS) &&
            		dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(LANG2.MODIFIERS)) {
            	mappingStore.addMapping(srcClassAnnotationTree.getParent(), dstClassAnnotationTree.getParent());
            }
        }
    }
}
