package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLTypeParameterListDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.matchers.statement.BasicTreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

/* Created by pourya on 2024-05-22*/
public class MethodMatcher extends BodyMapperMatcher{

    public MethodMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper) {
        super(optimizationData, bodyMapper, false);
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processMethod(srcTree, dstTree, bodyMapper, mappingStore);
    }

    private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        Tree srcOperationNode;
        Tree dstOperationNode;
        if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
            srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo());
            dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo());
            if (srcOperationNode == null || !(srcOperationNode.getType().name.equals(Constants.METHOD_DECLARATION) || srcOperationNode.getType().name.equals(Constants.ANNOTATION_TYPE_MEMBER_DECLARATION))) return;
            if (dstOperationNode == null || !(dstOperationNode.getType().name.equals(Constants.METHOD_DECLARATION) || dstOperationNode.getType().name.equals(Constants.ANNOTATION_TYPE_MEMBER_DECLARATION))) return;
            new JavaDocMatcher(optimizationData, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc())
                    .match(srcOperationNode, dstOperationNode, mappingStore);
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
        } else {
            //Static Initializers
            srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
            dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
            if (srcOperationNode != null & dstOperationNode != null) {
                if (srcOperationNode.getType().name.equals(Constants.INITIALIZER) && dstOperationNode.getType().name.equals(Constants.INITIALIZER)) {
                    mappingStore.addMapping(srcOperationNode, dstOperationNode);
                    //static keyword
                    if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer && umlOperationBodyMapper.getContainer2() instanceof UMLInitializer) {
                    	UMLInitializer initializer1 = (UMLInitializer)umlOperationBodyMapper.getContainer1();
                        UMLInitializer initializer2 = (UMLInitializer)umlOperationBodyMapper.getContainer2();
                    	if (initializer1.isStatic() && initializer2.isStatic()) {
                            Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode, Constants.MODIFIER);
                            Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode, Constants.MODIFIER);
                            if (srcModifier != null && dstModifier != null)
                                mappingStore.addMapping(srcModifier, dstModifier);
                        }
                        if (initializer1.getJavadoc() != null && initializer2.getJavadoc() != null) {
                        	//Javadoc
                        	new JavaDocMatcher(optimizationData, initializer1.getJavadoc(), initializer2.getJavadoc())
                        			.match(srcOperationNode, dstOperationNode, mappingStore);
                        }
                    }
                }
            }
        }
        if (srcOperationNode != null && dstOperationNode != null) {
            processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            new BodyMapperMatcher(optimizationData, umlOperationBodyMapper, false).match(srcOperationNode, dstOperationNode, mappingStore);
            processOperationDiff(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            processMethodParameters(srcOperationNode, dstOperationNode, umlOperationBodyMapper.getMatchedVariables(), mappingStore);
        }
    }

    private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        if (srcOperationNode == null || dstOperationNode == null) return;
        List<String> searchingTypes = new ArrayList<>();
        //searchingTypes.add(Constants.ACCESS_MODIFIER);
        searchingTypes.add(Constants.SIMPLE_NAME);
        searchingTypes.add(Constants.PRIMITIVE_TYPE);
        searchingTypes.add(Constants.BLOCK);
        for (String type : searchingTypes) {
            com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,type);
            if (matched != null)
                mappingStore.addMapping(matched.first,matched.second);
        }
        if (umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation2() != null) {
            if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
                new SameModifierMatcher(Constants.STATIC).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
                new SameModifierMatcher(Constants.FINAL).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
                new SameModifierMatcher(Constants.ABSTRACT).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isSynchronized() && umlOperationBodyMapper.getOperation2().isSynchronized())
                new SameModifierMatcher(Constants.SYNCHRONIZED).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isNative() && umlOperationBodyMapper.getOperation2().isNative())
                new SameModifierMatcher(Constants.NATIVE).match(srcOperationNode,dstOperationNode,mappingStore);
            String v1 = umlOperationBodyMapper.getOperation1().getVisibility().toString();
            String v2 = umlOperationBodyMapper.getOperation2().getVisibility().toString();
            Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcOperationNode, Constants.MODIFIER, v1);
            Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstOperationNode, Constants.MODIFIER, v2);
            if (tree1 != null && tree2 != null)
                mappingStore.addMappingRecursively(tree1,tree2);
        }
    }

    private void processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
        if (umlOperationDiff == null) return;
        UMLTypeParameterListDiff umlTypeParameterListDiff = umlOperationDiff.getTypeParameterListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : umlTypeParameterListDiff.getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo());
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        if (commonExceptionTypes != null) {
            for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
                Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
                Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        AbstractMap.SimpleEntry<Set<UMLType>, Set<UMLType>> changedExceptionTypes = umlOperationDiff.getChangedExceptionTypes();
        if (changedExceptionTypes != null){
            Iterator<UMLType> keys = changedExceptionTypes.getKey().iterator();
            Iterator<UMLType> values = changedExceptionTypes.getValue().iterator();
            if (keys.hasNext() && values.hasNext())
            {
                Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, keys.next().getLocationInfo());
                Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, values.next().getLocationInfo());
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        if (umlOperationDiff.getRemovedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
            LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
            LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
            Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo);
            Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo);
            if (srcNode == null || dstNode == null) return;
            if (srcNode.isIsoStructuralTo(dstNode))
                mappingStore.addMappingRecursively(srcNode,dstNode);
            else {
                new LeafMatcher().match(srcNode,dstNode,mappingStore);
            }
        }
    }

    private void processMethodParameters(Tree srcTree, Tree dstTree, Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
            VariableDeclaration leftVarDecl = matchedPair.getLeft();
            VariableDeclaration rightVarDecl = matchedPair.getRight();
            Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getLocationInfo());
            Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getLocationInfo());
            if (leftTree == null || rightTree == null) return;
            if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
                if (TreeUtilFunctions.isIsomorphicTo(rightTree, leftTree))
                    mappingStore.addMappingRecursively(leftTree, rightTree);
                else {
                    new LeafMatcher().match(leftTree,rightTree,mappingStore);
                    mappingStore.addMapping(leftTree,rightTree);
                }
            }
        }
    }
}
