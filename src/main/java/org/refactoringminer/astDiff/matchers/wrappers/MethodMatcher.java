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
            if (srcOperationNode != null && srcOperationNode.getType().name.equals(LANG1.FUNCTION)) {
                srcOperationNode = srcOperationNode.getParent();
            }
            if (dstOperationNode != null && dstOperationNode.getType().name.equals(LANG2.FUNCTION)) {
                dstOperationNode = dstOperationNode.getParent();
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
            if (srcOperationNode == null || !(srcOperationNode.getType().name.equals(LANG1.METHOD_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.SECONDARY_CONSTRUCTOR) || srcOperationNode.getType().name.equals(LANG1.DECORATED_METHOD) || srcOperationNode.getType().name.equals(LANG1.ANNOTATION_TYPE_MEMBER_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.GETTER) || srcOperationNode.getType().name.equals(LANG1.SETTER) || srcOperationNode.getType().name.equals(LANG1.LEXICAL_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.METHOD_DEFINITION) || srcOperationNode.getType().name.equals(LANG1.METHOD_SIGNATURE) || srcOperationNode.getType().name.equals(LANG1.FUNCTION_SIGNATURE) || srcOperationNode.getType().name.equals(LANG1.GENERATOR_FUNCTION_DECLARATION) || srcOperationNode.getType().name.equals(LANG1.PAIR) || srcOperationNode.getType().name.equals(LANG1.FUNCTION_EXPRESSION) || srcOperationNode.getType().name.equals(LANG1.VARIABLE_DECLARATION))) return;
            if (dstOperationNode == null || !(dstOperationNode.getType().name.equals(LANG2.METHOD_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.SECONDARY_CONSTRUCTOR) || dstOperationNode.getType().name.equals(LANG2.DECORATED_METHOD) || dstOperationNode.getType().name.equals(LANG2.ANNOTATION_TYPE_MEMBER_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.GETTER) || dstOperationNode.getType().name.equals(LANG2.SETTER) || dstOperationNode.getType().name.equals(LANG2.LEXICAL_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.METHOD_DEFINITION) || dstOperationNode.getType().name.equals(LANG2.METHOD_SIGNATURE) || dstOperationNode.getType().name.equals(LANG2.FUNCTION_SIGNATURE) || dstOperationNode.getType().name.equals(LANG2.GENERATOR_FUNCTION_DECLARATION) || dstOperationNode.getType().name.equals(LANG2.PAIR) || dstOperationNode.getType().name.equals(LANG2.FUNCTION_EXPRESSION) || dstOperationNode.getType().name.equals(LANG2.VARIABLE_DECLARATION))) return;
            new JavaDocMatcher(optimizationData, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), umlOperationBodyMapper.getJavadocDiff(), LANG1, LANG2)
                    .match(srcOperationNode, dstOperationNode, mappingStore);
            mappingStore.addMapping(srcOperationNode, dstOperationNode);
            if(srcOperationNode.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT) && dstOperationNode.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT)) {
                mappingStore.addMapping(srcOperationNode.getParent(), dstOperationNode.getParent());
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode.getParent(),dstOperationNode.getParent(),LANG1.EXPORT_KEYWORD,LANG2.EXPORT_KEYWORD);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcOperationNode.getParent(),dstOperationNode.getParent(),LANG1.DEFAULT_KEYWORD,LANG2.DEFAULT_KEYWORD);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.LEXICAL_DECLARATION) && dstOperationNode.getType().name.equals(LANG1.LEXICAL_DECLARATION)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.VARIABLE_DECLARATOR,LANG2.VARIABLE_DECLARATOR);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(matched.first,matched.second,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first, identifiers.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> equals = Helpers.findPairOfType(matched.first,matched.second,LANG1.EQUAL_OPERATOR,LANG2.EQUAL_OPERATOR);
                    if (equals != null) {
                        mappingStore.addMapping(equals.first, equals.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> arrowFunctions = Helpers.findPairOfType(matched.first,matched.second,LANG1.ARROW_FUNCTION,LANG2.ARROW_FUNCTION);
                    if(arrowFunctions != null) {
                        mappingStore.addMapping(arrowFunctions.first, arrowFunctions.second);
                        BodyMapperMatcher.processArrowFunction(arrowFunctions.first, arrowFunctions.second, mappingStore, LANG1, LANG2);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> typeAnnotations = Helpers.findPairOfType(matched.first,matched.second,LANG1.TYPE_ANNOTATION,LANG2.TYPE_ANNOTATION);
                    if(typeAnnotations != null) {
                        mappingStore.addMappingRecursively(typeAnnotations.first, typeAnnotations.second);
                    }
                }
                matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.CONST_KEYWORD,LANG2.CONST_KEYWORD);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SEMICOLON,LANG2.SEMICOLON);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.VARIABLE_DECLARATION) && dstOperationNode.getType().name.equals(LANG1.VARIABLE_DECLARATION)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.VARIABLE_DECLARATOR,LANG2.VARIABLE_DECLARATOR);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(matched.first,matched.second,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first, identifiers.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> equals = Helpers.findPairOfType(matched.first,matched.second,LANG1.EQUAL_OPERATOR,LANG2.EQUAL_OPERATOR);
                    if (equals != null) {
                        mappingStore.addMapping(equals.first, equals.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> arrowFunctions = Helpers.findPairOfType(matched.first,matched.second,LANG1.ARROW_FUNCTION,LANG2.ARROW_FUNCTION);
                    if(arrowFunctions != null) {
                        mappingStore.addMapping(arrowFunctions.first, arrowFunctions.second);
                        BodyMapperMatcher.processArrowFunction(arrowFunctions.first, arrowFunctions.second, mappingStore, LANG1, LANG2);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> functionExpressions = Helpers.findPairOfType(matched.first,matched.second,LANG1.FUNCTION_EXPRESSION,LANG2.FUNCTION_EXPRESSION);
                    if(functionExpressions != null) {
                        mappingStore.addMapping(functionExpressions.first, functionExpressions.second);
                        processMethodSignature(functionExpressions.first, functionExpressions.second, umlOperationBodyMapper, mappingStore);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> typeAnnotations = Helpers.findPairOfType(matched.first,matched.second,LANG1.TYPE_ANNOTATION,LANG2.TYPE_ANNOTATION);
                    if(typeAnnotations != null) {
                        mappingStore.addMappingRecursively(typeAnnotations.first, typeAnnotations.second);
                    }
                }
                matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.VAR_KEYWORD,LANG2.VAR_KEYWORD);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SEMICOLON,LANG2.SEMICOLON);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.PAIR) && dstOperationNode.getType().name.equals(LANG1.PAIR)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PROPERTY_IDENTIFIER,LANG2.PROPERTY_IDENTIFIER);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first, identifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> colons = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.COLON,LANG2.COLON);
                if (colons != null) {
                    mappingStore.addMapping(colons.first, colons.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> arrowFunctions = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.ARROW_FUNCTION,LANG2.ARROW_FUNCTION);
                if(arrowFunctions != null) {
                    mappingStore.addMapping(arrowFunctions.first, arrowFunctions.second);
                    BodyMapperMatcher.processArrowFunction(arrowFunctions.first, arrowFunctions.second, mappingStore, LANG1, LANG2);
                }
                int index1 = srcOperationNode.getParent().getChildPosition(srcOperationNode);
                int index2 = dstOperationNode.getParent().getChildPosition(dstOperationNode);
                if(srcOperationNode.getParent().getChildren().size() > index1+1 && srcOperationNode.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                        dstOperationNode.getParent().getChildren().size() > index2+1 && dstOperationNode.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                    Tree t1 = srcOperationNode.getParent().getChild(index1+1);
                    Tree t2 = dstOperationNode.getParent().getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.METHOD_DEFINITION) && dstOperationNode.getType().name.equals(LANG1.METHOD_DEFINITION)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PROPERTY_IDENTIFIER,LANG2.PROPERTY_IDENTIFIER);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first, identifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> private_identifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PRIVATE_PROPERTY_IDENTIFIER,LANG2.PRIVATE_PROPERTY_IDENTIFIER);
                if (private_identifiers != null) {
                    mappingStore.addMapping(private_identifiers.first, private_identifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> modifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.ACCESSIBILITY_MODIFIER,LANG2.ACCESSIBILITY_MODIFIER);
                if (modifiers != null) {
                    mappingStore.addMappingRecursively(modifiers.first, modifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> get = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.GET_KEYWORD,LANG2.GET_KEYWORD);
                if (get != null) {
                    mappingStore.addMappingRecursively(get.first, get.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> set = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SET_KEYWORD,LANG2.SET_KEYWORD);
                if (set != null) {
                    mappingStore.addMappingRecursively(set.first, set.second);
                }
                int index1 = srcOperationNode.getParent().getChildPosition(srcOperationNode);
                int index2 = dstOperationNode.getParent().getChildPosition(dstOperationNode);
                if(srcOperationNode.getParent().getChildren().size() > index1+1 && srcOperationNode.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                        dstOperationNode.getParent().getChildren().size() > index2+1 && dstOperationNode.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                    Tree t1 = srcOperationNode.getParent().getChild(index1+1);
                    Tree t2 = dstOperationNode.getParent().getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.FUNCTION_EXPRESSION) && dstOperationNode.getType().name.equals(LANG1.FUNCTION_EXPRESSION)) {
                if(srcOperationNode.getParent().getType().name.equals(LANG1.PAIR) && dstOperationNode.getParent().getType().name.equals(LANG1.PAIR)) {
                    Tree parent1 = srcOperationNode.getParent();
                    Tree parent2 = dstOperationNode.getParent();
                    mappingStore.addMapping(parent1,parent2);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(parent1,parent2,LANG1.PROPERTY_IDENTIFIER,LANG2.PROPERTY_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first, identifiers.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> colons = Helpers.findPairOfType(parent1,parent2,LANG1.COLON,LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first, colons.second);
                    }
                    int index1 = parent1.getParent().getChildPosition(parent1);
                    int index2 = parent2.getParent().getChildPosition(parent2);
                    if(parent1.getParent().getChildren().size() > index1+1 && parent1.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                            parent2.getParent().getChildren().size() > index2+1 && parent2.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                        Tree t1 = parent1.getParent().getChild(index1+1);
                        Tree t2 = parent2.getParent().getChild(index2+1);
                        mappingStore.addMapping(t1,t2);
                    }
                    if(parent1.getParent().getType().name.equals(LANG1.OBJECT) && parent2.getParent().getType().name.equals(LANG2.OBJECT)) {
                        Tree object1 = parent1.getParent();
                        Tree object2 = parent2.getParent();
                        mappingStore.addMapping(object1,object2);
                        com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(object1,object2, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(object1,object2, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                        processObjectLiteralWithinMethodCall(object1, object2, mappingStore, LANG1, LANG2);
                    }
                }
                else if(srcOperationNode.getParent().getType().name.equals(LANG1.ASSIGNMENT_EXPRESSION) && dstOperationNode.getParent().getType().name.equals(LANG1.ASSIGNMENT_EXPRESSION)) {
                    Tree parent1 = srcOperationNode.getParent();
                    Tree parent2 = dstOperationNode.getParent();
                    mappingStore.addMapping(parent1,parent2);
                    if(parent1.getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT) && parent2.getParent().getType().name.equals(LANG2.EXPRESSION_STATEMENT)) {
                        mappingStore.addMapping(parent1.getParent(),parent2.getParent());
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(parent1, parent2,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first, identifiers.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> members = Helpers.findPairOfType(parent1, parent2,LANG1.MEMBER_EXPRESSION,LANG2.MEMBER_EXPRESSION);
                    if (members != null) {
                        mappingStore.addMappingRecursively(members.first, members.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> equals = Helpers.findPairOfType(parent1, parent2,LANG1.EQUAL_OPERATOR,LANG2.EQUAL_OPERATOR);
                    if (equals != null) {
                        mappingStore.addMapping(equals.first, equals.second);
                    }
                    int index1 = parent1.getParent().getChildPosition(parent1);
                    int index2 = parent2.getParent().getChildPosition(parent2);
                    if(parent1.getParent().getChildren().size() > index1+1 && parent1.getParent().getChild(index1+1).getType().name.equals(LANG1.SEMICOLON) &&
                            parent2.getParent().getChildren().size() > index2+1 && parent2.getParent().getChild(index2+1).getType().name.equals(LANG2.SEMICOLON)) {
                        Tree t1 = parent1.getParent().getChild(index1+1);
                        Tree t2 = parent2.getParent().getChild(index2+1);
                        mappingStore.addMapping(t1,t2);
                    }
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.METHOD_SIGNATURE) && dstOperationNode.getType().name.equals(LANG1.METHOD_SIGNATURE)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PROPERTY_IDENTIFIER,LANG2.PROPERTY_IDENTIFIER);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first, identifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> optionals = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.OPTIONAL_KEYWORD,LANG2.OPTIONAL_KEYWORD);
                if (optionals != null) {
                    mappingStore.addMapping(optionals.first, optionals.second);
                }
                int index1 = srcOperationNode.getParent().getChildPosition(srcOperationNode);
                int index2 = dstOperationNode.getParent().getChildPosition(dstOperationNode);
                if(srcOperationNode.getParent().getChildren().size() > index1+1 && srcOperationNode.getParent().getChild(index1+1).getType().name.equals(LANG1.SEMICOLON) &&
                        dstOperationNode.getParent().getChildren().size() > index2+1 && dstOperationNode.getParent().getChild(index2+1).getType().name.equals(LANG2.SEMICOLON)) {
                    Tree t1 = srcOperationNode.getParent().getChild(index1+1);
                    Tree t2 = dstOperationNode.getParent().getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.FUNCTION_SIGNATURE) && dstOperationNode.getType().name.equals(LANG1.FUNCTION_SIGNATURE)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SEMICOLON,LANG2.SEMICOLON);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.METHOD_DECLARATION) && dstOperationNode.getType().name.equals(LANG1.METHOD_DEFINITION)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.SIMPLE_NAME,LANG2.PROPERTY_IDENTIFIER);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first, identifiers.second);
                }
                com.github.gumtreediff.utils.Pair<Tree,Tree> modifiers = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.ACCESSIBILITY_MODIFIER,LANG2.ACCESSIBILITY_MODIFIER);
                if (modifiers != null) {
                    mappingStore.addMappingRecursively(modifiers.first, modifiers.second);
                }
            }
            if(srcOperationNode.getType().name.equals(LANG1.DECORATED_METHOD) && dstOperationNode.getType().name.equals(LANG1.DECORATED_METHOD)) {
                com.github.gumtreediff.utils.Pair<Tree,Tree> function_definitions = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.METHOD_DECLARATION,LANG2.METHOD_DECLARATION);
                if (function_definitions != null) {
                    mappingStore.addMapping(function_definitions.first, function_definitions.second);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> identifiers = Helpers.findPairOfType(function_definitions.first, function_definitions.second,LANG1.SIMPLE_NAME,LANG2.SIMPLE_NAME);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first, identifiers.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> parameters = Helpers.findPairOfType(function_definitions.first, function_definitions.second,LANG1.PARAMETERS,LANG2.PARAMETERS);
                    if (parameters != null) {
                        mappingStore.addMapping(parameters.first, parameters.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> arrows = Helpers.findPairOfType(function_definitions.first, function_definitions.second,LANG1.ARROW_PYTHON,LANG2.ARROW_PYTHON);
                    if (arrows != null) {
                        mappingStore.addMapping(arrows.first, arrows.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> types = Helpers.findPairOfType(function_definitions.first, function_definitions.second,LANG1.TYPE,LANG2.TYPE);
                    if (types != null) {
                        mappingStore.addMappingRecursively(types.first, types.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> blocks = Helpers.findPairOfType(function_definitions.first, function_definitions.second,LANG1.CLASS_BLOCK,LANG2.CLASS_BLOCK);
                    if (blocks != null) {
                        mappingStore.addMapping(blocks.first, blocks.second);
                    }
                }
            }
            }
        } else {
            //Static Initializers
            if(srcTree.getType().name.equals(LANG1.MODULE) || srcTree.getType().name.equals(LANG1.PROGRAM))
                srcOperationNode = srcTree;
            else
                srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo(), LANG1);
            if(dstTree.getType().name.equals(LANG2.MODULE) || dstTree.getType().name.equals(LANG2.PROGRAM))
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
        if (srcOperationNode != null && dstOperationNode != null && !srcOperationNode.getType().name.equals("ERROR") && !dstOperationNode.getType().name.equals("ERROR")) {
            processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
            if(umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation1().getDefaultExpression() != null &&
                    umlOperationBodyMapper.getOperation2() != null && umlOperationBodyMapper.getOperation2().getDefaultExpression() != null) {
                Tree srcOperationFunctionBody = srcOperationNode.getChild(srcOperationNode.getChildren().size()-1);
                Tree dstOperationFunctionBody = dstOperationNode.getChild(dstOperationNode.getChildren().size()-1);
                Tree srcAffectationKeyword = TreeUtilFunctions.findChildByType(srcOperationFunctionBody, LANG1.AFFECTATION_OPERATOR);
                Tree dstAffectationKeyword = TreeUtilFunctions.findChildByType(dstOperationFunctionBody, LANG2.AFFECTATION_OPERATOR);
                if(srcAffectationKeyword != null && dstAffectationKeyword != null)
                    mappingStore.addMapping(srcAffectationKeyword, dstAffectationKeyword);
            }
            if(umlOperationBodyMapper.getImportListDiff() != null) {
                new ImportMatcher(umlOperationBodyMapper.getImportListDiff(), LANG1, LANG2).match(srcOperationNode, dstOperationNode, mappingStore);
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

    public static void processObjectLiteralWithinMethodCall(Tree object1, Tree object2, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        if(object1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION_ARGUMENTS) && object2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION_ARGUMENTS)) {
            Tree arguments1 = object1.getParent();
            Tree arguments2 = object2.getParent();
            mappingStore.addMapping(arguments1,arguments2);
            com.github.gumtreediff.utils.Pair<Tree, Tree> openingParen = Helpers.findPairOfType(arguments1,arguments2, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
            if (openingParen != null) {
                mappingStore.addMapping(openingParen.first,openingParen.second);
            }
            com.github.gumtreediff.utils.Pair<Tree, Tree> closingParen = Helpers.findPairOfType(arguments1,arguments2, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
            if (closingParen != null) {
                mappingStore.addMapping(closingParen.first,closingParen.second);
            }
            //match first argument if it is not an object
            if(!arguments1.getChild(1).getType().name.equals(LANG1.OBJECT) && !arguments2.getChild(1).getType().name.equals(LANG2.OBJECT)) {
                mappingStore.addMappingRecursively(arguments1.getChild(1), arguments2.getChild(1));
                if(arguments1.getChild(2).getType().name.equals(LANG1.COMMA) && arguments2.getChild(2).getType().name.equals(LANG2.COMMA))
                    mappingStore.addMapping(arguments1.getChild(2), arguments2.getChild(2));
            }
            Tree call1 = arguments1.getParent();
            Tree call2 = arguments2.getParent();
            if(call1.getType().name.equals(LANG1.METHOD_INVOCATION) && call2.getType().name.equals(LANG2.METHOD_INVOCATION)) {
                mappingStore.addMapping(call1, call2);
                com.github.gumtreediff.utils.Pair<Tree, Tree> memberExpressions = Helpers.findPairOfType(call1, call2, LANG1.MEMBER_EXPRESSION, LANG2.MEMBER_EXPRESSION);
                if (memberExpressions != null) {
                    mappingStore.addMappingRecursively(memberExpressions.first,memberExpressions.second);
                }
                if(call1.getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT) && call2.getParent().getType().name.equals(LANG2.EXPRESSION_STATEMENT)) {
                    mappingStore.addMapping(call1.getParent(), call2.getParent());
                    com.github.gumtreediff.utils.Pair<Tree,Tree> semicolons = Helpers.findPairOfType(call1.getParent(), call2.getParent(),LANG1.SEMICOLON,LANG2.SEMICOLON);
                    if (semicolons != null) {
                        mappingStore.addMapping(semicolons.first, semicolons.second);
                    }
                }
                else if(call1.getParent().getType().name.equals(LANG1.ASSIGNMENT_EXPRESSION) && call2.getParent().getType().name.equals(LANG2.ASSIGNMENT_EXPRESSION)) {
                    Tree assignment1 = call1.getParent();
                    Tree assignment2 = call2.getParent();
                    mappingStore.addMapping(assignment1, assignment2);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> equals = Helpers.findPairOfType(assignment1, assignment2,LANG1.EQUAL_OPERATOR,LANG2.EQUAL_OPERATOR);
                    if (equals != null) {
                        mappingStore.addMapping(equals.first, equals.second);
                    }
                    memberExpressions = Helpers.findPairOfType(assignment1, assignment2, LANG1.MEMBER_EXPRESSION, LANG2.MEMBER_EXPRESSION);
                    if (memberExpressions != null) {
                        mappingStore.addMappingRecursively(memberExpressions.first,memberExpressions.second);
                    }
                    if(assignment1.getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT) && assignment2.getParent().getType().name.equals(LANG2.EXPRESSION_STATEMENT)) {
                        mappingStore.addMapping(assignment1.getParent(), assignment2.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> semicolons = Helpers.findPairOfType(assignment1.getParent(), assignment2.getParent(),LANG1.SEMICOLON,LANG2.SEMICOLON);
                        if (semicolons != null) {
                            mappingStore.addMapping(semicolons.first, semicolons.second);
                        }
                    }
                }
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
        com.github.gumtreediff.utils.Pair<Tree,Tree> arrows = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.ARROW_PYTHON,LANG2.ARROW_PYTHON);
        if (arrows != null) {
            mappingStore.addMapping(arrows.first, arrows.second);
        }
        com.github.gumtreediff.utils.Pair<Tree,Tree> types = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.TYPE,LANG2.TYPE);
        if (types != null) {
            mappingStore.addMappingRecursively(types.first, types.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PRIMITIVE_TYPE,LANG2.PRIMITIVE_TYPE);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.IMPORT_STAR,LANG2.IMPORT_STAR);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.BLOCK,LANG2.BLOCK);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.CLASS_BLOCK,LANG2.CLASS_BLOCK);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION_BODY,LANG2.FUNCTION_BODY);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.STATEMENT_BLOCK,LANG2.STATEMENT_BLOCK);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
            com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(matched.first,matched.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
            if (opening != null) {
                mappingStore.addMapping(opening.first,opening.second);
            }
            com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(matched.first,matched.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
            if (closing != null) {
                mappingStore.addMapping(closing.first,closing.second);
            }
        }
        if(Constants.isCrossLanguage(LANG1, LANG2)) {
            JavaToKotlinMigration.handleFunctionBodyMapping(mappingStore, srcOperationNode, dstOperationNode, LANG1, LANG2);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION_KEYWORD,LANG2.FUNCTION_KEYWORD);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FUNCTION,LANG2.FUNCTION);
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
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.PARAMETERS,LANG2.PARAMETERS);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
            if(matched.first.getChildren().size() > 0 && matched.second.getChildren().size() > 0) {
                Tree firstParam1 = matched.first.getChild(0);
                Tree firstParam2 = matched.second.getChild(0);
                if(firstParam1.getLabel().equals("self") && firstParam2.getLabel().equals("self")) {
                    mappingStore.addMapping(firstParam1,firstParam2);
                }
            }
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.FORMAL_PARAMETERS,LANG2.FORMAL_PARAMETERS);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
            com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(matched.first,matched.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
            if (opening != null) {
                mappingStore.addMapping(opening.first,opening.second);
            }
            com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(matched.first,matched.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
            if (closing != null) {
                mappingStore.addMapping(closing.first,closing.second);
            }
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode,LANG1.MODIFIERS,LANG2.MODIFIERS);
        if (matched != null) {
            mappingStore.addMapping(matched.first,matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode, LANG1.ASYNC_KEYWORD, LANG2.ASYNC_KEYWORD);
        if(matched != null) {
            mappingStore.addMapping(matched.first, matched.second);
        }
        matched = Helpers.findPairOfType(srcOperationNode,dstOperationNode, LANG1.STATIC, LANG2.STATIC);
        if(matched != null) {
            mappingStore.addMapping(matched.first, matched.second);
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
                    JavaToKotlinMigration.handleModifierMapping(mappingStore, tree1, tree2, LANG1, LANG2);
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
                com.github.gumtreediff.utils.Pair<Tree, Tree> matched = Helpers.findPairOfType(srcTypeParam.getParent(),dstTypeParam.getParent(),LANG1.OPENING_TAG,LANG2.OPENING_TAG);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcTypeParam.getParent(),dstTypeParam.getParent(),LANG1.CLOSING_TAG,LANG2.CLOSING_TAG);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                int index1 = srcTypeParam.getParent().getChildPosition(srcTypeParam);
                int index2 = dstTypeParam.getParent().getChildPosition(dstTypeParam);
                if(srcTypeParam.getParent().getChildren().size() > index1+1 && srcTypeParam.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                        dstTypeParam.getParent().getChildren().size() > index2+1 && dstTypeParam.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                    Tree t1 = srcTypeParam.getParent().getChild(index1+1);
                    Tree t2 = dstTypeParam.getParent().getChild(index2+1);
                    mappingStore.addMapping(t1,t2);
                }
            }
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations()) {
            Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo(), LANG1);
            if(srcClassAnnotationTree.getParent() != null && srcClassAnnotationTree.getParent().getType().name.equals(LANG1.DECORATOR))
                srcClassAnnotationTree = srcClassAnnotationTree.getParent();
            Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo(), LANG2);
            if(dstClassAnnotationTree.getParent() != null && dstClassAnnotationTree.getParent().getType().name.equals(LANG1.DECORATOR))
                dstClassAnnotationTree = dstClassAnnotationTree.getParent();
            if (srcClassAnnotationTree == null || dstClassAnnotationTree == null) continue;
            if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
                mappingStore.addMappingRecursively(srcClassAnnotationTree, dstClassAnnotationTree);
            else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                JavaToKotlinMigration.handleAnnotationMapping(mappingStore, srcClassAnnotationTree, dstClassAnnotationTree, LANG1, LANG2);
            }
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
                JavaToKotlinMigration.handleTypeMapping(mappingStore, srcNode, dstNode, LANG1, LANG2);
            }
            else if(!srcNode.getType().name.equals(LANG1.METHOD_DECLARATION) && !dstNode.getType().name.equals(LANG2.METHOD_DECLARATION)) {
                new LeafMatcher(LANG1, LANG2).match(srcNode,dstNode,mappingStore);
            }
            if((srcNode.getParent().getType().name.equals(LANG1.TYPE_ANNOTATION) || srcNode.getParent().getType().name.equals(LANG1.TYPE_PREDICATE_ANNOTATION)) && 
                    (dstNode.getParent().getType().name.equals(LANG2.TYPE_ANNOTATION) || dstNode.getParent().getType().name.equals(LANG2.TYPE_PREDICATE_ANNOTATION))) {
                mappingStore.addMapping(srcNode.getParent(), dstNode.getParent());
                com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcNode.getParent(),dstNode.getParent(),LANG1.COLON,LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
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
            else if(!srcNode.getType().name.equals(LANG1.METHOD_DECLARATION) && !dstNode.getType().name.equals(LANG2.METHOD_DECLARATION)) {
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
            if(leftTree.getType().name.equals(LANG1.SIMPLE_NAME) && rightTree.getType().name.equals(LANG2.SIMPLE_NAME) &&
                    leftTree.getParent().getType().name.equals(LANG1.REQUIRED_PARAMETER) && rightTree.getParent().getType().name.equals(LANG2.REQUIRED_PARAMETER)) {
                leftTree = leftTree.getParent();
                rightTree = rightTree.getParent();
            }
            if(leftTree.getType().name.equals(LANG1.SIMPLE_NAME) && rightTree.getType().name.equals(LANG2.SIMPLE_NAME) &&
                    leftTree.getParent().getType().name.equals(LANG1.TYPED_DEFAULT_PARAMETER) && rightTree.getParent().getType().name.equals(LANG2.TYPED_DEFAULT_PARAMETER)) {
                leftTree = leftTree.getParent();
                rightTree = rightTree.getParent();
            }
            if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
                if (TreeUtilFunctions.isIsomorphicTo(rightTree, leftTree))
                    mappingStore.addMappingRecursively(leftTree, rightTree);
                else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                    JavaToKotlinMigration.handleParameterMapping(mappingStore, leftTree, rightTree, LANG1, LANG2);
                }
                else {
                    new LeafMatcher(LANG1, LANG2).match(leftTree,rightTree,mappingStore);
                    mappingStore.addMapping(leftTree,rightTree);
                }
                if(leftTree.getParent() != null && rightTree.getParent() != null) {
                    int index1 = leftTree.getParent().getChildPosition(leftTree);
                    int index2 = rightTree.getParent().getChildPosition(rightTree);
                    if(leftTree.getParent().getChildren().size() > index1+1 && leftTree.getParent().getChild(index1+1).getType().name.equals(LANG1.COMMA) &&
                            rightTree.getParent().getChildren().size() > index2+1 && rightTree.getParent().getChild(index2+1).getType().name.equals(LANG2.COMMA)) {
                        Tree t1 = leftTree.getParent().getChild(index1+1);
                        Tree t2 = rightTree.getParent().getChild(index2+1);
                        mappingStore.addMapping(t1,t2);
                    }
                    if(leftTree.getParent().getType().name.equals(LANG1.DICTIONARY_SPLAT_PATTERN) && rightTree.getParent().getType().name.equals(LANG2.DICTIONARY_SPLAT_PATTERN)) {
                        mappingStore.addMapping(leftTree.getParent(),rightTree.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(leftTree.getParent(),rightTree.getParent(),LANG1.SPLAT_DOUBLE,LANG2.SPLAT_DOUBLE);
                        if(matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                    if(leftTree.getParent().getType().name.equals(LANG1.LIST_SPLAT_PATTERN) && rightTree.getParent().getType().name.equals(LANG2.LIST_SPLAT_PATTERN)) {
                        mappingStore.addMapping(leftTree.getParent(),rightTree.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(leftTree.getParent(),rightTree.getParent(),LANG1.SPLAT_SINGLE,LANG2.SPLAT_SINGLE);
                        if(matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                    if(leftTree.getParent().getType().name.equals(LANG1.OBJECT_PATTERN) && rightTree.getParent().getType().name.equals(LANG2.OBJECT_PATTERN)) {
                        mappingStore.addMapping(leftTree.getParent(),rightTree.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(leftTree.getParent(),rightTree.getParent(), LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                        matched = Helpers.findPairOfType(leftTree.getParent(),rightTree.getParent(), LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                        Tree requiredParameter1 = leftTree.getParent().getParent();
                        Tree requiredParameter2 = rightTree.getParent().getParent();
                        if(requiredParameter1 != null && requiredParameter2 != null && requiredParameter1.getType().name.equals(LANG1.REQUIRED_PARAMETER) && requiredParameter2.getType().name.equals(LANG2.REQUIRED_PARAMETER)) {
                            mappingStore.addMapping(requiredParameter1,requiredParameter2);
                            if(requiredParameter1.getChildren().size() > 1 && requiredParameter2.getChildren().size() > 1 &&
                                    requiredParameter1.getChild(1).getType().name.equals(LANG1.TYPE_ANNOTATION) && requiredParameter2.getChild(1).getType().name.equals(LANG2.TYPE_ANNOTATION)) {
                                mappingStore.addMappingRecursively(requiredParameter1.getChild(1), requiredParameter2.getChild(1));
                            }
                        }
                    }
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
                    if(leftInitializerTree.getParent().getType().name.equals(LANG1.DEFAULT_PARAMETER) && rightInitializerTree.getParent().getType().name.equals(LANG2.DEFAULT_PARAMETER)) {
                        mappingStore.addMapping(leftInitializerTree.getParent(), rightInitializerTree.getParent());
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
