package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLTypeParameterListDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.statement.IgnoringCommentsLeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

/* Created by pourya on 2024-05-22*/
public class MethodMatcher extends BodyMapperMatcher{

    private static final String THROWS_KEYWORD_LABEL = "throws";
    private boolean refactoringProcessor = false;

    public MethodMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractMethod, boolean refactoringProcessor) {
        super(bodyMapper, isPartOfExtractMethod);
        this.refactoringProcessor = refactoringProcessor;
    }

    public MethodMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractMethod) {
        super(bodyMapper, isPartOfExtractMethod);
    }

    public MethodMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper) {
        super(optimizationData, bodyMapper, false);
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processMethod(srcTree, dstTree, bodyMapper, mappingStore);

    }

    private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        Tree srcOperationNode;
        Tree dstOperationNode;
        if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
            srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo());
            dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo());
            if (srcOperationNode != null && dstOperationNode != null) {
            //handle case where class has only a single function and the locationInfo of function declaration is identical with the class block in Python
            if (srcOperationNode.getType().name.equals(Constants.get().CLASS_BLOCK)) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (dstOperationNode.getType().name.equals(Constants.get().CLASS_BLOCK)) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (srcOperationNode.getType().name.endsWith("_comment")) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (dstOperationNode.getType().name.endsWith("_comment")) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (srcOperationNode.getType().name.equals(Constants.get().PREFIX_EXPRESSION)) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (dstOperationNode.getType().name.equals(Constants.get().PREFIX_EXPRESSION)) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), Constants.get().METHOD_DECLARATION);
            }
            if (srcOperationNode.getType().name.equals(Constants.get().FUNCTION_KEYWORD)) {
            	srcOperationNode = srcOperationNode.getParent().getParent();
            }
            if (dstOperationNode.getType().name.equals(Constants.get().FUNCTION_KEYWORD)) {
            	dstOperationNode = dstOperationNode.getParent().getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getParent() != null && srcOperationNode.getType().name.equals(Constants.get().MODIFIERS)) {
                srcOperationNode = srcOperationNode.getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getParent() != null && dstOperationNode.getType().name.equals(Constants.get().MODIFIERS)) {
                dstOperationNode = dstOperationNode.getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getParent() != null && srcOperationNode.getParent().getType().name.equals(Constants.get().DECORATED_METHOD)) {
                srcOperationNode = srcOperationNode.getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getParent() != null && dstOperationNode.getParent().getType().name.equals(Constants.get().DECORATED_METHOD)) {
                dstOperationNode = dstOperationNode.getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(Constants.get().ERROR) && srcOperationNode.getChildren().size() > 0 &&
            		srcOperationNode.getChildren().get(0).getType().name.equals(Constants.get().METHOD_DECLARATION)) {
            	srcOperationNode = srcOperationNode.getChildren().get(0);
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(Constants.get().ERROR) && dstOperationNode.getChildren().size() > 0 &&
            		dstOperationNode.getChildren().get(0).getType().name.equals(Constants.get().METHOD_DECLARATION)) {
            	dstOperationNode = dstOperationNode.getChildren().get(0);
            }
            if (srcOperationNode == null || !(srcOperationNode.getType().name.equals(Constants.get().METHOD_DECLARATION) || srcOperationNode.getType().name.equals(Constants.get().SECONDARY_CONSTRUCTOR) || srcOperationNode.getType().name.equals(Constants.get().DECORATED_METHOD) || srcOperationNode.getType().name.equals(Constants.get().ANNOTATION_TYPE_MEMBER_DECLARATION) || srcOperationNode.getType().name.equals(Constants.get().GETTER) || srcOperationNode.getType().name.equals(Constants.get().SETTER))) return;
            if (dstOperationNode == null || !(dstOperationNode.getType().name.equals(Constants.get().METHOD_DECLARATION) || dstOperationNode.getType().name.equals(Constants.get().SECONDARY_CONSTRUCTOR) || dstOperationNode.getType().name.equals(Constants.get().DECORATED_METHOD) || dstOperationNode.getType().name.equals(Constants.get().ANNOTATION_TYPE_MEMBER_DECLARATION) || dstOperationNode.getType().name.equals(Constants.get().GETTER) || dstOperationNode.getType().name.equals(Constants.get().SETTER))) return;
            new JavaDocMatcher(optimizationData, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), umlOperationBodyMapper.getJavadocDiff())
                    .match(srcOperationNode, dstOperationNode, mappingStore);
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
            }
        } else {
            //Static Initializers
            if(srcTree.getType().name.equals(Constants.get().MODULE))
                srcOperationNode = srcTree;
            else
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
            if(dstTree.getType().name.equals(Constants.get().MODULE))
                dstOperationNode = dstTree;
            else
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
            if (srcOperationNode != null && dstOperationNode != null) {
                if (srcOperationNode.getType().name.equals(Constants.get().INITIALIZER) && dstOperationNode.getType().name.equals(Constants.get().INITIALIZER)) {
                    mappingStore.addMapping(srcOperationNode, dstOperationNode);
                    //static keyword
                    if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer && umlOperationBodyMapper.getContainer2() instanceof UMLInitializer) {
                        UMLInitializer initializer1 = (UMLInitializer)umlOperationBodyMapper.getContainer1();
                        UMLInitializer initializer2 = (UMLInitializer)umlOperationBodyMapper.getContainer2();
                        Tree srcInitKeyword = TreeUtilFunctions.findChildByType(srcOperationNode, Constants.get().INIT_KEYWORD);
                        Tree dstInitKeyword = TreeUtilFunctions.findChildByType(dstOperationNode, Constants.get().INIT_KEYWORD);
                        if (srcInitKeyword != null && dstInitKeyword != null) {
                            mappingStore.addMapping(srcInitKeyword, dstInitKeyword);
                        }
                        if (initializer1.isStatic() && initializer2.isStatic()) {
                            Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode, Constants.get().MODIFIER);
                            Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode, Constants.get().MODIFIER);
                            if (srcModifier != null && dstModifier != null)
                                mappingStore.addMapping(srcModifier, dstModifier);
                        }
                        if (initializer1.getJavadoc() != null && initializer2.getJavadoc() != null) {
                            //Javadoc
                            new JavaDocMatcher(optimizationData, initializer1.getJavadoc(), initializer2.getJavadoc(), umlOperationBodyMapper.getJavadocDiff())
                                    .match(srcOperationNode, dstOperationNode, mappingStore);
                        }
                    }
                }
            }
        }
        if (srcOperationNode != null && dstOperationNode != null) {
            processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            if(umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation1().getDefaultExpression() != null &&
                    umlOperationBodyMapper.getOperation2() != null && umlOperationBodyMapper.getOperation2().getDefaultExpression() != null) {
                Tree srcOperationFunctionBody = srcOperationNode.getChild(srcOperationNode.getChildren().size()-1);
                Tree dstOperationFunctionBody = dstOperationNode.getChild(dstOperationNode.getChildren().size()-1);
                Tree srcAffectationKeyword = TreeUtilFunctions.findChildByType(srcOperationFunctionBody, Constants.get().AFFECTATION_OPERATOR);
                Tree dstAffectationKeyword = TreeUtilFunctions.findChildByType(dstOperationFunctionBody, Constants.get().AFFECTATION_OPERATOR);
                mappingStore.addMapping(srcAffectationKeyword, dstAffectationKeyword);
            }
            new BodyMapperMatcher(optimizationData, umlOperationBodyMapper, isPartOfExtractedMethod).match(srcOperationNode, dstOperationNode, mappingStore);
            processOperationDiff(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            processMethodParameters(srcOperationNode, dstOperationNode, umlOperationBodyMapper.getMatchedVariables(), mappingStore);
            if (refactoringProcessor){
                new RefactoringMatcher(optimizationData, new ArrayList<>(bodyMapper.getRefactoringsAfterPostProcessing())).
                        matchAndUpdateOptimizationStore(srcTree, dstTree, mappingStore);
            }
        }
    }

    private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        if (srcOperationNode == null || dstOperationNode == null) return;
        List<String> searchingTypes = new ArrayList<>();
        //searchingTypes.add(Constants.get().ACCESS_MODIFIER);
        searchingTypes.add(Constants.get().SIMPLE_NAME);
        searchingTypes.add(Constants.get().PRIMITIVE_TYPE);
        searchingTypes.add(Constants.get().BLOCK);
        searchingTypes.add(Constants.get().FUNCTION_BODY);
        searchingTypes.add(Constants.get().FUNCTION_KEYWORD);
        searchingTypes.add(Constants.get().CONSTRUCTOR_KEYWORD);
        searchingTypes.add(Constants.get().FUNCTION_PARAMETERS);
        searchingTypes.add(Constants.get().MODIFIERS);
        for (String type : searchingTypes) {
            com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,type);
            if (matched != null) {
                if(type.equals(Constants.get().SIMPLE_NAME) && matched.first.getChildren().size() > 0 && matched.second.getChildren().size() > 0) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                else {
                    mappingStore.addMapping(matched.first,matched.second);
                }
            }
        }
        if (umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation2() != null) {
            if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
                new SameModifierMatcher(Constants.get().STATIC).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
                new SameModifierMatcher(Constants.get().FINAL).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
                new SameModifierMatcher(Constants.get().ABSTRACT).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isSynchronized() && umlOperationBodyMapper.getOperation2().isSynchronized())
                new SameModifierMatcher(Constants.get().SYNCHRONIZED).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isNative() && umlOperationBodyMapper.getOperation2().isNative())
                new SameModifierMatcher(Constants.get().NATIVE).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isDefault() && umlOperationBodyMapper.getOperation2().isDefault())
                new SameModifierMatcher(Constants.get().DEFAULT).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isStrictfp() && umlOperationBodyMapper.getOperation2().isStrictfp())
                new SameModifierMatcher(Constants.get().STRICTFP).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isInline() && umlOperationBodyMapper.getOperation2().isInline())
                new SameModifierMatcher(Constants.get().INLINE).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().OVERRIDE).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().SUSPEND).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().OPEN).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().OPERATOR).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().INTERNAL).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(Constants.get().INFIX).match(srcOperationNode,dstOperationNode,mappingStore);
            String v1 = umlOperationBodyMapper.getOperation1().getVisibility().toString();
            String v2 = umlOperationBodyMapper.getOperation2().getVisibility().toString();
            Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcOperationNode, Constants.get().MODIFIER, v1);
            Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstOperationNode, Constants.get().MODIFIER, v2);
            if (tree1 != null && tree2 != null) {
                mappingStore.addMappingRecursively(tree1,tree2);
            }
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
            if (srcTypeParam.getParent().getType().name.equals(Constants.get().TYPE_PARAMETERS) && dstTypeParam.getParent().getType().name.equals(Constants.get().TYPE_PARAMETERS)) {
                mappingStore.addMapping(srcTypeParam.getParent(), dstTypeParam.getParent());
            }
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
            if (srcClassAnnotationTree == null || dstClassAnnotationTree == null) continue;
            if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
                mappingStore.addMappingRecursively(srcClassAnnotationTree, dstClassAnnotationTree);
            else {
                new IgnoringCommentsLeafMatcher().match(srcClassAnnotationTree, dstClassAnnotationTree, mappingStore);
            }
            if(umlOperationDiff.getAnnotationListDiff().getCommonAnnotations().size() > 1 &&
            		srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(Constants.get().MODIFIERS) &&
            		dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(Constants.get().MODIFIERS)) {
            	mappingStore.addMapping(srcClassAnnotationTree.getParent(), dstClassAnnotationTree.getParent());
            }
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        new KeywordMatcher(Constants.get().THROWS_KEYWORD, THROWS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
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
        VariableDeclarationContainer removedOperation = umlOperationDiff.getRemovedOperation();
        VariableDeclarationContainer addedOperation = umlOperationDiff.getAddedOperation();
        if (removedOperation instanceof UMLOperation && ((UMLOperation)removedOperation).getReturnParameter() != null &&
                addedOperation instanceof UMLOperation && ((UMLOperation)addedOperation).getReturnParameter() != null ) {
            LocationInfo srcLocationInfo = ((UMLOperation)removedOperation).getReturnParameter().getType().getLocationInfo();
            LocationInfo dstLocationInfo = ((UMLOperation)addedOperation).getReturnParameter().getType().getLocationInfo();
            Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo);
            Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo);
            if (srcNode == null || dstNode == null) return;
            if (srcNode.isIsoStructuralTo(dstNode))
                mappingStore.addMappingRecursively(srcNode,dstNode);
            else {
                new LeafMatcher().match(srcNode,dstNode,mappingStore);
            }
        }
        if (removedOperation instanceof UMLOperation leftOperation && addedOperation instanceof UMLOperation rightOperation &&
                leftOperation.getReceiverTypeReference() != null && rightOperation.getReceiverTypeReference() != null) {
            LocationInfo srcLocationInfo = leftOperation.getReceiverTypeReference().getLocationInfo();
            LocationInfo dstLocationInfo = rightOperation.getReceiverTypeReference().getLocationInfo();
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

    private void matchThrowKeyword(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcThrowKeyword = TreeUtilFunctions.findChildByType(srcTree, Constants.get().THROWS_KEYWORD);
        Tree dstThrowKeyword = TreeUtilFunctions.findChildByType(dstTree, Constants.get().THROWS_KEYWORD);
        if (srcThrowKeyword != null && dstThrowKeyword != null) {
            mappingStore.addMappingRecursively(srcThrowKeyword, dstThrowKeyword);
        }
    }

    private void processMethodParameters(Tree srcTree, Tree dstTree, Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
            VariableDeclaration leftVarDecl = matchedPair.getLeft();
            VariableDeclaration rightVarDecl = matchedPair.getRight();
            Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getLocationInfo());
            Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getLocationInfo());
            if (leftTree == null || rightTree == null) return;
            if (leftTree.getType().name.endsWith("_comment")) {
                leftTree = TreeUtilFunctions.findByLocationInfo(srcTree, leftVarDecl.getLocationInfo(), Constants.get().PARAMETER);
            }
            if (rightTree.getType().name.endsWith("_comment")) {
                rightTree = TreeUtilFunctions.findByLocationInfo(dstTree, rightVarDecl.getLocationInfo(), Constants.get().PARAMETER);
            }
            if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
                if (TreeUtilFunctions.isIsomorphicTo(rightTree, leftTree))
                    mappingStore.addMappingRecursively(leftTree, rightTree);
                else {
                    new LeafMatcher().match(leftTree,rightTree,mappingStore);
                    mappingStore.addMapping(leftTree,rightTree);
                }
                if(leftTree.getType().name.equals(Constants.get().PARAMETER_MODIFIERS) && rightTree.getType().name.equals(Constants.get().PARAMETER_MODIFIERS)) {
                	Tree leftParameter = TreeUtilFunctions.findByLocationInfo(leftTree.getParent(), leftVarDecl.getLocationInfo(), Constants.get().PARAMETER);
                	Tree rightParameter = TreeUtilFunctions.findByLocationInfo(rightTree.getParent(), rightVarDecl.getLocationInfo(), Constants.get().PARAMETER);
                	if (TreeUtilFunctions.isIsomorphicTo(leftParameter, rightParameter))
                        mappingStore.addMappingRecursively(leftParameter, rightParameter);
                    else {
                        new LeafMatcher().match(leftParameter,rightParameter,mappingStore);
                        mappingStore.addMapping(leftParameter,rightParameter);
                    }
                }
                if(leftVarDecl.getInitializer() != null && rightVarDecl.getInitializer() != null) {
                	Tree leftInitializerTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getInitializer().getLocationInfo());
                    Tree rightInitializerTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getInitializer().getLocationInfo());
                    if (leftInitializerTree == null || rightInitializerTree == null) return;
                    if (TreeUtilFunctions.isIsomorphicTo(leftInitializerTree, rightInitializerTree))
                        mappingStore.addMappingRecursively(leftInitializerTree, rightInitializerTree);
                    else {
                        new LeafMatcher().match(leftInitializerTree, rightInitializerTree,mappingStore);
                        mappingStore.addMapping(leftInitializerTree, rightInitializerTree);
                    }
                    int leftPosition = leftInitializerTree.positionInParent();
                    int rightPosition = rightInitializerTree.positionInParent();
                    if(leftPosition > 0 && rightPosition > 0) {
                    	Tree previousLeft = leftInitializerTree.getParent().getChild(leftPosition-1);
                    	Tree previousRight = rightInitializerTree.getParent().getChild(rightPosition-1);
                    	if(previousLeft.getType().name.equals(Constants.get().AFFECTATION_OPERATOR) && previousRight.getType().name.equals(Constants.get().AFFECTATION_OPERATOR)) {
                    		mappingStore.addMapping(previousLeft, previousRight);
                    	}
                    }
                }
            }
        }
    }
}
