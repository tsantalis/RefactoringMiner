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
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo(),LANG1);
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo(),LANG2);
        //handle case where the parent block has only a single statement and the locationInfo of compositeStatement is identical with the parent block locationInfo in Python
        //the solution uses reflection to obtain the value of Constants value from the CodeElementType constant name
        if (srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.CLASS_BLOCK) && !compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            String astTypeName = compositeStatementObjectMapping.getFragment1().getLocationInfo().getCodeElementType().name();
            try {
                java.lang.reflect.Field publicField = Constants.class.getField(astTypeName);
                String value = (String) publicField.get(LANG1);
                Tree tmp = TreeUtilFunctions.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo(), LANG1, value);
                if(tmp != null)
                    srcStatementNode = tmp;
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                //e.printStackTrace();
            }
        }
        if (dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.CLASS_BLOCK) && !compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) {
            String astTypeName = compositeStatementObjectMapping.getFragment2().getLocationInfo().getCodeElementType().name();
            try {
                java.lang.reflect.Field publicField = Constants.class.getField(astTypeName);
                String value = (String) publicField.get(LANG2);
                Tree tmp = TreeUtilFunctions.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo(), LANG2, value);
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
                Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.ELSE_IF, LANG2.ELSE_IF);
                if (matched != null) {
                    mappingStore.addMapping(matched.first,matched.second);
                    Pair<Tree, Tree> elifs = Helpers.findPairOfType(matched.first,matched.second, LANG1.ELIF_KEYWORD, LANG2.ELIF_KEYWORD);
                    if (elifs != null) {
                        mappingStore.addMapping(elifs.first,elifs.second);
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
            	Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.COLLECTION_ITERATED, LANG2.COLLECTION_ITERATED);
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
                matchBlocks(srcStatementNode, dstStatementNode, mappingStore);
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode,dstStatementNode,mappingStore);
            } else if (!srcStatementNode.getType().name.equals(LANG1.BLOCK) && !dstStatementNode.getType().name.equals(LANG2.BLOCK)) {
                new CompositeMatcher(abstractCodeMapping, LANG1, LANG2).match(srcStatementNode, dstStatementNode, mappingStore);
            }
        }
    }

    private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.BLOCK, LANG2.BLOCK);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
        if (matched == null) {
            Pair<Tree, Tree> blocks = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CLASS_BLOCK, LANG2.CLASS_BLOCK);
            if (blocks != null) {
                mappingStore.addMapping(blocks.first,blocks.second);
            }
            Pair<Tree, Tree> trys = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.TRY_KEYWORD, LANG2.TRY_KEYWORD);
            if (trys != null) {
                mappingStore.addMapping(trys.first,trys.second);
            }
            Pair<Tree, Tree> catches = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.CATCH_CLAUSE, LANG2.CATCH_CLAUSE);
            if (catches != null) {
                mappingStore.addMapping(catches.first,catches.second);
                Pair<Tree, Tree> exceptions = Helpers.findPairOfType(catches.first,catches.second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME);
                if (exceptions != null) {
                    mappingStore.addMapping(exceptions.first,exceptions.second);
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
            if(srcStatementNode.getType().name.equals(LANG1.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(LANG2.CATCH_CLAUSE)) {
            	Pair<Tree, Tree> types = Helpers.findPairOfType(srcStatementNode,dstStatementNode, LANG1.USER_TYPE, LANG2.USER_TYPE);
            	if (types != null) {
                    mappingStore.addMappingRecursively(types.first,types.second);
            	}
            }
        }
    }

    private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo(),LANG1);
        if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.STATEMENTS)) {
            srcStatementNode = srcStatementNode.getChild(0);
        }
        else if(srcStatementNode != null && srcStatementNode.getType().name.equals(LANG1.PROPERTY_DECLARATION_KEYWORD)) {
            srcStatementNode = srcStatementNode.getParent();
        }
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo(),LANG2);
        if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.STATEMENTS)) {
            dstStatementNode = dstStatementNode.getChild(0);
        }
        else if(dstStatementNode != null && dstStatementNode.getType().name.equals(LANG2.PROPERTY_DECLARATION_KEYWORD)) {
            dstStatementNode = dstStatementNode.getParent();
        }
        if (srcStatementNode == null || dstStatementNode == null) {
            System.err.println("Tree not found for " + abstractCodeMapping);
            return;
        }
        if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
            if(srcStatementNode.isIsoStructuralTo(dstStatementNode) && srcTree.getType().name.equals(LANG1.MODULE))
                mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
            else
                mappingStore.addMapping(srcStatementNode, dstStatementNode);

        if(srcStatementNode.getParent().getType().name.equals(LANG1.STATEMENTS) && dstStatementNode.getParent().getType().name.equals(LANG2.STATEMENTS)) {
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
