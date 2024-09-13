package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.matchers.statement.CompositeMatcher;
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

    public BodyMapperMatcher(UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod) {
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
    }
    public BodyMapperMatcher(OptimizationData optimizationData, UMLOperationBodyMapper bodyMapper, boolean isPartOfExtractedMethod) {
        super(optimizationData);
        this.bodyMapper = bodyMapper;
        this.isPartOfExtractedMethod = isPartOfExtractedMethod;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore,isPartOfExtractedMethod);
    }
    private void processBodyMapper(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        if (bodyMapper.getAnonymousClassDiffs() != null) {
            for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
                new ClassAttrMatcher(optimizationData, anonymousClassDiff).match(srcTree,dstTree,mappingStore);
                for (UMLOperationBodyMapper umlOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                    new MethodMatcher(optimizationData, umlOperationBodyMapper).match(srcTree,dstTree,mappingStore);
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
            new JavaDocMatcher(optimizationData, bodyMapper.getOperation1().getJavadoc(), bodyMapper.getOperation2().getJavadoc(), bodyMapper.getJavadocDiff()).match(srcTree,dstTree,mappingStore);
    }

    private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo());
        //if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
        //{
        //	mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
        //}
        //else
        {
            if (srcStatementNode == null || dstStatementNode == null)
                return;
            if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
                mappingStore.addMapping(srcStatementNode,dstStatementNode);
            if ((srcStatementNode.getType().name.equals(Constants.TRY_STATEMENT) && dstStatementNode.getType().name.equals(Constants.TRY_STATEMENT)) ||
                    (srcStatementNode.getType().name.equals(Constants.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(Constants.CATCH_CLAUSE))) {
                matchBlocks(srcStatementNode, dstStatementNode, mappingStore);
                new CompositeMatcher(abstractCodeMapping).match(srcStatementNode,dstStatementNode,mappingStore);
            } else if (!srcStatementNode.getType().name.equals(Constants.BLOCK) && !dstStatementNode.getType().name.equals(Constants.BLOCK)) {
                new CompositeMatcher(abstractCodeMapping).match(srcStatementNode, dstStatementNode, mappingStore);
            }
        }
    }
    private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
        String searchingType = Constants.BLOCK;
        Pair<Tree, Tree> matched = Helpers.findPairOfType(srcStatementNode,dstStatementNode, searchingType);
        if (matched != null)
            mappingStore.addMapping(matched.first,matched.second);
    }

    private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
        LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
        Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo());
        Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo());
        if (srcStatementNode == null || dstStatementNode == null) {
            System.err.println("Tree not found for " + abstractCodeMapping);
            return;
        }
        if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
            mappingStore.addMapping(srcStatementNode, dstStatementNode);

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
            new LeafMatcher().match(srcStatementNode,dstStatementNode,mappingStore);
            additionallyMatchedStatements(srcTree, dstTree, srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
        }
        optimizeVariableDeclarations(abstractCodeMapping);
        if (!isPartOfExtractedMethod && srcStatementNode.getType().name.equals(Constants.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(Constants.RETURN_STATEMENT)) {
            optimizationData.getSubtreeMappings().addMapping(srcStatementNode,dstStatementNode);
        }
        if (!abstractCodeMapping.getRefactorings().isEmpty()) {
            leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
        }

    }
    private static void additionallyMatchedStatements(Tree srcTree, Tree dstTree, Tree srcStatementNode, Tree dstStatementNode, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        if (abstractCodeMapping != null) {
            for (Replacement replacement : abstractCodeMapping.getReplacements()) {
                if (replacement instanceof CompositeReplacement) {
                    CompositeReplacement compositeReplacement = (CompositeReplacement) replacement;
                    if (!compositeReplacement.getAdditionallyMatchedStatements1().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements1()) {
                            Tree srcAdditionalTree = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeFragment.getLocationInfo());
                            new LeafMatcher().match(srcAdditionalTree, dstStatementNode, mappingStore);
                        }
                    } else if (!compositeReplacement.getAdditionallyMatchedStatements2().isEmpty()) {
                        for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements2()) {
                            Tree dstAdditionalTree = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeFragment.getLocationInfo());
                            new LeafMatcher().match(srcStatementNode, dstAdditionalTree, mappingStore);
                        }
                    }
                }
            }
        }
    }

    private static void leafMappingRefactoringAwareness(Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
        for (Refactoring refactoring : abstractCodeMapping.getRefactorings()) {
            if (refactoring instanceof ExtractVariableRefactoring)
            {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                for (AbstractCodeMapping reference : extractVariableRefactoring.getReferences()) {
                    for (LeafExpression variable : reference.getFragment2().getVariables()) {
                        if (variable.getString().equals(extractVariableRefactoring.getVariableDeclaration().getVariableName())) {
                            Tree referenceNode = TreeUtilFunctions.findByLocationInfo(dstTree, variable.getLocationInfo());
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
