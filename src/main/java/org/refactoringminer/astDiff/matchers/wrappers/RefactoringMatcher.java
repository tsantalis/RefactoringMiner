package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
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

    private final UMLClassBaseDiff baseClassDiff;
    private final List<Refactoring> modelDiffRefactorings;

    public RefactoringMatcher(UMLClassBaseDiff baseClassDiff, List<Refactoring> modelDiffRefactorings) {
        this.baseClassDiff = baseClassDiff;
        this.modelDiffRefactorings = modelDiffRefactorings;
    }

    public RefactoringMatcher(OptimizationData optimizationData, List<Refactoring> modelDiffRefactorings, UMLClassBaseDiff baseClassDiff) {
        super(optimizationData);
        this.modelDiffRefactorings = modelDiffRefactorings;
        this.baseClassDiff = baseClassDiff;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processRefactorings(srcTree,dstTree,getClassDiffRefactorings(baseClassDiff),mappingStore);
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
                for (LambdaExpressionObject lambda : lambdas) {
                    for (VariableDeclaration parameter : lambda.getParameters()) {
                        String variableName = parameter.getVariableName();
                        VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
                        if (variableDeclaration != null) {
                            Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
                            Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,parameter.getLocationInfo());
                            new LeafMatcher().match(srcNode,dstNode,mappingStore);
                        }
                    }
                }
                Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,enhancedFor.getLocationInfo());
                Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,next.getLocationInfo());
                if (srcSt != null && dstSt != null)
                    if (srcSt.getType().name.equals(dstSt.getType().name))
                        mappingStore.addMapping(srcSt,dstSt);
            } else if (refactoring instanceof ReplacePipelineWithLoopRefactoring) {
                ReplacePipelineWithLoopRefactoring replaceLoopWithPipelineRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
                if (replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().size() != 1) continue;
                AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().iterator().next();
                List<LambdaExpressionObject> lambdas = next.getLambdas();
                AbstractCodeFragment enhancedFor = null;
                for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter()) {
                    if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT)) {
                        enhancedFor = abstractCodeFragment;
                        break;
                    }
                }
                if( enhancedFor == null) continue;
                for (LambdaExpressionObject lambda : lambdas) {
                    for (VariableDeclaration parameter : lambda.getParameters()) {
                        String variableName = parameter.getVariableName();
                        VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
                        if (variableDeclaration == null)
                            continue;
                        Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,parameter.getLocationInfo());
                        Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,variableDeclaration.getLocationInfo());
                        new LeafMatcher().match(srcNode,dstNode,mappingStore);
                    }
                }
                Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,next.getLocationInfo());
                Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,enhancedFor.getLocationInfo());
                if (dstSt != null && srcSt != null && srcSt.getType().name.equals(dstSt.getType().name))
                    mappingStore.addMapping(srcSt, dstSt);
            } else if (refactoring instanceof MergeOperationRefactoring) {
                MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                for(UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                    new MethodMatcher(optimizationData, bodyMapper).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof SplitOperationRefactoring) {
                SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                for(UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
                    new MethodMatcher(optimizationData, bodyMapper).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof ExtractOperationRefactoring) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                new BodyMapperMatcher(optimizationData, bodyMapper, true).match(srcTree,dstTree,mappingStore);
                processArgumentMappings(srcTree, dstTree, refactoringList, refactoring, extractOperationRefactoring.getArgumentMappings());
            } else if (refactoring instanceof InlineOperationRefactoring) {
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                new BodyMapperMatcher(optimizationData, bodyMapper, false).match(srcTree,dstTree,mappingStore);
                processArgumentMappings(srcTree, dstTree, refactoringList, refactoring, inlineOperationRefactoring.getArgumentMappings());
            } else if (refactoring instanceof MoveCodeRefactoring) {
                MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
                new BodyMapperMatcher(optimizationData, bodyMapper, false).match(srcTree,dstTree,mappingStore);
            } else if (refactoring instanceof ParameterizeTestRefactoring) {
                ParameterizeTestRefactoring parameterizeTestRefactoring = (ParameterizeTestRefactoring) refactoring;
                UMLOperationBodyMapper bodyMapper = parameterizeTestRefactoring.getBodyMapper();
                new BodyMapperMatcher(optimizationData, bodyMapper, false).match(srcTree,dstTree,mappingStore);
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
                    new AnonymousClassDiffMatcher(optimizationData, umlAnonymousClassDiff).match(srcTree,dstTree,mappingStore);
                }
            } else if (refactoring instanceof MergeVariableRefactoring) {
                MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
                VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
                Tree dstVariableDeclaration =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getLocationInfo());

                if (dstVariableDeclaration != null) {
                    List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
                    Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
                    for (VariableDeclaration variableDeclaration : mergedVariables) {
                        Tree srcVariableDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, variableDeclaration.getLocationInfo());
                        if (srcVariableDeclaration != null) {
                            List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
                            Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
                            mappingStore.addMapping(srcVarName, dstVarName);
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
                Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,mergeCatchRefactoring.getNewCatchBlock().getLocationInfo());
                for (AbstractCodeFragment eachMerged : mergeCatchRefactoring.getMergedCatchBlocks()) {
                    Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,eachMerged.getLocationInfo());
                    new GeneralMatcher(eachMerged, mergeCatchRefactoring.getNewCatchBlock())
                            .match(srcSubTree,dstSubTree,mappingStore);
                }
            } else if (refactoring instanceof RenameVariableRefactoring) {
                RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                VariableDeclaration originalVariable = renameVariableRefactoring.getOriginalVariable();
                VariableDeclaration renamedVariable = renameVariableRefactoring.getRenamedVariable();
                Tree srcInput = TreeUtilFunctions.findByLocationInfo(srcTree,originalVariable.getLocationInfo());
                Tree dstInput = TreeUtilFunctions.findByLocationInfo(dstTree, renamedVariable.getLocationInfo());
                if (srcInput == null || dstInput == null) continue;
                boolean eligible = true;
                switch (renameVariableRefactoring.getRefactoringType()) {
                    case REPLACE_VARIABLE_WITH_ATTRIBUTE:
                        srcInput = TreeUtilFunctions.getParentUntilType(srcInput, Constants.VARIABLE_DECLARATION_STATEMENT);
                        dstInput = TreeUtilFunctions.getParentUntilType(dstInput,Constants.FIELD_DECLARATION);
                        if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
                            continue;
                        break;
                    case REPLACE_ATTRIBUTE_WITH_VARIABLE:
                        srcInput = TreeUtilFunctions.getParentUntilType(srcInput,Constants.FIELD_DECLARATION);
                        dstInput = TreeUtilFunctions.getParentUntilType(dstInput,Constants.VARIABLE_DECLARATION_STATEMENT);
                        if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
                            continue;
                        break;
                    case RENAME_PARAMETER:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
                        break;
                    case RENAME_VARIABLE:
                        Set<AbstractCodeMapping> references = renameVariableRefactoring.getReferences();
                        for (AbstractCodeMapping abstractCodeMapping : references) {
                            if (((RenameVariableRefactoring) refactoring).isInsideExtractedOrInlinedMethod() &&
                                    multipleInstancesWithSameDescription(refactoringList,((RenameVariableRefactoring) refactoring).getOperationBefore(),((RenameVariableRefactoring) refactoring).getOperationAfter())) {
                                eligible = false;
                            }
                            if (eligible) {
                                if (abstractCodeMapping instanceof LeafMapping) {
                                    findVariablesAndMatch(srcTree, dstTree, abstractCodeMapping, renameVariableRefactoring.getOriginalVariable().getVariableName(), renameVariableRefactoring.getRenamedVariable().getVariableName());
                                }
                            }
                        }
                        eligible = false;
                        break;
                    case PARAMETERIZE_VARIABLE:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
                        if (!eligible)
                            break;
                        while (!TreeUtilFunctions.isStatement(srcInput.getType().name)) {
                            if (srcInput.getType() == null) break;
                            srcInput = srcInput.getParent();
                        }
                        if (TreeUtilFunctions.isStatement(srcInput.getType().name)){
                            new LeafMatcher().match(srcInput,dstInput,mappingStore);
                        }
                        eligible = false;
                        break;
                    case LOCALIZE_PARAMETER:
                        eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
                        if (!eligible)
                            break;
                        while (!TreeUtilFunctions.isStatement(dstInput.getType().name)) {
                            if (dstInput.getType() == null) break;
                            dstInput = dstInput.getParent();
                        }
                        if (TreeUtilFunctions.isStatement(dstInput.getType().name)){
                            new LeafMatcher().match(srcInput,dstInput,mappingStore);
                        }
                        eligible = false;
                        break;
                    default:
                        eligible = false;
                }
                if (eligible) new LeafMatcher().match(
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
                        Tree srcSimpleName = TreeUtilFunctions.findByLocationInfo(srcTree, subExpressionMapping.getFragment1().getLocationInfo(), Constants.SIMPLE_NAME);
                        Tree dstSimpleName = TreeUtilFunctions.findByLocationInfo(dstTree, subExpressionMapping.getFragment2().getLocationInfo(), Constants.SIMPLE_NAME);
                        if (srcSimpleName != null && dstSimpleName != null)
                            optimizationData.getSubtreeMappings().addMapping(srcSimpleName,dstSimpleName);
                    }
                }
            }
            else if (refactoring instanceof InvertConditionRefactoring) {
                InvertConditionRefactoring invertConditionRefactoring = (InvertConditionRefactoring) refactoring;
                Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,invertConditionRefactoring.getOriginalConditional().getLocationInfo());
                Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,invertConditionRefactoring.getInvertedConditional().getLocationInfo());
//				new CompositeMatcher(invertConditionRefactoring.getOriginalConditional(), )
                new GeneralMatcher(
                        invertConditionRefactoring.getOriginalConditional(),
                        invertConditionRefactoring.getInvertedConditional())
                        .match(srcSubTree,dstSubTree,mappingStore);
            }
        }

    }

    private static boolean multipleInstancesWithSameDescription(List<Refactoring> refactoringList, Refactoring refactoring) {
        int count = 0;
        for (Refactoring r : refactoringList) {
            if(r.toString().equals(refactoring.toString()))
                count++;
        }
        return count > 1;
    }
    private void processArgumentMappings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, Refactoring refactoring, List<AbstractCodeMapping> argumentMappings) {
        //skip argument mappings, if the same method is extracted more than once from the original method.
        if(!multipleInstancesWithSameDescription(refactoringList, refactoring)) {
            for(AbstractCodeMapping expressionMapping : argumentMappings) {
                Tree t1 = TreeUtilFunctions.findByLocationInfo(srcTree,expressionMapping.getFragment1().getLocationInfo());
                Tree t2 = TreeUtilFunctions.findByLocationInfo(dstTree,expressionMapping.getFragment2().getLocationInfo());
                new LeafMatcher().match(t1,t2,optimizationData.getSubtreeMappings());
            }
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
        Tree srcAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree, annotationBefore.getLocationInfo());
        Tree dstAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, annotationAfter.getLocationInfo());
        if (srcAnnotationTree != null & dstAnnotationTree != null)
            new BasicTreeMatcher().match(srcAnnotationTree,dstAnnotationTree, mappingStore);
        mappingStore.addMapping(srcAnnotationTree,dstAnnotationTree);
    }

    private void findVariablesAndMatch(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, String originalVariableName, String renamedVariableName) {
        Tree srcStatement = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo());
        Tree dstStatement = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo());
        List<Tree> srcRefs = TreeUtilFunctions.findVariable(srcStatement ,originalVariableName);
        List<Tree> dstRefs = TreeUtilFunctions.findVariable(dstStatement ,renamedVariableName);
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
        }

    }
    private List<Refactoring> getClassDiffRefactorings(UMLClassBaseDiff classDiff) {
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
                    && umlClassAfter.getLocationInfo().getFilePath().equals(refactoringClassAfter.getLeft())) {
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
