package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import gr.uom.java.xmi.diff.MoveCodeRefactoring.Type;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.matchers.statement.BasicTreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.GeneralMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class RefactoringMatcher extends OptimizationAwareMatcher {

    private final List<Refactoring> refactoringList;

    public RefactoringMatcher(OptimizationData optimizationData, List<Refactoring> modelDiffRefactorings, UMLClassBaseDiff baseClassDiff) {
        super(optimizationData);
        this.refactoringList = getClassDiffRefactorings(baseClassDiff, modelDiffRefactorings);
    }

    public RefactoringMatcher(OptimizationData optimizationData, List<Refactoring> refactoringList) {
        super(optimizationData);
        this.refactoringList = refactoringList;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processRefactorings(srcTree,dstTree,refactoringList,mappingStore);
    }

    private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, ExtendedMultiMappingStore mappingStore){
        for (Refactoring refactoring : refactoringList) {
            if (refactoring instanceof ReplaceLoopWithPipelineRefactoring) {
                ReplaceLoopWithPipelineRefactoring replaceLoopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) refactoring;
                if (replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().size() != 1) continue;
                AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().iterator().next();
                List<LambdaExpressionObject> lambdas = next.getLambdas();
                AbstractCodeFragment enhancedFor = null;
                for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore()) {
                    if (abstractCodeFragment instanceof CompositeStatementObject && ((CompositeStatementObject)abstractCodeFragment).isLoop()) {
                        enhancedFor = abstractCodeFragment;
                        break;
                    }
                }
                if (enhancedFor == null) continue;
                Constants LANG1 = new Constants(enhancedFor.getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(next.getLocationInfo().getFilePath());
                for (LambdaExpressionObject lambda : lambdas) {
                    for (VariableDeclaration parameter : lambda.getParameters()) {
                        String variableName = parameter.getVariableName();
                        VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
                        if (variableDeclaration != null) {
                            Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo(),LANG1);
                            Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,parameter.getLocationInfo(),LANG2);
                            new LeafMatcher(LANG1, LANG2).match(srcNode,dstNode,mappingStore);
                        }
                    }
                }
                Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,enhancedFor.getLocationInfo(),LANG1);
                Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,next.getLocationInfo(),LANG2);
                if (srcSt != null && dstSt != null)
                    if (srcSt.getType().name.equals(dstSt.getType().name))
                        mappingStore.addMapping(srcSt,dstSt);
            } else if (refactoring instanceof ReplacePipelineWithLoopRefactoring) {
                ReplacePipelineWithLoopRefactoring replacePipelineWithLoopRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
                if (replacePipelineWithLoopRefactoring.getCodeFragmentsBefore().size() != 1) continue;
                AbstractCodeFragment next = replacePipelineWithLoopRefactoring.getCodeFragmentsBefore().iterator().next();
                List<LambdaExpressionObject> lambdas = next.getLambdas();
                AbstractCodeFragment enhancedFor = null;
                for (AbstractCodeFragment abstractCodeFragment : replacePipelineWithLoopRefactoring.getCodeFragmentsAfter()) {
                    if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT)) {
                        enhancedFor = abstractCodeFragment;
                        break;
                    }
                }
                if( enhancedFor == null) continue;
                Constants LANG1 = new Constants(next.getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(enhancedFor.getLocationInfo().getFilePath());
                for (LambdaExpressionObject lambda : lambdas) {
                    for (VariableDeclaration parameter : lambda.getParameters()) {
                        String variableName = parameter.getVariableName();
                        VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
                        if (variableDeclaration == null)
                            continue;
                        Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,parameter.getLocationInfo(),LANG1);
                        Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,variableDeclaration.getLocationInfo(),LANG2);
                        new LeafMatcher(LANG1, LANG2).match(srcNode,dstNode,mappingStore);
                    }
                }
                Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,next.getLocationInfo(),LANG1);
                Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,enhancedFor.getLocationInfo(),LANG2);
                if (dstSt != null && srcSt != null && srcSt.getType().name.equals(dstSt.getType().name))
                    mappingStore.addMapping(srcSt, dstSt);
            } else if (refactoring instanceof MergeOperationRefactoring) {
                MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                for(UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                    Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                    Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                    new MethodMatcher(optimizationData, bodyMapper, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof SplitOperationRefactoring) {
                SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                for(UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
                    Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                    Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                    new MethodMatcher(optimizationData, bodyMapper, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof ExtractOperationRefactoring) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                new BodyMapperMatcher(optimizationData, bodyMapper, true, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                if(!multipleInstancesWithSameDescription(refactoringList, refactoring))
                    processArgumentMappings(srcTree, dstTree, extractOperationRefactoring.getArgumentMappings());
            } else if (refactoring instanceof InlineOperationRefactoring) {
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                new BodyMapperMatcher(optimizationData, bodyMapper, false, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                if(!multipleInstancesWithSameDescription(refactoringList, refactoring))
                    processArgumentMappings(srcTree, dstTree, inlineOperationRefactoring.getArgumentMappings());
            } else if (refactoring instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                if (!moveCodeRefactoring.getMoveType().equals(Type.MOVE_BETWEEN_FILES)) {
                    UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
                    Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                    Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                    new BodyMapperMatcher(optimizationData, bodyMapper, false, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof ReplaceAnonymousWithLambdaRefactoring) {
            	ReplaceAnonymousWithLambdaRefactoring replaceAnonymousWithLambdaRefactoring = (ReplaceAnonymousWithLambdaRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = replaceAnonymousWithLambdaRefactoring.getBodyMapper();
                Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                new BodyMapperMatcher(optimizationData, bodyMapper, false, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
            } else if (refactoring instanceof ParameterizeTestRefactoring) {
                ParameterizeTestRefactoring parameterizeTestRefactoring = (ParameterizeTestRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = parameterizeTestRefactoring.getBodyMapper();
                Constants LANG1 = new Constants(bodyMapper.getContainer1().getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(bodyMapper.getContainer2().getLocationInfo().getFilePath());
                new BodyMapperMatcher(optimizationData, bodyMapper, false, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
            } else if (refactoring instanceof ExtractVariableRefactoring) {
                ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(extractVariableRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof InlineVariableRefactoring) {
                InlineVariableRefactoring inlineVariableRefactoring = (InlineVariableRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(inlineVariableRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof AssertThrowsRefactoring) {
                AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(assertThrowsRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof InlineAttributeRefactoring) {
                InlineAttributeRefactoring inlineAttributeRefactoring = (InlineAttributeRefactoring) refactoring;
                //Tree srcAttrDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, inlineAttributeRefactoring.getVariableDeclaration().getLocationInfo());
                //for (AbstractCodeMapping reference : inlineAttributeRefactoring.getReferences()) {
                //	Tree dstStatementTree = TreeUtilFunctions.findByLocationInfo(dstTree,reference.getFragment2().getLocationInfo());
                //	new LeafMatcher().match(srcAttrDeclaration,dstStatementTree,mappingStore);
                //}
                optimizationData.getLastStepMappings().addAll(inlineAttributeRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof ExtractAttributeRefactoring) {
                ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) refactoring;
                //Tree dstAttrDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree, extractAttributeRefactoring.getVariableDeclaration().getLocationInfo());
                //for (AbstractCodeMapping reference : extractAttributeRefactoring.getReferences()) {
                //	Tree srcStatementTree = TreeUtilFunctions.findByLocationInfo(srcTree,reference.getFragment1().getLocationInfo());
                //	new LeafMatcher().match(srcStatementTree,dstAttrDeclaration,mappingStore);
                //}
                optimizationData.getLastStepMappings().addAll(extractAttributeRefactoring.getSubExpressionMappings());
                for (UMLAnonymousClassDiff umlAnonymousClassDiff : extractAttributeRefactoring.getAnonymousClassDiffList()) {
                    Constants LANG1 = new Constants(umlAnonymousClassDiff.getOriginalClass().getLocationInfo().getFilePath());
                    Constants LANG2 = new Constants(umlAnonymousClassDiff.getNextClass().getLocationInfo().getFilePath());
                    new AnonymousClassDiffMatcher(optimizationData, umlAnonymousClassDiff, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof MergeVariableRefactoring) {
                MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
                VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
                Constants LANG2 = new Constants(newVariable.getLocationInfo().getFilePath());
                Tree dstVariableDeclaration =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getLocationInfo(),LANG2);

                if (dstVariableDeclaration != null) {
                    List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
                    if (dstChildrenList.isEmpty()) continue; //Might need to investigate what would be the better diff in these cases
                    Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
                    for (VariableDeclaration variableDeclaration : mergedVariables) {
                        Constants LANG1 = new Constants(variableDeclaration.getLocationInfo().getFilePath());
                        Tree srcVariableDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, variableDeclaration.getLocationInfo(), LANG1);
                        if (srcVariableDeclaration != null) {
                            List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
                            if (srcChildrenList.size() > 0) {
                                Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
                                mappingStore.addMapping(srcVarName, dstVarName);
                            }
                        }
                    }
                }
            } else if (refactoring instanceof SplitConditionalRefactoring) {
                SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(splitConditionalRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof MergeConditionalRefactoring) {
                MergeConditionalRefactoring mergeConditionalRefactoring = (MergeConditionalRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(mergeConditionalRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof ReplaceGenericWithDiamondRefactoring) {
                ReplaceGenericWithDiamondRefactoring replaceGenericWithDiamondRefactoring = (ReplaceGenericWithDiamondRefactoring) refactoring;
                optimizationData.getLastStepMappings().addAll(replaceGenericWithDiamondRefactoring.getSubExpressionMappings());
            } else if (refactoring instanceof MergeCatchRefactoring) {
                MergeCatchRefactoring mergeCatchRefactoring = (MergeCatchRefactoring) refactoring;
                Constants LANG2 = new Constants(mergeCatchRefactoring.getNewCatchBlock().getLocationInfo().getFilePath());
                Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,mergeCatchRefactoring.getNewCatchBlock().getLocationInfo(),LANG2);
                for (AbstractCodeFragment eachMerged : mergeCatchRefactoring.getMergedCatchBlocks()) {
                    Constants LANG1 = new Constants(eachMerged.getLocationInfo().getFilePath());
                    Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,eachMerged.getLocationInfo(),LANG1);
                    new GeneralMatcher(eachMerged, mergeCatchRefactoring.getNewCatchBlock(), LANG1, LANG2)
                            .match(srcSubTree,dstSubTree,mappingStore);
                }
            }
            else if (refactoring instanceof RenameVariableRefactoring) {
                RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                VariableDeclaration originalVariable = renameVariableRefactoring.getOriginalVariable();
                VariableDeclaration renamedVariable = renameVariableRefactoring.getRenamedVariable();
                Constants LANG1 = new Constants(originalVariable.getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(renamedVariable.getLocationInfo().getFilePath());
                Tree srcInput = TreeUtilFunctions.findByLocationInfo(srcTree, originalVariable.getLocationInfo(), LANG1);
                Tree dstInput = TreeUtilFunctions.findByLocationInfo(dstTree, renamedVariable.getLocationInfo(), LANG2);
                if (srcInput == null || dstInput == null) continue;
                boolean eligible = true;
                switch (renameVariableRefactoring.getRefactoringType()) {
                    case REPLACE_VARIABLE_WITH_ATTRIBUTE:
                        srcInput = TreeUtilFunctions.getParentUntilType(srcInput, LANG1.VARIABLE_DECLARATION_STATEMENT);
                        dstInput = TreeUtilFunctions.getParentUntilType(dstInput,LANG2.FIELD_DECLARATION);
                        if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
                            continue;
                        break;
                    case REPLACE_ATTRIBUTE_WITH_VARIABLE:
                        srcInput = TreeUtilFunctions.getParentUntilType(srcInput,LANG1.FIELD_DECLARATION);
                        dstInput = TreeUtilFunctions.getParentUntilType(dstInput,LANG2.VARIABLE_DECLARATION_STATEMENT);
                        if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
                            continue;
                        break;
                    case RENAME_PARAMETER:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
                        if (eligible)
                            for (AbstractCodeMapping abstractCodeMapping : renameVariableRefactoring.getReferences())
                                if (abstractCodeMapping instanceof LeafMapping)
                                    findVariablesAndMatch(TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo(), LANG1), TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo(), LANG2), renameVariableRefactoring.getOriginalVariable().getVariableName(), renameVariableRefactoring.getRenamedVariable().getVariableName(), LANG1, LANG2);
                        break;
                    case RENAME_VARIABLE:
                        Set<AbstractCodeMapping> references = renameVariableRefactoring.getReferences();
//                        findVariablesAndMatch(srcInput, dstInput, renameVariableRefactoring.getOriginalVariable().getVariableName(), renameVariableRefactoring.getRenamedVariable().getVariableName());

                        for (AbstractCodeMapping abstractCodeMapping : references) {
                            if (((RenameVariableRefactoring) refactoring).isInsideExtractedOrInlinedMethod() &&
                                    multipleInstancesWithSameDescription(refactoringList,((RenameVariableRefactoring) refactoring).getOperationBefore(),((RenameVariableRefactoring) refactoring).getOperationAfter())) {
                                eligible = false;
                            }
                            if (eligible) {
                                if (abstractCodeMapping instanceof LeafMapping) {
                                    findVariablesAndMatch(TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo(), LANG1), TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo(), LANG2), renameVariableRefactoring.getOriginalVariable().getVariableName(), renameVariableRefactoring.getRenamedVariable().getVariableName(), LANG1, LANG2);
                                }
                            }
                        }
                        eligible = false;
                        break;
                    case PARAMETERIZE_VARIABLE:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();

                        if (!eligible)
                            break;
                        while (srcInput != null && !TreeUtilFunctions.isStatement(srcInput.getType().name, LANG1)) {
                            if (srcInput.getType() == null) break;
                            srcInput = srcInput.getParent();
                        }
                        if (srcInput == null) return;
                        if (TreeUtilFunctions.isStatement(srcInput.getType().name, LANG1)){
                            new LeafMatcher(LANG1, LANG2).match(srcInput,dstInput,mappingStore);
                        }
                        eligible = false;
                        break;
                    case LOCALIZE_PARAMETER:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
                        if (!eligible)
                            break;
                        while (dstInput != null && !TreeUtilFunctions.isStatement(dstInput.getType().name, LANG2) ) {
                            if (dstInput.getType() == null) break;
                            dstInput = dstInput.getParent();
                        }
                        if (dstInput == null) return;
                        if (TreeUtilFunctions.isStatement(dstInput.getType().name, LANG2)){
                            new LeafMatcher(LANG1, LANG2).match(srcInput,dstInput,mappingStore);
                        }
                        eligible = false;
                        break;
                    default:
                        eligible = false;
                }
                if (eligible) new LeafMatcher(LANG1, LANG2).match(
                        srcInput,
                        dstInput,
                        mappingStore);
            }
            else if (refactoring instanceof ModifyClassAnnotationRefactoring)
            {
                ModifyClassAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyClassAnnotationRefactoring) refactoring;
                if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
                    processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
            }
            else if (refactoring instanceof ModifyMethodAnnotationRefactoring)
            {
                ModifyMethodAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
                if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
                    processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
            }
            else if (refactoring instanceof ModifyAttributeAnnotationRefactoring)
            {
                ModifyAttributeAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyAttributeAnnotationRefactoring) refactoring;
                if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
                    processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
            }
            else if (refactoring instanceof ModifyVariableAnnotationRefactoring)
            {
                ModifyVariableAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
                    processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
            }
            else if (refactoring instanceof RenameAttributeRefactoring) {
                RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) refactoring;
                for (AbstractCodeMapping reference : renameAttributeRefactoring.getReferences()) {
                    List<LeafMapping> subExpressionMappings = reference.getSubExpressionMappings();
                    for (LeafMapping subExpressionMapping : subExpressionMappings) {
                        if (belongToDifferentFiles(subExpressionMapping.getFragment1(), renameAttributeRefactoring.getOriginalAttribute()) ||
                                belongToDifferentFiles(subExpressionMapping.getFragment2(), renameAttributeRefactoring.getRenamedAttribute()))
                            continue;
                        Constants LANG1 = new Constants(subExpressionMapping.getFragment1().getLocationInfo().getFilePath());
                        Constants LANG2 = new Constants(subExpressionMapping.getFragment2().getLocationInfo().getFilePath());
                        Tree srcSimpleName = TreeUtilFunctions.findByLocationInfo(srcTree, subExpressionMapping.getFragment1().getLocationInfo(), LANG1, LANG1.SIMPLE_NAME);
                        Tree dstSimpleName = TreeUtilFunctions.findByLocationInfo(dstTree, subExpressionMapping.getFragment2().getLocationInfo(), LANG2, LANG2.SIMPLE_NAME);
                        if (srcSimpleName != null && dstSimpleName != null)
                            optimizationData.getSubtreeMappings().addMapping(srcSimpleName,dstSimpleName);
                    }
                }
            }
            else if (refactoring instanceof InvertConditionRefactoring) {
                InvertConditionRefactoring invertConditionRefactoring = (InvertConditionRefactoring) refactoring;
                Constants LANG1 = new Constants(invertConditionRefactoring.getOriginalConditional().getLocationInfo().getFilePath());
                Constants LANG2 = new Constants(invertConditionRefactoring.getInvertedConditional().getLocationInfo().getFilePath());
                Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,invertConditionRefactoring.getOriginalConditional().getLocationInfo(),LANG1);
                Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,invertConditionRefactoring.getInvertedConditional().getLocationInfo(),LANG2);
//				new CompositeMatcher(invertConditionRefactoring.getOriginalConditional(), )
                new GeneralMatcher(
                        invertConditionRefactoring.getOriginalConditional(),
                        invertConditionRefactoring.getInvertedConditional(), LANG1, LANG2)
                        .match(srcSubTree,dstSubTree,mappingStore);
            }
        }

    }

    private boolean belongToDifferentFiles(LocationInfoProvider lp1, LocationInfoProvider lp2) {
        return !lp1.getLocationInfo().getFilePath().equals(lp2.getLocationInfo().getFilePath());
    }

    private static boolean multipleInstancesWithSameDescription(List<Refactoring> refactoringList, Refactoring refactoring) {
        int count = 0;
        for (Refactoring r : refactoringList) {
            if(r.toString().equals(refactoring.toString()))
                count++;
        }
        return count > 1;
    }
    private void processArgumentMappings(Tree srcTree, Tree dstTree, List<AbstractCodeMapping> argumentMappings) {
        for(AbstractCodeMapping expressionMapping : argumentMappings) {
            Constants LANG1 = new Constants(expressionMapping.getFragment1().getLocationInfo().getFilePath());
            Constants LANG2 = new Constants(expressionMapping.getFragment2().getLocationInfo().getFilePath());
            Tree t1 = TreeUtilFunctions.findByLocationInfo(srcTree,expressionMapping.getFragment1().getLocationInfo(),LANG1);
            Tree t2 = TreeUtilFunctions.findByLocationInfo(dstTree,expressionMapping.getFragment2().getLocationInfo(),LANG2);
            new LeafMatcher(LANG1, LANG2).match(t1,t2,optimizationData.getSubtreeMappings());
        }
    }

    private boolean multipleInstancesWithSameDescription(List<Refactoring> refactoringList, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
        int counter1 = 0;
        int counter2 = 0;
        for (Refactoring r : refactoringList) {
            if (r.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
            {
                if (((ExtractOperationRefactoring)r).getExtractedOperation().equals(operationAfter))
                    counter1++;
            }
            else if (r.getRefactoringType().equals(RefactoringType.INLINE_OPERATION))
            {
                if (((InlineOperationRefactoring)r).getInlinedOperation().equals(operationBefore))
                    counter2++;
            }
        }
        return (counter1 > 1 || counter2 > 1);
    }

    private static void processModifiedAnnotation(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, UMLAnnotation annotationBefore, UMLAnnotation annotationAfter) {
        Constants LANG1 = new Constants(annotationBefore.getLocationInfo().getFilePath());
        Constants LANG2 = new Constants(annotationAfter.getLocationInfo().getFilePath());
        Tree srcAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree, annotationBefore.getLocationInfo(),LANG1);
        Tree dstAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, annotationAfter.getLocationInfo(),LANG2);
        if (srcAnnotationTree != null & dstAnnotationTree != null)
            new BasicTreeMatcher(LANG1, LANG2).match(srcAnnotationTree,dstAnnotationTree, mappingStore);
        mappingStore.addMapping(srcAnnotationTree,dstAnnotationTree);
    }

    private void findVariablesAndMatch(Tree srcStatement, Tree dstStatement, String originalVariableName, String renamedVariableName, Constants LANG1, Constants LANG2) {
        List<Tree> srcRefs = TreeUtilFunctions.findVariable(srcStatement,originalVariableName,LANG1);
        List<Tree> dstRefs = TreeUtilFunctions.findVariable(dstStatement,renamedVariableName,LANG2);
        if (srcRefs == null || dstRefs == null) return;
        if (srcRefs.size() == 1 && dstRefs.size() == 1)
            optimizationData.getSubtreeMappings().addMapping(srcRefs.get(0),dstRefs.get(0));
        else{
            if (srcRefs.size() == dstRefs.size())
            {
                for (int i = 0; i < srcRefs.size(); i++) {
                    optimizationData.getSubtreeMappings().addMapping(srcRefs.get(i),dstRefs.get(i));
                }
            }
            else {
                //TODO: Begin with the one with fewer occurrences, find the match with the exact same parent type
                for (Tree srcRef : srcRefs) {
                    for (Tree dstRef : dstRefs) {
                        if (srcRef.getParent() != null && dstRef.getParent() != null &&
                                srcRef.getParent().getType().name.equals(dstRef.getParent().getType().name)) {
                            optimizationData.getSubtreeMappings().addMapping(srcRef, dstRef);
                            break;
                        }
                    }
                }

            }
        }

    }
    private List<Refactoring> getClassDiffRefactorings(UMLClassBaseDiff classDiff, List<Refactoring> modelDiffRefactorings) {
        List<Refactoring> classDiffRefactorings = new ArrayList<>();
        for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
            Set<ImmutablePair<String, String>> involvedClassesBeforeRefactoring = modelDiffRefactoring.getInvolvedClassesBeforeRefactoring();
            Set<ImmutablePair<String, String>> involvedClassesAfterRefactoring = modelDiffRefactoring.getInvolvedClassesAfterRefactoring();
            if (involvedClassesBeforeRefactoring.size() > 1 || involvedClassesAfterRefactoring.size() > 1) continue; //Must extend the logic to work for cases with more than one involving classes such as ExtractAndMoveMethodRefactoring
            UMLClass umlClassBefore = classDiff.getOriginalClass();
            UMLClass umlClassAfter = classDiff.getNextClass();
            ImmutablePair<String, String> refactoringClassBefore = involvedClassesBeforeRefactoring.iterator().next();
            ImmutablePair<String, String> refactoringClassAfter = involvedClassesAfterRefactoring.iterator().next();
            if (umlClassBefore.getLocationInfo().getFilePath().equals(refactoringClassBefore.getLeft())
                    || umlClassAfter.getLocationInfo().getFilePath().equals(refactoringClassAfter.getLeft())) {
                String refactoringClassNameBefore = refactoringClassBefore.getRight();
                String refactoringClassNameAfter = refactoringClassAfter.getRight();
                // Relied on || in order to ascertain at least one class involves (handling move to anonymous and vice versa)
                boolean isNameMatching = refactoringClassNameBefore.equals(umlClassBefore.getName()) || refactoringClassNameAfter.equals(umlClassAfter.getName());
                boolean bothInAnonymous = refactoringClassNameBefore.contains(umlClassBefore.getName()) && refactoringClassNameAfter.contains(umlClassAfter.getName());
                if (isNameMatching || bothInAnonymous)
                    classDiffRefactorings.add(modelDiffRefactoring);
            }

        }
        return classDiffRefactorings;
    }
}
