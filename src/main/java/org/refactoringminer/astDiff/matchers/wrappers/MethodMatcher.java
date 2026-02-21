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

    public MethodMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractMethod, boolean refactoringProcessor, Constants LANG1, Constants LANG2) {
        super(bodyMapper, isPartOfExtractMethod, LANG1, LANG2);
        this.refactoringProcessor = refactoringProcessor;
    }

    public MethodMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractMethod, Constants LANG1, Constants LANG2) {
        super(bodyMapper, isPartOfExtractMethod, LANG1, LANG2);
    }

    public MethodMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper, Constants LANG1, Constants LANG2) {
        super(optimizationData, bodyMapper, false, LANG1, LANG2);
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processMethod(srcTree, dstTree, bodyMapper, mappingStore);

    }

    private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        Tree srcOperationNode;
        Tree dstOperationNode;
        if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
            srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), LANG1);
            dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), LANG2);
            if (srcOperationNode != null && dstOperationNode != null) {
            //handle case where class has only a single function and the locationInfo of function declaration is identical with the class block in Python
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(LANG1.CLASS_BLOCK)) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), LANG1, LANG1.METHOD_DECLARATION);
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(LANG2.CLASS_BLOCK)) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), LANG2, LANG2.METHOD_DECLARATION);
            }
            if (srcOperationNode != null && srcOperationNode.getType().name.endsWith("_comment")) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), LANG1, LANG1.METHOD_DECLARATION);
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.endsWith("_comment")) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), LANG2, LANG2.METHOD_DECLARATION);
            }
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(LANG1.PREFIX_EXPRESSION)) {
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo(), LANG1, LANG1.METHOD_DECLARATION);
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(LANG2.PREFIX_EXPRESSION)) {
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo(), LANG2, LANG2.METHOD_DECLARATION);
            }
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(LANG1.FUNCTION_KEYWORD)) {
            	srcOperationNode = srcOperationNode.getParent().getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(LANG2.FUNCTION_KEYWORD)) {
            	dstOperationNode = dstOperationNode.getParent().getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getParent() != null && srcOperationNode.getType().name.equals(LANG1.MODIFIERS)) {
                srcOperationNode = srcOperationNode.getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getParent() != null && dstOperationNode.getType().name.equals(LANG2.MODIFIERS)) {
                dstOperationNode = dstOperationNode.getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getParent() != null && srcOperationNode.getParent().getType().name.equals(LANG1.DECORATED_METHOD)) {
                srcOperationNode = srcOperationNode.getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getParent() != null && dstOperationNode.getParent().getType().name.equals(LANG2.DECORATED_METHOD)) {
                dstOperationNode = dstOperationNode.getParent();
            }
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(LANG1.ERROR) && srcOperationNode.getChildren().size() > 0 &&
            		srcOperationNode.getChildren().get(0).getType().name.equals(LANG1.METHOD_DECLARATION)) {
            	srcOperationNode = srcOperationNode.getChildren().get(0);
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(LANG2.ERROR) && dstOperationNode.getChildren().size() > 0 &&
            		dstOperationNode.getChildren().get(0).getType().name.equals(LANG2.METHOD_DECLARATION)) {
            	dstOperationNode = dstOperationNode.getChildren().get(0);
            }
            if (srcOperationNode == null || !(srcOperationNode.getType().name.equals(LANG1.METHOD_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.SECONDARY_CONSTRUCTOR) || srcOperationNode.getType().name.equals(LANG1.DECORATED_METHOD) || srcOperationNode.getType().name.equals(LANG1.ANNOTATION_TYPE_MEMBER_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.GETTER) || srcOperationNode.getType().name.equals(LANG1.SETTER))) return;
            if (dstOperationNode == null || !(dstOperationNode.getType().name.equals(LANG2.METHOD_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.SECONDARY_CONSTRUCTOR) || dstOperationNode.getType().name.equals(LANG2.DECORATED_METHOD) || dstOperationNode.getType().name.equals(LANG2.ANNOTATION_TYPE_MEMBER_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.GETTER) || dstOperationNode.getType().name.equals(LANG2.SETTER))) return;
            new JavaDocMatcher(optimizationData, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), umlOperationBodyMapper.getJavadocDiff(), LANG1, LANG2)
                    .match(srcOperationNode, dstOperationNode, mappingStore);
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
            }
        } else {
            //Static Initializers
            if(srcTree.getType().name.equals(LANG1.MODULE))
                srcOperationNode = srcTree;
            else
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo(), LANG1);
            if(dstTree.getType().name.equals(LANG2.MODULE))
                dstOperationNode = dstTree;
            else
                dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo(), LANG2);
            if (srcOperationNode != null && dstOperationNode != null) {
                if (srcOperationNode.getType().name.equals(LANG1.INITIALIZER) && dstOperationNode.getType().name.equals(LANG2.INITIALIZER)) {
                    mappingStore.addMapping(srcOperationNode, dstOperationNode);
                    //static keyword
                    if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer && umlOperationBodyMapper.getContainer2() instanceof UMLInitializer) {
                        UMLInitializer initializer1 = (UMLInitializer)umlOperationBodyMapper.getContainer1();
                        UMLInitializer initializer2 = (UMLInitializer)umlOperationBodyMapper.getContainer2();
                        Tree srcInitKeyword = TreeUtilFunctions.findChildByType(srcOperationNode, LANG1.INIT_KEYWORD);
                        Tree dstInitKeyword = TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.INIT_KEYWORD);
                        if (srcInitKeyword != null && dstInitKeyword != null) {
                            mappingStore.addMapping(srcInitKeyword, dstInitKeyword);
                        }
                        if (initializer1.isStatic() && initializer2.isStatic()) {
                            Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode, LANG1.MODIFIER);
                            Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode, LANG2.MODIFIER);
                            if (srcModifier != null && dstModifier != null)
                                mappingStore.addMapping(srcModifier, dstModifier);
                        }
                        if (initializer1.getJavadoc() != null && initializer2.getJavadoc() != null) {
                            //Javadoc
                            new JavaDocMatcher(optimizationData, initializer1.getJavadoc(), initializer2.getJavadoc(), umlOperationBodyMapper.getJavadocDiff(), LANG1, LANG2)
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
                Tree srcAffectationKeyword = TreeUtilFunctions.findChildByType(srcOperationFunctionBody, LANG1.AFFECTATION_OPERATOR);
                Tree dstAffectationKeyword = TreeUtilFunctions.findChildByType(dstOperationFunctionBody, LANG2.AFFECTATION_OPERATOR);
                mappingStore.addMapping(srcAffectationKeyword, dstAffectationKeyword);
            }
            new BodyMapperMatcher(optimizationData, umlOperationBodyMapper, isPartOfExtractedMethod, LANG1, LANG2).match(srcOperationNode, dstOperationNode, mappingStore);
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
        com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
        if (matched != null) {
            if(matched.first.getChildren().size() > 0 && matched.second.getChildren().size() > 0) {
                mappingStore.addMappingRecursively(matched.first,matched.second);
            }
            else {
                mappingStore.addMapping(matched.first,matched.second);
            }
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PRIMITIVE_TYPE,LANG2.PRIMITIVE_TYPE);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.BLOCK,LANG2.BLOCK);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION_BODY,LANG2.FUNCTION_BODY);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        if(Constants.isCrossLanguage(LANG1, LANG2)) {
            matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.BLOCK,LANG2.FUNCTION_BODY);
            if (matched != null) {
                mappingStore.addMapping(matched.first,matched.second);
                if(matched.second.getChildren().size() > 0 && matched.second.getChild(0).getType().name.equals(LANG2.STATEMENTS)) {
                    mappingStore.addMapping(matched.first,matched.second.getChild(0));
                }
            }
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION_KEYWORD,LANG2.FUNCTION_KEYWORD);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.CONSTRUCTOR_KEYWORD,LANG2.CONSTRUCTOR_KEYWORD);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION_PARAMETERS,LANG2.FUNCTION_PARAMETERS);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.MODIFIERS,LANG2.MODIFIERS);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }

        if (umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation2() != null) {
            if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
                new SameModifierMatcher(LANG1, LANG2, LANG1.STATIC).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
                new SameModifierMatcher(LANG1, LANG2, LANG1.FINAL).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
                new SameModifierMatcher(LANG1, LANG2, LANG1.ABSTRACT).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isSynchronized() && umlOperationBodyMapper.getOperation2().isSynchronized())
                new SameModifierMatcher(LANG1, LANG2, LANG1.SYNCHRONIZED).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isNative() && umlOperationBodyMapper.getOperation2().isNative())
                new SameModifierMatcher(LANG1, LANG2, LANG1.NATIVE).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isDefault() && umlOperationBodyMapper.getOperation2().isDefault())
                new SameModifierMatcher(LANG1, LANG2, LANG1.DEFAULT).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isStrictfp() && umlOperationBodyMapper.getOperation2().isStrictfp())
                new SameModifierMatcher(LANG1, LANG2, LANG1.STRICTFP).match(srcOperationNode,dstOperationNode,mappingStore);
            if (umlOperationBodyMapper.getOperation1().isInline() && umlOperationBodyMapper.getOperation2().isInline())
                new SameModifierMatcher(LANG1, LANG2, LANG1.INLINE).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.OVERRIDE).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.SUSPEND).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.OPEN).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.OPERATOR).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.INTERNAL).match(srcOperationNode,dstOperationNode,mappingStore);
            new SameModifierMatcher(LANG1, LANG2, LANG1.INFIX).match(srcOperationNode,dstOperationNode,mappingStore);
            String v1 = umlOperationBodyMapper.getOperation1().getVisibility().toString();
            String v2 = umlOperationBodyMapper.getOperation2().getVisibility().toString();
            Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcOperationNode, LANG1.MODIFIER, v1, LANG1);
            Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstOperationNode, LANG2.MODIFIER, v2, LANG2);
            if (tree1 != null && tree2 != null) {
                if(Constants.isCrossLanguage(LANG1, LANG2)) {
                    Tree modifier2 = TreeUtilFunctions.findChildByType(tree2, LANG2.MODIFIER);
                    if(modifier2 != null)
                        mappingStore.addMapping(tree1, modifier2);
                }
                else {
                    mappingStore.addMappingRecursively(tree1,tree2);
                }
            }
        }
    }

    private void processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
        UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
        if (umlOperationDiff == null) return;
        UMLTypeParameterListDiff umlTypeParameterListDiff = umlOperationDiff.getTypeParameterListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : umlTypeParameterListDiff.getCommonTypeParameters()) {
            Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo(), LANG1);
            Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo(), LANG2);
            mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
            if (srcTypeParam.getParent().getType().name.equals(LANG1.TYPE_PARAMETERS) && dstTypeParam.getParent().getType().name.equals(LANG2.TYPE_PARAMETERS)) {
                mappingStore.addMapping(srcTypeParam.getParent(), dstTypeParam.getParent());
            }
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo(), LANG1);
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo(), LANG2);
            if (srcClassAnnotationTree == null || dstClassAnnotationTree == null) continue;
            if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
                mappingStore.addMappingRecursively(srcClassAnnotationTree, dstClassAnnotationTree);
            else {
                new IgnoringCommentsLeafMatcher(LANG1, LANG2).match(srcClassAnnotationTree, dstClassAnnotationTree, mappingStore);
            }
            if(umlOperationDiff.getAnnotationListDiff().getCommonAnnotations().size() > 1 &&
            		srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(LANG1.MODIFIERS) &&
            		dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(LANG2.MODIFIERS)) {
            	mappingStore.addMapping(srcClassAnnotationTree.getParent(), dstClassAnnotationTree.getParent());
            }
        }
        Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
        new KeywordMatcher(LANG1, LANG2, LANG1.THROWS_KEYWORD, LANG2.THROWS_KEYWORD, THROWS_KEYWORD_LABEL).match(srcTree, dstTree, mappingStore);
        if (commonExceptionTypes != null) {
            for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
                Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo(), LANG1);
                Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo(), LANG2);
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        AbstractMap.SimpleEntry<Set<UMLType>, Set<UMLType>> changedExceptionTypes = umlOperationDiff.getChangedExceptionTypes();
        if (changedExceptionTypes != null){
            Iterator<UMLType> keys = changedExceptionTypes.getKey().iterator();
            Iterator<UMLType> values = changedExceptionTypes.getValue().iterator();
            if (keys.hasNext() && values.hasNext())
            {
                Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, keys.next().getLocationInfo(), LANG1);
                Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, values.next().getLocationInfo(), LANG2);
                mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
            }
        }
        VariableDeclarationContainer removedOperation = umlOperationDiff.getRemovedOperation();
        VariableDeclarationContainer addedOperation = umlOperationDiff.getAddedOperation();
        if (removedOperation instanceof UMLOperation && ((UMLOperation)removedOperation).getReturnParameter() != null &&
                addedOperation instanceof UMLOperation && ((UMLOperation)addedOperation).getReturnParameter() != null ) {
            LocationInfo srcLocationInfo = ((UMLOperation)removedOperation).getReturnParameter().getType().getLocationInfo();
            LocationInfo dstLocationInfo = ((UMLOperation)addedOperation).getReturnParameter().getType().getLocationInfo();
            Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo, LANG1);
            Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo, LANG2);
            if (srcNode == null || dstNode == null) return;
            if (srcNode.isIsoStructuralTo(dstNode))
                mappingStore.addMappingRecursively(srcNode,dstNode);
            else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                Tree type1 = srcNode;
                Tree type2 = dstNode.getType().name.equals(LANG2.USER_TYPE) ? dstNode : TreeUtilFunctions.findChildByType(dstNode, LANG2.USER_TYPE);
                if(type1 != null && type2 != null) {
                    mappingStore.addMapping(type1, type2);
                    if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                        mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
                    }
                }
            }
            else {
                new LeafMatcher(LANG1, LANG2).match(srcNode,dstNode,mappingStore);
            }
        }
        if (removedOperation instanceof UMLOperation leftOperation && addedOperation instanceof UMLOperation rightOperation &&
                leftOperation.getReceiverTypeReference() != null && rightOperation.getReceiverTypeReference() != null) {
            LocationInfo srcLocationInfo = leftOperation.getReceiverTypeReference().getLocationInfo();
            LocationInfo dstLocationInfo = rightOperation.getReceiverTypeReference().getLocationInfo();
            Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo, LANG1);
            Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo, LANG2);
            if (srcNode == null || dstNode == null) return;
            if (srcNode.isIsoStructuralTo(dstNode))
                mappingStore.addMappingRecursively(srcNode,dstNode);
            else {
                new LeafMatcher(LANG1, LANG2).match(srcNode,dstNode,mappingStore);
            }
        }
    }

    private void matchThrowKeyword(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        Tree srcThrowKeyword = TreeUtilFunctions.findChildByType(srcTree, LANG1.THROWS_KEYWORD);
        Tree dstThrowKeyword = TreeUtilFunctions.findChildByType(dstTree, LANG2.THROWS_KEYWORD);
        if (srcThrowKeyword != null && dstThrowKeyword != null) {
            mappingStore.addMappingRecursively(srcThrowKeyword, dstThrowKeyword);
        }
    }

    private void processMethodParameters(Tree srcTree, Tree dstTree, Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
        for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
            VariableDeclaration leftVarDecl = matchedPair.getLeft();
            VariableDeclaration rightVarDecl = matchedPair.getRight();
            Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getLocationInfo(),LANG1);
            Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getLocationInfo(),LANG2);
            if (leftTree == null || rightTree == null) return;
            if (leftTree.getType().name.endsWith("_comment")) {
                leftTree = TreeUtilFunctions.findByLocationInfo(srcTree, leftVarDecl.getLocationInfo(), LANG1, LANG1.PARAMETER);
            }
            if (rightTree.getType().name.endsWith("_comment")) {
                rightTree = TreeUtilFunctions.findByLocationInfo(dstTree, rightVarDecl.getLocationInfo(), LANG2, LANG2.PARAMETER);
            }
            if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
                if (TreeUtilFunctions.isIsomorphicTo(rightTree, leftTree))
                    mappingStore.addMappingRecursively(leftTree, rightTree);
                else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                    mappingStore.addMapping(leftTree, rightTree);
                    Tree type1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.SIMPLE_TYPE);
                    Tree type2 = TreeUtilFunctions.findChildByType(rightTree, LANG2.USER_TYPE);
                    if(type1 != null && type2 != null) {
                        mappingStore.addMapping(type1, type2);
                        if(type1.getChildren().size() > 0 && type2.getChildren().size() > 0) {
                            mappingStore.addMapping(type1.getChild(0),type2.getChild(0));
                        }
                    }
                    Tree name1 = TreeUtilFunctions.findChildByType(leftTree, LANG1.SIMPLE_NAME);
                    Tree name2 = TreeUtilFunctions.findChildByType(rightTree, LANG2.SIMPLE_NAME);
                    if(name1 != null && name2 != null) {
                        mappingStore.addMapping(name1, name2);
                    }
                }
                else {
                    new LeafMatcher(LANG1, LANG2).match(leftTree,rightTree,mappingStore);
                    mappingStore.addMapping(leftTree,rightTree);
                }
                if(leftTree.getType().name.equals(LANG1.PARAMETER_MODIFIERS) && rightTree.getType().name.equals(LANG2.PARAMETER_MODIFIERS)) {
                    Tree leftParameter = TreeUtilFunctions.findByLocationInfo(leftTree.getParent(), leftVarDecl.getLocationInfo(), LANG1, LANG1.PARAMETER);
                    Tree rightParameter = TreeUtilFunctions.findByLocationInfo(rightTree.getParent(), rightVarDecl.getLocationInfo(), LANG2, LANG2.PARAMETER);
                    if (TreeUtilFunctions.isIsomorphicTo(leftParameter, rightParameter))
                        mappingStore.addMappingRecursively(leftParameter, rightParameter);
                    else {
                        new LeafMatcher(LANG1, LANG2).match(leftParameter,rightParameter,mappingStore);
                        mappingStore.addMapping(leftParameter,rightParameter);
                    }
                }
                if(leftVarDecl.getInitializer() != null && rightVarDecl.getInitializer() != null) {
                    Tree leftInitializerTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getInitializer().getLocationInfo(),LANG1);
                    Tree rightInitializerTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getInitializer().getLocationInfo(),LANG2);
                    if (leftInitializerTree == null || rightInitializerTree == null) return;
                    if (TreeUtilFunctions.isIsomorphicTo(leftInitializerTree, rightInitializerTree))
                        mappingStore.addMappingRecursively(leftInitializerTree, rightInitializerTree);
                    else {
                        new LeafMatcher(LANG1, LANG2).match(leftInitializerTree, rightInitializerTree,mappingStore);
                        mappingStore.addMapping(leftInitializerTree, rightInitializerTree);
                    }
                    int leftPosition = leftInitializerTree.positionInParent();
                    int rightPosition = rightInitializerTree.positionInParent();
                    if(leftPosition > 0 && rightPosition > 0) {
                    	Tree previousLeft = leftInitializerTree.getParent().getChild(leftPosition-1);
                    	Tree previousRight = rightInitializerTree.getParent().getChild(rightPosition-1);
                    	if(previousLeft.getType().name.equals(LANG1.AFFECTATION_OPERATOR) && previousRight.getType().name.equals(LANG2.AFFECTATION_OPERATOR)) {
                    		mappingStore.addMapping(previousLeft, previousRight);
                    	}
                    }
                }
            }
        }
    }
}
