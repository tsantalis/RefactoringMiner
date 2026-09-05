package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.matchers.statement.CompositeMatcher;
import org.refactoringminer.astDiff.matchers.statement.IgnoringCommentsLeafMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.Helpers;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class BodyMapperMatcher extends OptimizationAwareMatcher {


    protected final UMLOperationBodyMapper bodyMapper;
    protected final boolean isPartOfExtractedMethod;
    protected final Constants LANG1;
    protected final Constants LANG2;

    public BodyMapperMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod, Constants LANG1, Constants LANG2) {
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }
    public BodyMapperMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore,isPartOfExtractedMethod);
    }
    private void processBodyMapper(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        if (bodyMapper.getAnonymousClassDiffs() != null) {
            for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
                new ClassAttrMatcher(optimizationData, anonymousClassDiff, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                for (UMLOperationBodyMapper umlOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                    new MethodMatcher(optimizationData, umlOperationBodyMapper, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
                Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,anonymousClassDiff.getOriginalClass().getLocationInfo(),LANG1,LANG1.OBJECT);
                Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,anonymousClassDiff.getNextClass().getLocationInfo(),LANG2,LANG2.OBJECT);
                if(srcTypeDeclaration != null && dstTypeDeclaration != null) {
                    Tree object1 = srcTypeDeclaration;
                    Tree object2 = dstTypeDeclaration;
                    mappingStore.addMapping(object1,object2);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(object1,object2, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (opening != null) {
                        mappingStore.addMapping(opening.first,opening.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(object1,object2, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (closing != null) {
                        mappingStore.addMapping(closing.first,closing.second);
                    }
                    MethodMatcher.processObjectLiteralWithinMethodCall(object1, object2, mappingStore, LANG1, LANG2);
                }
            }
        }
        Set<AbstractCodeMapping> mappingSet = bodyMapper.getMappings();
        ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
        for (AbstractCodeMapping abstractCodeMapping : mappings) {
            if (abstractCodeMapping instanceof LeafMapping)
                processLeafMapping(srcTree,dstTree,abstractCodeMapping,mappingStore, isPartOfExtractedMethod);
            else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
                processCompositeMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
        }
        if (isPartOfExtractedMethod)
            new JavaDocMatcher(optimizationData, bodyMapper.getContainer1().getJavadoc(), bodyMapper.getContainer2().getJavadoc(), bodyMapper.getJavadocDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);
        new CommentMatcher(optimizationData, bodyMapper.getCommentListDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);

    }

    private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        LocationInfo srcLocationInfo = compositeStatementObjectMapping.getFragment1().getLocationInfo();
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcLocationInfo,LANG1);
        LocationInfo dstLocationInfo = compositeStatementObjectMapping.getFragment2().getLocationInfo();
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstLocationInfo,LANG2);
        if (srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.IF_KEYWORD))
            srcStatementNode = srcStatementNode.getParent();
        if (dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.IF_KEYWORD))
            dstStatementNode = dstStatementNode.getParent();
        if (srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.FOR_KEYWORD))
            srcStatementNode = srcStatementNode.getParent();
        if (dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.FOR_KEYWORD))
            dstStatementNode = dstStatementNode.getParent();
        if (srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.SWITCH_KEYWORD))
            srcStatementNode = srcStatementNode.getParent();
        if (dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.SWITCH_KEYWORD))
            dstStatementNode = dstStatementNode.getParent();
        //handle case where the parent block has only a single statement and the locationInfo of compositeStatement is identical with the parent block locationInfo in Python
        //the solution uses reflection to obtain the value of Constants value from the CodeElementType constant name
        if (srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.CLASS_BLOCK) && !srcLocationInfo.getCodeElementType().equals(CodeElementType.BLOCK)) {
            String astTypeName = srcLocationInfo.getCodeElementType().name();
            try {
                java.lang.reflect.Field publicField = Constants.class.getField(astTypeName);
                String value = (String) publicField.get(LANG1);
                Tree tmp = TreeUtilFunctions.findByLocationInfo(srcTree,srcLocationInfo, LANG1, value);
                if(tmp != null && !tmp.getType().name.equals(value)) {
                    tmp = TreeUtilFunctions.getTreeBetweenPositions(srcTree, srcLocationInfo.getStartOffset(), srcLocationInfo.getEndOffset(), LANG1, value);
                }
                if(tmp != null)
                    srcStatementNode = tmp;
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                //e.printStackTrace();
            }
        }
        if(Constants.isCrossLanguage(LANG1, LANG2) && dstStatementNode.getType().name.equals(LANG2.STATEMENTS) && abstractCodeMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT)) {
            dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstLocationInfo, LANG2, LANG2.TRY_STATEMENT);
        }
        if (dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.CLASS_BLOCK) && !dstLocationInfo.getCodeElementType().equals(CodeElementType.BLOCK)) {
            String astTypeName = dstLocationInfo.getCodeElementType().name();
            try {
                java.lang.reflect.Field publicField = Constants.class.getField(astTypeName);
                String value = (String) publicField.get(LANG2);
                Tree tmp = TreeUtilFunctions.findByLocationInfo(dstTree,dstLocationInfo, LANG2, value);
                if(tmp != null && !tmp.getType().name.equals(value)) {
                    tmp = TreeUtilFunctions.getTreeBetweenPositions(dstTree, dstLocationInfo.getStartOffset(), dstLocationInfo.getEndOffset(), LANG2, value);
                }
                if(tmp != null)
                    dstStatementNode = tmp;
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                //e.printStackTrace();
            }
        }
        {
            if (srcStatementNode == null || dstStatementNode == null)
                return;
            if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name)) {
                mappingStore.addMapping(srcStatementNode,dstStatementNode);
                if(srcStatementNode.getType().name.equals(LANG1.STATEMENT_BLOCK) && dstStatementNode.getType().name.equals(LANG2.STATEMENT_BLOCK)) {
                    Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.COMPOUND_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.COMPOUND_STATEMENT)) {
                    Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getType().name.equals(LANG2.STATEMENTS)) {
                    Tree srcFirstChild = srcStatementNode.getChild(0);
                    Tree dstFirstChild = dstStatementNode.getChild(0);
                    mappingStore.addMapping(srcFirstChild, dstFirstChild);
                    if (srcFirstChild.getType().name.equals(LANG1.WHEN_EXPRESSION) && dstFirstChild.getType().name.equals(LANG1.WHEN_EXPRESSION)) {
                        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcFirstChild,dstFirstChild, LANG1.WHEN_SUBJECT, LANG2.WHEN_SUBJECT);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                    if (srcFirstChild.getType().name.equals(LANG1.FOR_STATEMENT) && dstFirstChild.getType().name.equals(LANG2.FOR_STATEMENT)) {
                        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcFirstChild,dstFirstChild, LANG1.COLLECTION_ITERATED, LANG2.COLLECTION_ITERATED);
                        if (matched != null) {
                            mappingStore.addMapping(matched.first,matched.second);
                        }
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.CONTROL_STRUCTURE_BODY) && dstStatementNode.getType().name.equals(LANG2.CONTROL_STRUCTURE_BODY)) {
                    if(srcStatementNode.getChildren().size() > 0 && dstStatementNode.getChildren().size() > 0) {
                        Tree srcFirstChild = srcStatementNode.getChild(0);
                        Tree dstFirstChild = dstStatementNode.getChild(0);
                        if(abstractCodeMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && srcFirstChild.getType().name.equals(LANG1.IF_STATEMENT)) {
                            srcStatementNode = srcFirstChild;
                        }
                        if(abstractCodeMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && dstFirstChild.getType().name.equals(LANG2.IF_STATEMENT)) {
                            dstStatementNode = dstFirstChild;
                        }
                    }
                }
                if(!isPartOfExtractedMethod && srcStatementNode.getParent().getType().name.equals(LANG1.METHOD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.METHOD_DECLARATION)) {
                    Tree parent1 = srcStatementNode.getParent();
                    Tree parent2 = dstStatementNode.getParent();
                    if(parent1.isIsomorphicTo(parent2)) {
                        mappingStore.addMappingRecursively(parent1, parent2);
                        Tree grandParent1 = parent1.getParent();
                        Tree grandParent2 = parent2.getParent();
                        if(grandParent1.isIsomorphicTo(grandParent2)) {
                            mappingStore.addMappingRecursively(grandParent1, grandParent2);
                        }
                    }
                    else {
                        mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    }
                }
            }
            else if(Constants.isCrossLanguage(LANG1, LANG2)) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                JavaToKotlinMigration.handleCompositeMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2);
                return;
            }
            if (srcStatementNode.getType().name.equals(LANG1.IF_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.IF_STATEMENT)) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //if expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE_IF, LANG2.ELSE_IF);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    Pair<Tree, Tree> elifs = Helpers.findPairOfType(matched.first,matched.second, LANG1.ELIF_KEYWORD, LANG2.ELIF_KEYWORD);
                    if (elifs != null) {
                        mappingStore.addMapping(elifs.first,elifs.second);
                    }
                }
                List<Tree> elseIfs1 = TreeUtilFunctions.findChildrenByTypeRecursively(srcStatementNode, LANG1.ELSE_IF);
                List<Tree> elseIfs2 = TreeUtilFunctions.findChildrenByTypeRecursively(dstStatementNode, LANG2.ELSE_IF);
                if(elseIfs1.size() == elseIfs2.size()) {
                    for(int i=0; i<elseIfs1.size(); i++) {
                        mappingStore.addMapping(elseIfs1.get(i), elseIfs2.get(i));
                        Pair<Tree, Tree> elifs = Helpers.findPairOfType(elseIfs1.get(i), elseIfs2.get(i), LANG1.ELIF_KEYWORD, LANG2.ELIF_KEYWORD);
                        if (elifs != null) {
                            mappingStore.addMapping(elifs.first,elifs.second);
                        }
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE, LANG2.ELSE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    Pair<Tree, Tree> elses = Helpers.findPairOfType(matched.first,matched.second, LANG1.ELSE_KEYWORD, LANG2.ELSE_KEYWORD);
                    if (elses != null) {
                        mappingStore.addMapping(elses.first,elses.second);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.IF_KEYWORD, LANG2.IF_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONSTEXPR, LANG2.CONSTEXPR);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> conditionClauses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITION_CLAUSE, LANG2.CONDITION_CLAUSE);
                if (conditionClauses != null) {
                    mappingStore.addMapping(conditionClauses.first,conditionClauses.second);
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHEN_ENTRY) && dstStatementNode.getType().name.equals(LANG2.WHEN_ENTRY)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARROW, LANG2.ARROW);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHEN_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.WHEN_EXPRESSION)) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //when expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHEN_SUBJECT, LANG2.WHEN_SUBJECT);
                if (matched != null) {
                    //when subject can be a property declaration, thus we need to match it recursively
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.FOR_STATEMENT)) {
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLLECTION_ITERATED, LANG2.COLLECTION_ITERATED);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SUBSCRIPT, LANG2.SUBSCRIPT);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CALL, LANG2.CALL);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DECLARATION, LANG2.DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.VARIABLE_DECLARATION, LANG2.VARIABLE_DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TUPLE_PATTERN, LANG2.TUPLE_PATTERN);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PATTERN_LIST, LANG2.PATTERN_LIST);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.EMPTY_STATEMENT, LANG2.EMPTY_STATEMENT);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SEMICOLON, LANG2.SEMICOLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.UPDATE_EXPRESSION, LANG2.UPDATE_EXPRESSION);
                if (matched != null) {
                    Tree t1 = matched.first;
                    Tree t2 = matched.second;
                    int index1 = t1.getParent().getChildPosition(t1);
                    int index2 = t2.getParent().getChildPosition(t2);
                    if(index1 > 0 && t1.getParent().getChild(index1-1).getType().name.equals(LANG1.SEMICOLON) &&
                            index2 > 0 && t2.getParent().getChild(index2-1).getType().name.equals(LANG2.SEMICOLON)) {
                        Tree tt1 = t1.getParent().getChild(index1-1);
                        Tree tt2 = t2.getParent().getChild(index2-1);
                        mappingStore.addMapping(tt1,tt2);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.LEXICAL_DECLARATION, LANG2.LEXICAL_DECLARATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_RANGE_LOOP) && dstStatementNode.getType().name.equals(LANG2.FOR_RANGE_LOOP)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    int index1 = srcStatementNode.getChildPosition(matched.first);
                    int index2 = dstStatementNode.getChildPosition(matched.second);
                    if(srcStatementNode.getChildren().size() > index1+1 && srcStatementNode.getChild(index1+1).getType().name.equals(LANG1.SIMPLE_NAME) &&
                            dstStatementNode.getChildren().size() > index2+1 && dstStatementNode.getChild(index2+1).getType().name.equals(LANG2.SIMPLE_NAME)) {
                        Tree t1 = srcStatementNode.getChild(index1+1);
                        Tree t2 = dstStatementNode.getChild(index2+1);
                        mappingStore.addMapping(t1,t2);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PLACEHOLDER_TYPE_SPECIFIER, LANG2.PLACEHOLDER_TYPE_SPECIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.STRUCTURED_BINDING_DECLARATOR, LANG2.STRUCTURED_BINDING_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TEMPLATE_TYPE, LANG2.TEMPLATE_TYPE);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_QUALIFIER, LANG2.TYPE_QUALIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.INITIALIZER_LIST, LANG2.INITIALIZER_LIST);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_IDENTIFIER, LANG2.TYPE_IDENTIFIER);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.QUALIFIED_IDENTIFIER, LANG2.QUALIFIED_IDENTIFIER);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.REFERENCE_DECLARATOR, LANG2.REFERENCE_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.POINTER_DECLARATOR, LANG2.POINTER_DECLARATOR);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.BINARY_EXPRESSION, LANG2.BINARY_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.POINTER_EXPRESSION, LANG2.POINTER_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SUBSCRIPT_EXPRESSION, LANG2.SUBSCRIPT_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITIONAL_EXPRESSION, LANG2.CONDITIONAL_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.METHOD_INVOCATION, LANG2.METHOD_INVOCATION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FIELD_EXPRESSION, LANG2.FIELD_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COMPOUND_LITERAL_EXPRESSION, LANG2.COMPOUND_LITERAL_EXPRESSION);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PRIMITIVE_TYPE, LANG2.PRIMITIVE_TYPE);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
            }
            else if (srcStatementNode.getType().name.equals(LANG1.FOR_IN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.FOR_IN_STATEMENT)) {
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FOR_KEYWORD, LANG2.FOR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OF_KEYWORD, LANG2.OF_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.IN_KEYWORD, LANG2.IN_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> objectPattern = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OBJECT_PATTERN, LANG2.OBJECT_PATTERN);
                if (objectPattern != null) {
                    mappingStore.addMappingRecursively(objectPattern.first,objectPattern.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONST_KEYWORD, LANG2.CONST_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.VAR_KEYWORD, LANG2.VAR_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.LET_KEYWORD, LANG2.LET_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARRAY_PATTERN, LANG2.ARRAY_PATTERN);
                if (matched != null) {
                    mappingStore.addMappingRecursively(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.SWITCH_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SWITCH_KEYWORD, LANG2.SWITCH_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> conditionClauses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITION_CLAUSE, LANG2.CONDITION_CLAUSE);
                if (conditionClauses != null) {
                    mappingStore.addMapping(conditionClauses.first,conditionClauses.second);
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SWITCH_BODY, LANG2.SWITCH_BODY);
                if(blocks != null) {
                    mappingStore.addMapping(blocks.first, blocks.second);
                    com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                    if (opening != null) {
                        mappingStore.addMapping(opening.first,opening.second);
                    }
                    com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                    if (closing != null) {
                        mappingStore.addMapping(closing.first,closing.second);
                    }
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_CASE) && dstStatementNode.getType().name.equals(LANG2.SWITCH_CASE)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CASE_KEYWORD, LANG2.CASE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.SWITCH_DEFAULT) && dstStatementNode.getType().name.equals(LANG2.SWITCH_DEFAULT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DEFAULT_KEYWORD, LANG2.DEFAULT_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLON, LANG2.COLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.WHILE_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.WHILE_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHILE_KEYWORD, LANG2.WHILE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> conditionClauses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CONDITION_CLAUSE, LANG2.CONDITION_CLAUSE);
                if (conditionClauses != null) {
                    mappingStore.addMapping(conditionClauses.first,conditionClauses.second);
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(conditionClauses.first,conditionClauses.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if (srcStatementNode.getType().name.equals(LANG1.DO_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.DO_STATEMENT)) {
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.WHILE_KEYWORD, LANG2.WHILE_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.DO_KEYWORD, LANG2.DO_KEYWORD);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                Pair<Tree, Tree> parenthesized = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.PARENTHESIZED_EXPRESSION, LANG2.PARENTHESIZED_EXPRESSION);
                if (parenthesized != null) {
                    mappingStore.addMapping(parenthesized.first,parenthesized.second);
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                    matched = Helpers.findPairOfType(parenthesized.first,parenthesized.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SEMICOLON, LANG2.SEMICOLON);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            }
            else if ((srcStatementNode.getType().name.equals(LANG1.TRY_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.TRY_STATEMENT)) ||
                    (srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE))) {
                if(srcStatementNode.getParent().getType().name.equals(LANG1.FIELD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.FIELD_DECLARATION)) {
                    //try expression is the initializer of a property declaration
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
                else if(srcStatementNode.getParent().getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    new KeywordMatcher(LANG1, LANG2, LANG1.JUMP_KEYWORD, LANG2.JUMP_KEYWORD, "return").match(srcStatementNode.getParent(),dstStatementNode.getParent(),mappingStore);
                }
                matchBlocks(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            } else if (!srcStatementNode.getType().name.equals(LANG1.BLOCK) && !dstStatementNode.getType().name.equals(LANG2.BLOCK)) {
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode, dstStatementNode, mappingStore);
            }
        }
        if (!abstractCodeMapping.getRefactorings().isEmpty()) {
            leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
        }
    }

    public static void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.BLOCK, LANG2.BLOCK);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        if (matched == null) {
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
            if (blocks != null) {
                mappingStore.addMapping(blocks.first,blocks.second);
            }
            blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COMPOUND_STATEMENT, LANG2.COMPOUND_STATEMENT);
            if (blocks != null) {
                mappingStore.addMapping(blocks.first,blocks.second);
                matched = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
                matched = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                }
            }
            Pair<Tree, Tree> trys = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TRY_KEYWORD, LANG2.TRY_KEYWORD);
            if (trys != null) {
                mappingStore.addMapping(trys.first,trys.second);
            }
            Pair<Tree, Tree> catches = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CATCH_CLAUSE, LANG2.CATCH_CLAUSE);
            List<Tree> children1 = new ArrayList<>();
            for(Tree t : srcStatementNode.getChildren()) {
                if(t.getType().name.equals(LANG1.CATCH_CLAUSE))
                    children1.add(t);
            }
            List<Tree> children2 = new ArrayList<>();
            for(Tree t : dstStatementNode.getChildren()) {
                if(t.getType().name.equals(LANG2.CATCH_CLAUSE))
                    children2.add(t);
            }
            if (catches != null) {
                Pair<Tree, Tree> exceptions = Helpers.findPairOfType(catches.first,catches.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                boolean differentExceptionType = exceptions != null && !exceptions.first.getLabel().equals(exceptions.second.getLabel()) && (children1.size() > 1 || children2.size() > 1);
                if(!differentExceptionType) {
                    mappingStore.addMapping(catches.first,catches.second);
                    if (exceptions != null) {
                        mappingStore.addMapping(exceptions.first,exceptions.second);
                    }
                }
                Pair<Tree, Tree> attributeExceptions = Helpers.findPairOfType(catches.first,catches.second, LANG1.ATTRIBUTE_EXCEPTION, LANG2.ATTRIBUTE_EXCEPTION);
                if (attributeExceptions != null) {
                    mappingStore.addMappingRecursively(attributeExceptions.first,attributeExceptions.second);
                }
                Pair<Tree, Tree> catch_blocks = Helpers.findPairOfType(catches.first,catches.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (catch_blocks != null) {
                    mappingStore.addMapping(catch_blocks.first,catch_blocks.second);
                }
            }
            Pair<Tree, Tree> finallys = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FINALLY_CLAUSE, LANG2.FINALLY_CLAUSE);
            if (finallys != null) {
                mappingStore.addMapping(finallys.first,finallys.second);
                Pair<Tree, Tree> finally_keywords = Helpers.findPairOfType(finallys.first,finallys.second, LANG1.FINALLY_KEYWORD, LANG2.FINALLY_KEYWORD);
                if (finally_keywords != null) {
                    mappingStore.addMapping(finally_keywords.first,finally_keywords.second);
                }
                Pair<Tree, Tree> finally_blocks = Helpers.findPairOfType(finallys.first,finallys.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (finally_blocks != null) {
                    mappingStore.addMapping(finally_blocks.first,finally_blocks.second);
                }
            }
            Pair<Tree, Tree> elses = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE, LANG2.ELSE);
            if (elses != null) {
                mappingStore.addMapping(elses.first,elses.second);
                Pair<Tree, Tree> finally_keywords = Helpers.findPairOfType(elses.first,elses.second, LANG1.ELSE_KEYWORD, LANG2.ELSE_KEYWORD);
                if (finally_keywords != null) {
                    mappingStore.addMapping(finally_keywords.first,finally_keywords.second);
                }
                Pair<Tree, Tree> finally_blocks = Helpers.findPairOfType(elses.first,elses.second, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
                if (finally_blocks != null) {
                    mappingStore.addMapping(finally_blocks.first,finally_blocks.second);
                }
            }
            if(srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE)) {
                Pair<Tree, Tree> excepts = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.EXCEPT_KEYWORD, LANG2.EXCEPT_KEYWORD);
                if (excepts != null) {
                    mappingStore.addMapping(excepts.first,excepts.second);
                }
                Pair<Tree, Tree> catchKeywords = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CATCH_KEYWORD, LANG2.CATCH_KEYWORD);
                if (catchKeywords != null) {
                    mappingStore.addMapping(catchKeywords.first,catchKeywords.second);
                }
                Pair<Tree, Tree> asPatterns = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.AS_PATTERN, LANG2.AS_PATTERN);
                if (asPatterns != null) {
                    mappingStore.addMappingRecursively(asPatterns.first,asPatterns.second);
                }
                Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (identifiers != null) {
                    mappingStore.addMapping(identifiers.first,identifiers.second);
                }
                Pair<Tree, Tree> opening = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                if (opening != null) {
                    mappingStore.addMapping(opening.first,opening.second);
                }
                Pair<Tree, Tree> closing = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                if (closing != null) {
                    mappingStore.addMapping(closing.first,closing.second);
                }
                Pair<Tree, Tree> typeAnnotation = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TYPE_ANNOTATION, LANG2.TYPE_ANNOTATION);
                if (typeAnnotation != null) {
                    mappingStore.addMappingRecursively(typeAnnotation.first,typeAnnotation.second);
                }
                Pair<Tree, Tree> types = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.USER_TYPE, LANG2.USER_TYPE);
                if (types != null) {
                    mappingStore.addMappingRecursively(types.first,types.second);
                }
                Pair<Tree,Tree> parameter_lists = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.PARAMETER_LIST,LANG2.PARAMETER_LIST);
                if (parameter_lists != null) {
                    mappingStore.addMapping(parameter_lists.first,parameter_lists.second);
                    opening = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                    if (opening != null) {
                        mappingStore.addMapping(opening.first,opening.second);
                    }
                    closing = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                    if (closing != null) {
                        mappingStore.addMapping(closing.first,closing.second);
                    }
                    Pair<Tree, Tree> ellipsis = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.ELLIPSIS, LANG2.ELLIPSIS);
                    if (ellipsis != null) {
                        mappingStore.addMapping(ellipsis.first,ellipsis.second);
                    }
                    Pair<Tree, Tree> parameterDeclarations = Helpers.findPairOfType(parameter_lists.first,parameter_lists.second, LANG1.PARAMETER_DECLARATION, LANG2.PARAMETER_DECLARATION);
                    if (parameterDeclarations != null) {
                        mappingStore.addMapping(parameterDeclarations.first,parameterDeclarations.second);
                        identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                        if (identifiers != null) {
                            mappingStore.addMapping(identifiers.first,identifiers.second);
                        }
                        types = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.PRIMITIVE_TYPE, LANG2.PRIMITIVE_TYPE);
                        if (types != null) {
                            mappingStore.addMappingRecursively(types.first,types.second);
                        }
                        Pair<Tree, Tree> reference_declarators = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.REFERENCE_DECLARATOR, LANG2.REFERENCE_DECLARATOR);
                        if (reference_declarators != null) {
                            mappingStore.addMappingRecursively(reference_declarators.first,reference_declarators.second);
                        }
                        reference_declarators = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.ABSTRACT_REFERENCE_DECLARATOR, LANG2.ABSTRACT_REFERENCE_DECLARATOR);
                        if (reference_declarators != null) {
                            mappingStore.addMappingRecursively(reference_declarators.first,reference_declarators.second);
                        }
                        Pair<Tree, Tree> qualified_identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.QUALIFIED_IDENTIFIER, LANG2.QUALIFIED_IDENTIFIER);
                        if (qualified_identifiers != null) {
                            mappingStore.addMappingRecursively(qualified_identifiers.first,qualified_identifiers.second);
                        }
                        Pair<Tree, Tree> type_identifiers = Helpers.findPairOfType(parameterDeclarations.first,parameterDeclarations.second, LANG1.TYPE_IDENTIFIER, LANG2.TYPE_IDENTIFIER);
                        if (type_identifiers != null) {
                            mappingStore.addMappingRecursively(type_identifiers.first,type_identifiers.second);
                        }
                    }
                }
            }
        }
    }

    private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
        if(leafMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.PROBLEM_STATEMENT) && leafMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.PROBLEM_STATEMENT)) {
            Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo(),LANG1);
            Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo(),LANG2);
            if (srcStatementNode == null || dstStatementNode == null) {
                System.err.println("Tree not found for " + abstractCodeMapping);
                return;
            }
            if(srcStatementNode.getType().name.endsWith("_statement") && dstStatementNode.getType().name.endsWith("_statement")) {
                
            }
            else {
                while(srcStatementNode.getParent() != null && !srcStatementNode.getType().name.endsWith("_statement") && (!srcStatementNode.getType().name.equals(LANG1.FUNCTION_DECLARATOR) || srcStatementNode.getParent().getType().name.equals(LANG1.FUNCTION_DECLARATOR))) {
                    srcStatementNode = srcStatementNode.getParent();
                }
                while(dstStatementNode.getParent() != null && !dstStatementNode.getType().name.endsWith("_statement") && (!dstStatementNode.getType().name.equals(LANG2.FUNCTION_DECLARATOR) || dstStatementNode.getParent().getType().name.equals(LANG2.FUNCTION_DECLARATOR))) {
                    dstStatementNode = dstStatementNode.getParent();
                }
                if(srcStatementNode.getParent().getType().name.equals(LANG1.ERROR) && dstStatementNode.getParent().getType().name.equals(LANG2.ERROR)) {
                    srcStatementNode = srcStatementNode.getParent();
                    dstStatementNode = dstStatementNode.getParent();
                }
                if(srcStatementNode.getType().name.equals(dstStatementNode.getType().name)) {
                    mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
                    if(srcStatementNode.getParent().getType().name.equals(LANG1.COMPOUND_STATEMENT) && dstStatementNode.getParent().getType().name.equals(LANG2.COMPOUND_STATEMENT)) {
                        mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                    }
                    if(!isPartOfExtractedMethod && srcStatementNode.getParent().getType().name.equals(LANG1.METHOD_DECLARATION) && dstStatementNode.getParent().getType().name.equals(LANG2.METHOD_DECLARATION)) {
                        mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                        com.github.gumtreediff.utils.Pair<Tree,Tree> compounds = Helpers.findPairOfType(srcStatementNode.getParent(), dstStatementNode.getParent(), LANG1.COMPOUND_STATEMENT, LANG2.COMPOUND_STATEMENT);
                        if(compounds != null && compounds.first.isIsomorphicTo(compounds.second)) {
                            mappingStore.addMappingRecursively(compounds.first, compounds.second);
                        }
                    }
                }
            }
        }
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo(),LANG1);
        if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.STATEMENTS)) {
            srcStatementNode = srcStatementNode.getChild(0);
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.PROPERTY_DECLARATION_KEYWORD)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.RETURN_KEYWORD)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.TYPE_QUALIFIER)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.PLACEHOLDER_TYPE_SPECIFIER)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.AUTO)) {
            srcStatementNode = srcStatementNode.getParent();
            if(srcStatementNode.getType().name.equals(LANG1.PLACEHOLDER_TYPE_SPECIFIER)) {
                srcStatementNode = srcStatementNode.getParent();
            }
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.MEMBER_EXPRESSION) && srcStatementNode.getParent().getType().name.equals(LANG1.METHOD_INVOCATION) &&
                srcStatementNode.getParent().getParent().getType().name.equals(LANG1.EXPRESSION_STATEMENT)) {
            srcStatementNode = srcStatementNode.getParent().getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.CONST_KEYWORD) && srcStatementNode.getParent().getType().name.equals(LANG1.LEXICAL_DECLARATION)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.THROW_KEYWORD) && srcStatementNode.getParent().getType().name.equals(LANG1.THROW_STATEMENT)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.CASE_KEYWORD) && srcStatementNode.getParent().getType().name.equals(LANG1.SWITCH_CASE)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.DEFAULT_KEYWORD) && srcStatementNode.getParent().getType().name.equals(LANG1.SWITCH_CASE)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.ERROR)) {
            Tree tmpSrcStatement = TreeUtilFunctions.getParentUntilType(srcStatementNode, LANG1.EXPRESSION_STATEMENT);
            if(tmpSrcStatement != null)
                srcStatementNode = tmpSrcStatement;
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.METHOD_INVOCATION)) {
            Tree tmpSrcStatement = TreeUtilFunctions.getParentUntilType(srcStatementNode, LANG1.EXPRESSION_STATEMENT);
            if(tmpSrcStatement != null)
                srcStatementNode = tmpSrcStatement;
        }
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo(),LANG2);
        if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.STATEMENTS)) {
            dstStatementNode = dstStatementNode.getChild(0);
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.PROPERTY_DECLARATION_KEYWORD)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.RETURN_KEYWORD)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.TYPE_QUALIFIER)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.PLACEHOLDER_TYPE_SPECIFIER)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.AUTO)) {
            dstStatementNode = dstStatementNode.getParent();
            if(dstStatementNode.getType().name.equals(LANG2.PLACEHOLDER_TYPE_SPECIFIER)) {
                dstStatementNode = dstStatementNode.getParent();
            }
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.MEMBER_EXPRESSION) && dstStatementNode.getParent().getType().name.equals(LANG2.METHOD_INVOCATION) &&
                dstStatementNode.getParent().getParent().getType().name.equals(LANG2.EXPRESSION_STATEMENT)) {
            dstStatementNode = dstStatementNode.getParent().getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.CONST_KEYWORD) && dstStatementNode.getParent().getType().name.equals(LANG2.LEXICAL_DECLARATION)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.THROW_KEYWORD) && dstStatementNode.getParent().getType().name.equals(LANG2.THROW_STATEMENT)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.CASE_KEYWORD) && dstStatementNode.getParent().getType().name.equals(LANG2.SWITCH_CASE)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.DEFAULT_KEYWORD) && dstStatementNode.getParent().getType().name.equals(LANG2.SWITCH_CASE)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.ERROR)) {
            Tree tmpDstStatement = TreeUtilFunctions.getParentUntilType(dstStatementNode, LANG2.EXPRESSION_STATEMENT);
            if(tmpDstStatement != null)
                dstStatementNode = tmpDstStatement;
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.METHOD_INVOCATION)) {
            Tree tmpDstStatement = TreeUtilFunctions.getParentUntilType(dstStatementNode, LANG2.EXPRESSION_STATEMENT);
            if(tmpDstStatement != null)
                dstStatementNode = tmpDstStatement;
        }
        if (srcStatementNode == null || dstStatementNode == null) {
            System.err.println("Tree not found for " + abstractCodeMapping);
            return;
        }
        if(srcStatementNode.getType().name.equals(LANG1.CLASS_BLOCK) && !dstStatementNode.getType().name.equals(LANG2.CLASS_BLOCK)) {
            if(srcStatementNode.getChildren().size() == 1 && srcStatementNode.getChild(0).getType().name.equals(dstStatementNode.getType().name)) {
                srcStatementNode = srcStatementNode.getChild(0);
            }
        }
        else if(!srcStatementNode.getType().name.equals(LANG1.CLASS_BLOCK) && dstStatementNode.getType().name.equals(LANG2.CLASS_BLOCK)) {
            if(dstStatementNode.getChildren().size() == 1 && dstStatementNode.getChild(0).getType().name.equals(srcStatementNode.getType().name)) {
                dstStatementNode = dstStatementNode.getChild(0);
            }
        }
        if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
            if(srcStatementNode.getType().name.equals(LANG1.ARROW_FUNCTION) && dstStatementNode.getType().name.equals(LANG2.ARROW_FUNCTION)) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                processArrowFunction(srcStatementNode, dstStatementNode, mappingStore, LANG1, LANG2);
                return;
            }
            else if(containsLambdaMapperWithNonMappedLeaves(abstractCodeMapping, srcStatementNode, dstStatementNode)) {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                Pair<Tree,Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.SEMICOLON,LANG2.SEMICOLON);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                }
                matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode,LANG1.METHOD_INVOCATION,LANG2.METHOD_INVOCATION);
                if(matched != null) {
                    mappingStore.addMapping(matched.first, matched.second);
                    Pair<Tree,Tree> argument_lists = Helpers.findPairOfType(matched.first, matched.second,LANG1.ARGUMENT_LIST,LANG2.ARGUMENT_LIST);
                    if(argument_lists != null) {
                        mappingStore.addMapping(argument_lists.first, argument_lists.second);
                        Pair<Tree, Tree> opening = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
                        if (opening != null) {
                            mappingStore.addMapping(opening.first,opening.second);
                        }
                        Pair<Tree, Tree> closing = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
                        if (closing != null) {
                            mappingStore.addMapping(closing.first,closing.second);
                        }
                        Pair<Tree, Tree> lambdas = Helpers.findPairOfType(argument_lists.first, argument_lists.second, LANG1.LAMBDA_EXPRESSION, LANG2.LAMBDA_EXPRESSION);
                        if (lambdas != null) {
                            mappingStore.addMapping(lambdas.first,lambdas.second);
                            Pair<Tree, Tree> lambda_specifiers = Helpers.findPairOfType(lambdas.first,lambdas.second, LANG1.LAMBDA_CAPTURE_SPECIFIER, LANG2.LAMBDA_CAPTURE_SPECIFIER);
                            if(lambda_specifiers != null) {
                                mappingStore.addMappingRecursively(lambda_specifiers.first,lambda_specifiers.second);
                            }
                            Pair<Tree, Tree> function_declarators = Helpers.findPairOfType(lambdas.first,lambdas.second, LANG1.ABSTRACT_FUNCTION_DECLARATOR, LANG2.ABSTRACT_FUNCTION_DECLARATOR);
                            if(function_declarators != null) {
                                mappingStore.addMappingRecursively(function_declarators.first,function_declarators.second);
                            }
                        }
                    }
                    Pair<Tree,Tree> field_expressions = Helpers.findPairOfType(matched.first, matched.second,LANG1.FIELD_EXPRESSION,LANG2.FIELD_EXPRESSION);
                    if(field_expressions != null) {
                        mappingStore.addMappingRecursively(field_expressions.first, field_expressions.second);
                    }
                }
                for(UMLOperationBodyMapper lambdaMapper : abstractCodeMapping.getLambdaMappers()) {
                    processBodyMapper(srcTree, dstTree, lambdaMapper, mappingStore, isPartOfExtractedMethod);
                }
                return;
            }
            else {
                mappingStore.addMapping(srcStatementNode, dstStatementNode);
                //handle following error node
                int index1 = srcStatementNode.getParent().getChildren().indexOf(srcStatementNode);
                int index2 = dstStatementNode.getParent().getChildren().indexOf(dstStatementNode);
                if(srcStatementNode.getParent().getChildren().size() > index1+1 && srcStatementNode.getParent().getChild(index1+1).getType().name.equals(LANG1.ERROR) &&
                        dstStatementNode.getParent().getChildren().size() > index2+1 && dstStatementNode.getParent().getChild(index2+1).getType().name.equals(LANG2.ERROR)) {
                    Tree t1 = srcStatementNode.getParent().getChild(index1+1);
                    Tree t2 = dstStatementNode.getParent().getChild(index2+1);
                    if(t1.isIsomorphicTo(t2)) {
                        mappingStore.addMappingRecursively(t1, t2);
                    }
                    if(srcStatementNode.getParent().getChildren().size() > index1+2 && srcStatementNode.getParent().getChild(index1+2).getType().name.equals(LANG1.FIELD_DECLARATION) &&
                            dstStatementNode.getParent().getChildren().size() > index2+2 && dstStatementNode.getParent().getChild(index2+2).getType().name.equals(LANG2.FIELD_DECLARATION)) {
                        t1 = srcStatementNode.getParent().getChild(index1+2);
                        t2 = dstStatementNode.getParent().getChild(index2+2);
                        mappingStore.addMapping(t1, t2);
                        if(t1.getChildren().size() == t2.getChildren().size()) {
                            for(int i=0; i<t1.getChildren().size(); i++) {
                                Tree child1 = t1.getChildren().get(i);
                                Tree child2 = t2.getChildren().get(i);
                                if(child1.isIsomorphicTo(child2)) {
                                    mappingStore.addMappingRecursively(child1, child2);
                                }
                            }
                        }
                    }
                }
                //handle parent labeled statement
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.LABELED) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.LABELED)) {
                    mappingStore.addMapping(srcStatementNode.getParent(),dstStatementNode.getParent());
                    Pair<Tree, Tree> identifiers = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.STATEMENT_IDENTIFIER, LANG2.STATEMENT_IDENTIFIER);
                    if (identifiers != null) {
                        mappingStore.addMapping(identifiers.first,identifiers.second);
                    }
                    Pair<Tree, Tree> colons = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.COLON, LANG2.COLON);
                    if (colons != null) {
                        mappingStore.addMapping(colons.first,colons.second);
                    }
                }
                boolean testFunctionCall = (abstractCodeMapping.getFragment1().getString().startsWith("describe(") && abstractCodeMapping.getFragment2().getString().startsWith("describe(")) ||
                        (abstractCodeMapping.getFragment1().getString().startsWith("it(") && abstractCodeMapping.getFragment2().getString().startsWith("it("));
                if(testFunctionCall &&
                        srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENT_BLOCK) &&
                        dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENT_BLOCK) &&
                        srcStatementNode.getParent().getParent() != null && srcStatementNode.getParent().getParent().getType().name.equals(LANG1.FUNCTION_EXPRESSION) &&
                        dstStatementNode.getParent().getParent() != null && dstStatementNode.getParent().getParent().getType().name.equals(LANG2.FUNCTION_EXPRESSION)) {
                    Tree grandParent1 = srcStatementNode.getParent().getParent();
                    Tree grandParent2 = dstStatementNode.getParent().getParent();
                    mappingStore.addMapping(grandParent1, grandParent2);
                    processArrowFunction(grandParent1, grandParent2, mappingStore, LANG1, LANG2);
                    Pair<Tree, Tree> blocks = Helpers.findPairOfType(grandParent1, grandParent2, LANG1.STATEMENT_BLOCK, LANG2.STATEMENT_BLOCK);
                    if(blocks != null) {
                        List<Tree> children1 = blocks.first.getChildren();
                        List<Tree> children2 = blocks.second.getChildren();
                        for(int i=1; i<children1.size(); i++) {
                            Tree child1 = children1.get(i);
                            Tree child2 = children2.get(i);
                            if(child1.isIsomorphicTo(child2)) {
                                mappingStore.addMappingRecursively(child1, child2);
                            }
                            else {
                                break;
                            }
                        }
                    }
                    if(grandParent1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION_ARGUMENTS) && grandParent2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION_ARGUMENTS)) {
                        MethodMatcher.processObjectLiteralWithinMethodCall(grandParent1, grandParent2, mappingStore, LANG1, LANG2);
                    }
                }
                //special handling for multiplication expression following return
                if(srcStatementNode.getType().name.equals(LANG1.JUMP_EXPRESSION) && dstStatementNode.getType().name.equals(LANG2.JUMP_EXPRESSION)) {
                    Tree parent1 = srcStatementNode.getParent();
                    Tree parent2 = dstStatementNode.getParent();
                    index1 = parent1.getChildPosition(srcStatementNode);
                    index2 = parent2.getChildPosition(dstStatementNode);
                    if(index1 < parent1.getChildren().size()-1 && index2 < parent2.getChildren().size()-1) {
                        Tree t1 = parent1.getChild(index1+1);
                        Tree t2 = parent2.getChild(index2+1);
                        if(t1.getType().name.equals(LANG1.MULT_EXPRESSION) && t2.getType().name.equals(LANG2.MULT_EXPRESSION) && t1.isIsomorphicTo(t2)) {
                            mappingStore.addMappingRecursively(t1, t2);
                        }
                    }
                }
                if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.EXPORT_STATEMENT) && dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.EXPORT_STATEMENT)) {
                    mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
                    com.github.gumtreediff.utils.Pair<Tree,Tree> matched = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(),LANG1.EXPORT_KEYWORD,LANG2.EXPORT_KEYWORD);
                    if(matched != null) {
                        mappingStore.addMapping(matched.first, matched.second);
                    }
                    matched = Helpers.findPairOfType(srcStatementNode.getParent(),dstStatementNode.getParent(), LANG1.DEFAULT_KEYWORD, LANG2.DEFAULT_KEYWORD);
                    if (matched != null) {
                        mappingStore.addMapping(matched.first,matched.second);
                    }
                }
                if(srcStatementNode.getType().name.equals(LANG1.SIMPLE_NAME) && dstStatementNode.getType().name.equals(LANG2.SIMPLE_NAME) && srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.ERROR) && dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.ERROR) &&
                        srcStatementNode.getParent().isIsomorphicTo(dstStatementNode.getParent())) {
                    mappingStore.addMappingRecursively(srcStatementNode.getParent(), dstStatementNode.getParent());
                }
            }
        else if(Constants.isCrossLanguage(LANG1, LANG2)) {
            mappingStore.addMapping(srcStatementNode, dstStatementNode);
            JavaToKotlinMigration.handleLeafMapping(mappingStore, srcStatementNode, dstStatementNode, LANG1, LANG2);
        }
        if(srcStatementNode.getParent() != null && srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent() != null && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
            mappingStore.addMapping(srcStatementNode.getParent(), dstStatementNode.getParent());
        }
        boolean _abstractExp = abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression;
        boolean _leafExp = abstractCodeMapping.getFragment1() instanceof LeafExpression || abstractCodeMapping.getFragment2() instanceof LeafExpression;
        boolean _abstractExpWithNonCompositeOwner = _abstractExp;
        if (_abstractExp){
            if (abstractCodeMapping.getFragment1() instanceof AbstractExpression)
                if (((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner() != null
                        && ((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.FOR_STATEMENT))
                    _abstractExpWithNonCompositeOwner = false;
            if (abstractCodeMapping.getFragment2() instanceof AbstractExpression)
                if (((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner() != null
                        && ((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.FOR_STATEMENT))
                    _abstractExpWithNonCompositeOwner = false;
        }
        if (_abstractExpWithNonCompositeOwner || _leafExp) {
            optimizationData.getLastStepMappings().add(abstractCodeMapping);
        } else {
            new IgnoringCommentsLeafMatcher(LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            additionallyMatchedStatements(srcTree, dstTree, srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
        }
        optimizeVariableDeclarations(abstractCodeMapping);
        if (!isPartOfExtractedMethod && srcStatementNode.getType().name.equals(LANG1.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(LANG2.RETURN_STATEMENT)) {
            optimizationData.getSubtreeMappings().addMapping(srcStatementNode,dstStatementNode);
        }
        if (!abstractCodeMapping.getRefactorings().isEmpty()) {
            leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
        }
    }

    private boolean containsLambdaMapperWithNonMappedLeaves(AbstractCodeMapping abstractCodeMapping, Tree srcStatementNode, Tree dstStatementNode) {
        if(abstractCodeMapping.getLambdaMappers().size() > 0 && !srcStatementNode.isIsoStructuralTo(dstStatementNode)) {
            for(UMLOperationBodyMapper lambdaMapper : abstractCodeMapping.getLambdaMappers()) {
                if(lambdaMapper.getParentMapper() !=  null && lambdaMapper.getParentMapper().getParentMapper() != null)
                    continue;
                int nonMappedT2 = lambdaMapper.getNonMappedLeavesT2().size();
                for(AbstractCodeFragment fragment2 : lambdaMapper.getNonMappedLeavesT2()) {
                    for(UMLOperationBodyMapper childMapper : lambdaMapper.getParentMapper().getChildMappers()) {
                        if(fragment2.getLocationInfo().subsumes(childMapper.getOperationInvocation().getLocationInfo())) {
                            nonMappedT2--;
                            break;
                        }
                    }
                    if(fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
                        nonMappedT2--;
                    }
                }
                if(lambdaMapper.getNonMappedLeavesT1().size() > 0 && nonMappedT2 > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void processArrowFunction(Tree srcStatementNode, Tree dstStatementNode,
            ExtendedMultiMappingStore mappingStore, Constants LANG1, Constants LANG2) {
        Pair<Tree, Tree> formal_parameters = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FORMAL_PARAMETERS, LANG2.FORMAL_PARAMETERS);
        if(formal_parameters != null && formal_parameters.first.isIsoStructuralTo(formal_parameters.second)) {
            mappingStore.addMappingRecursively(formal_parameters.first, formal_parameters.second);
        }
        else if(formal_parameters != null) {
            mappingStore.addMapping(formal_parameters.first, formal_parameters.second);
            Pair<Tree, Tree> open_parenthesis = Helpers.findPairOfType(formal_parameters.first, formal_parameters.second, LANG1.OPENING_PARENTHESIS, LANG2.OPENING_PARENTHESIS);
            if(open_parenthesis != null) {
                mappingStore.addMapping(open_parenthesis.first, open_parenthesis.second);
            }
            Pair<Tree, Tree> close_parenthesis = Helpers.findPairOfType(formal_parameters.first, formal_parameters.second, LANG1.CLOSING_PARENTHESIS, LANG2.CLOSING_PARENTHESIS);
            if(close_parenthesis != null) {
                mappingStore.addMapping(close_parenthesis.first, close_parenthesis.second);
            }
        }
        Pair<Tree, Tree> arrows = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ARROW_TOKEN, LANG2.ARROW_TOKEN);
        if(arrows != null) {
            mappingStore.addMapping(arrows.first, arrows.second);
        }
        Pair<Tree, Tree> functions = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.FUNCTION, LANG2.FUNCTION);
        if(functions != null) {
            mappingStore.addMapping(functions.first, functions.second);
        }
        Pair<Tree, Tree> async = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ASYNC_KEYWORD, LANG2.ASYNC_KEYWORD);
        if(async != null) {
            mappingStore.addMapping(async.first, async.second);
        }
        Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.STATEMENT_BLOCK, LANG2.STATEMENT_BLOCK);
        if(blocks != null) {
            mappingStore.addMapping(blocks.first, blocks.second);
            com.github.gumtreediff.utils.Pair<Tree,Tree> opening = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.OPENING_CURLY_BRACE, LANG2.OPENING_CURLY_BRACE);
            if (opening != null) {
                mappingStore.addMapping(opening.first,opening.second);
            }
            com.github.gumtreediff.utils.Pair<Tree,Tree> closing = Helpers.findPairOfType(blocks.first,blocks.second, LANG1.CLOSING_CURLY_BRACE, LANG2.CLOSING_CURLY_BRACE);
            if (closing != null) {
                mappingStore.addMapping(closing.first,closing.second);
            }
        }
    }

    private void additionallyMatchedStatements(Tree srcTree, Tree dstTree, Tree srcStatementNode, Tree dstStatementNode, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        if (abstractCodeMapping != null) {
            for (Replacement replacement : abstractCodeMapping.getReplacements()) {
                if (replacement instanceof CompositeReplacement) {
                    CompositeReplacement compositeReplacement = (CompositeReplacement) replacement;
                    if (!compositeReplacement.getAdditionallyMatchedStatements1().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements1()) {
                            Tree srcAdditionalTree = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeFragment.getLocationInfo(), LANG1);
                            new LeafMatcher(LANG1, LANG2).match(srcAdditionalTree, dstStatementNode, mappingStore);
                        }
                    } else if (!compositeReplacement.getAdditionallyMatchedStatements2().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements2()) {
                            Tree dstAdditionalTree = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeFragment.getLocationInfo(), LANG2);
                            new LeafMatcher(LANG1, LANG2).match(srcStatementNode, dstAdditionalTree, mappingStore);
                        }
                    }
                }
            }
        }
    }

    private void leafMappingRefactoringAwareness(Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        for (Refactoring refactoring : abstractCodeMapping.getRefactorings()) {
            if (refactoring instanceof ExtractVariableRefactoring)
            {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                for (AbstractCodeMapping reference : extractVariableRefactoring.getReferences()) {
                    for (LeafExpression variable : reference.getFragment2().getVariables()) {
                        if (variable.getString().equals(extractVariableRefactoring.getVariableDeclaration().getVariableName())) {
                            Tree referenceNode = TreeUtilFunctions.findByLocationInfo(dstTree, variable.getLocationInfo(), LANG2);
                            if (referenceNode != null)
                            {
                                if (!referenceNode.getChildren().isEmpty()){
                                    referenceNode = referenceNode.getChild(0);
                                }
                                if (mappingStore.isDstMapped(referenceNode) && !mappingStore.isDstMultiMapped(referenceNode)) {
                                    Tree tempSrc = mappingStore.getSrcs(referenceNode).iterator().next();
                                    mappingStore.removeMapping(tempSrc, referenceNode);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void optimizeVariableDeclarations(AbstractCodeMapping abstractCodeMapping) {
        List<VariableDeclaration> variableDeclarations1 = abstractCodeMapping.getFragment1().getVariableDeclarations();
        List<VariableDeclaration> variableDeclarations2 = abstractCodeMapping.getFragment2().getVariableDeclarations();
        if (variableDeclarations1.size() == 1 && variableDeclarations2.isEmpty()){
            if (variableDeclarations1.get(0).getInitializer() != null)
                if (abstractCodeMapping.getFragment2().toString().contains(variableDeclarations1.get(0).getInitializer().toString()))
                    optimizationData.getLastStepMappings().add(new LeafMapping(variableDeclarations1.get(0).getInitializer(), abstractCodeMapping.getFragment2(),null,null));
        }
        if (variableDeclarations1.isEmpty() && variableDeclarations2.size() == 1){
            if (variableDeclarations2.get(0).getInitializer() != null)
                if (abstractCodeMapping.getFragment1().toString().contains(variableDeclarations2.get(0).getInitializer().toString()))
                    optimizationData.getLastStepMappings().add(new LeafMapping(abstractCodeMapping.getFragment1(),variableDeclarations2.get(0).getInitializer(),null,null));
        }
    }
}
