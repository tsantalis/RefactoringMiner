package org.refactoringminer.api;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.AddVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.diff.*;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PurityChecker {

    public static Map<Refactoring, PurityCheckResult> check(List<Refactoring> refactorings, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
        Map<Refactoring, PurityCheckResult> purityCheckResults = new LinkedHashMap<>();
        for (Refactoring refactoring : refactorings) {
            purityCheckResults.put(refactoring, check(refactoring, refactorings, modelDiff));
        }
        return purityCheckResults;
    }

    public static PurityCheckResult check(Refactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        PurityCheckResult result = null;
        switch (refactoring.getRefactoringType()) {
            case EXTRACT_OPERATION:
                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case MOVE_OPERATION:
                result = detectMoveMethodPurity((MoveOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case MOVE_AND_RENAME_OPERATION:
                result = detectMoveMethodPurity((MoveOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case PUSH_DOWN_OPERATION:
                result = detectPushDownMethodPurity((PushDownOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case PULL_UP_OPERATION:
                result = detectPullUpMethodPurity((PullUpOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case INLINE_OPERATION:
                result = detectInlineMethodPurity((InlineOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case EXTRACT_AND_MOVE_OPERATION:
                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case MOVE_AND_INLINE_OPERATION:
                result = detectInlineMethodPurity((InlineOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case SPLIT_OPERATION:
                result = detectSplitMethodPurity((SplitOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            default:
                result = null;
        }
        return result;
    }

    private static PurityCheckResult detectSplitMethodPurity(SplitOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        Set<AbstractCodeMapping> mappings = new LinkedHashSet<>();
        List<AbstractCodeFragment> nonMappedLeavesT2 = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1 = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT2 = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1 = new ArrayList<>();

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT2ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1ToRemove = new ArrayList<>();

        ReplacementJustificationResult replacementJustificationResult = new ReplacementJustificationResult();


        for (UMLOperationBodyMapper mapper : refactoring.getMappers()) {
            mappings.addAll(mapper.getMappings());

            nonMappedLeavesT2.addAll(mapper.getNonMappedLeavesT2());
            nonMappedLeavesT1.addAll(mapper.getNonMappedLeavesT1());
            nonMappedNodesT2.addAll(mapper.getNonMappedInnerNodesT2());
            nonMappedNodesT1.addAll(mapper.getNonMappedInnerNodesT1());
        }

        for (AbstractCodeMapping mapping : mappings) {
            if (!mapping.getReplacements().isEmpty()) {

                UMLOperationBodyMapper bodyMapper = findTheMapper(refactoring, mapping);
                HashSet<Replacement> replacementsToCheck = new HashSet<>(mapping.getReplacements());
                replacementJustificationResult = checkReplacementForSplitMethod(refactoring, replacementsToCheck, bodyMapper);
                if (!replacementJustificationResult.isJustificationState()) {
                    return new PurityCheckResult(false, "Replacements cannot be justified", "Severe Changes", 2);
                }
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (AbstractCodeMapping mapping : mappings) {
                if (abstractCodeFragment.equalFragment(mapping.getFragment2())) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                    break;
                }
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
            for (AbstractCodeMapping mapping : mappings) {
                if (abstractCodeFragment.equalFragment(mapping.getFragment1())) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                    break;
                }
            }
        }

        for (CompositeStatementObject abstractCodeFragment : nonMappedNodesT2) {
            for (AbstractCodeMapping mapping : mappings) {
                if (abstractCodeFragment.equalFragment(mapping.getFragment2())) {
                    nonMappedNodesT2ToRemove.add(abstractCodeFragment);
                    break;
                }
            }
        }

        for (CompositeStatementObject abstractCodeFragment : nonMappedNodesT1) {
            for (AbstractCodeMapping mapping : mappings) {
                if (abstractCodeFragment.equalFragment(mapping.getFragment1())) {
                    nonMappedNodesT1ToRemove.add(abstractCodeFragment);
                    break;
                }
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedNodesT2.removeAll(nonMappedNodesT2ToRemove);
        nonMappedNodesT1.removeAll(nonMappedNodesT1ToRemove);

        if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
            return new PurityCheckResult(true, "All replacements have been justified - all mapped", replacementJustificationResult.getJustificationComment(), 2);
        }

//        Check to see if all the non-mapped leaves are return statements

        return new PurityCheckResult(false, "Replacements cannot be justified", "Severe Changes", 2);
    }

    private static ReplacementJustificationResult checkReplacementForSplitMethod(SplitOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        ReplacementJustificationResult replacementJustificationResult = new ReplacementJustificationResult();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, bodyMapper);
        omitBooleanVariableDeclarationReplacement(replacementsToCheck, bodyMapper); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(replacementsToCheck);

        if (replacementsToCheck.isEmpty()) {
            replacementJustificationResult.setJustificationState(true);
            replacementJustificationResult.appendJustificationComment("Tolerable Changes in the body");  
        }
        return replacementJustificationResult;
    }

    private static UMLOperationBodyMapper findTheMapper(SplitOperationRefactoring refactoring, AbstractCodeMapping mapping) {
        for (UMLOperationBodyMapper mapper : refactoring.getMappers()) {
            for (AbstractCodeMapping mapperMapping : mapper.getMappings()) {
                if (mapping.equals(mapperMapping)) {
                    return mapper;
                }
            }
        }
        return null;
    }

    private static PurityCheckResult detectPullUpMethodPurity(PullUpOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
                refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped", "Identical statements", mappingState);

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            int sizeToCheckBefore = replacementsToCheck.size();

            omitThisPatternReplacements(replacementsToCheck);



            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
//            omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
//            omitReturnRelatedReplacements(refactoring, replacementsToCheck);

//            PurityUtils.makeAll(modelDiff, refactoring.getOriginalOperation().getClassName(), null);


            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);

            omitStringRelatedReplacements(replacementsToCheck);
            checkForThisPatternReplacement(replacementsToCheck);


            int sizeToCheckAfter = replacementsToCheck.size();

            if (sizeToCheckAfter != sizeToCheckBefore) {
                purityComment += "Tolerable changes in the body" + "\n";
            }

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "this pattern-has been either added or deleted", purityComment, mappingState);
            }


            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeToCheckAfter)
                purityComment += "Changes are within the Pull Up Method refactoring mechanics" + "\n";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method specific changes - all mapped", purityComment, mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            int sizeBefore = replacementsToCheck.size();
            checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Replace Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Replace Attribute on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Move Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Inline Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractMethodOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename class on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move (and Rename) class on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Move Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck); // This method also handles the MoveAndRename Method on top
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Pull Up Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the pull up method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the pull up method - all mapped", purityComment, mappingState);
            }

            Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";


            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsualte Opposite-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Getter method got replaced with direct access or vice verca - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

        } else {

            CheckReplacementResult checkReplacementResultVar = checkReplacementsPullUpMethod(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                return new PurityCheckResult(false, "Replacements cannot be justified - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            List<AbstractCodeFragment> nonMappedLeavesT2 = refactoring.getBodyMapper().getNonMappedLeavesT2();
            List<AbstractCodeFragment> nonMappedLeavesT1 = refactoring.getBodyMapper().getNonMappedLeavesT1();
            List<CompositeStatementObject> nonMappedNodesT2 = refactoring.getBodyMapper().getNonMappedInnerNodesT2();
            List<CompositeStatementObject> nonMappedNodesT1 = refactoring.getBodyMapper().getNonMappedInnerNodesT1();


            checkForBlockNonMapped(nonMappedNodesT2, nonMappedNodesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Just added or removed blocks - or non-important missed mapping", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkIfTheNonMappedStatementsAlreadyBeingMapped(refactoring, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "no non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the pull up method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " - Rename Method -", 5);
            }

//            checkForRemoveVariableOnTop_nonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
//            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the pull up method - with non-mapped leaves or nodes", "Severe Changes", 5);
//            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the pull up method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " - Extract Variable- ", 5);
            }

            checkForInlineVariableNonMappedLeaves(refactorings, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the pull up method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " - Inline Variable -", 5);
            }

            checkForExtraBreakStatementsWithinSwitch(nonMappedLeavesT1, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extra break statements within a switch statement - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkForLocalizeParameterOnTop(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Localize Parameter on top of the pull up method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " -Localize Parameter - ", 5);
            }

            int size1 = nonMappedLeavesT1.size();
            int size2 = nonMappedLeavesT2.size();
            int returnStatementCounter1 = 0;
            int returnStatementCounter2 = 0;


            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter2++;
                }
            }

            if (size1 == returnStatementCounter1 && size2 == returnStatementCounter2 && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Return expression has been added within the Pull Up Method mechanics - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }
            return new PurityCheckResult(false, "Contains non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
        }
    }

    private static void checkForBlockNonMapped(List<CompositeStatementObject> nonMappedNodesT2, List<CompositeStatementObject> nonMappedNodesT1) {
        List<CompositeStatementObject> nonMappedNodesT2ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1ToRemove = new ArrayList<>();

        for (CompositeStatementObject compositeStatementObject : nonMappedNodesT2) {
            if (compositeStatementObject.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                nonMappedNodesT2ToRemove.add(compositeStatementObject);
            }
        }

        for (CompositeStatementObject compositeStatementObject : nonMappedNodesT1) {
            if (compositeStatementObject.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                nonMappedNodesT1ToRemove.add(compositeStatementObject);
            }
        }
        nonMappedNodesT2.removeAll(nonMappedNodesT2ToRemove);
        nonMappedNodesT1.removeAll(nonMappedNodesT1ToRemove);
    }

    private static void checkIfTheNonMappedStatementsAlreadyBeingMapped(Refactoring refactoring, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1, List<CompositeStatementObject> nonMappedNodesT2, List<CompositeStatementObject> nonMappedNodesT1) {
        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
            bodyMapper = ((PullUpOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT2ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                if (abstractCodeFragment.equals(mapping.getFragment2())) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                if (abstractCodeFragment.equals(mapping.getFragment1())) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                }
            }
        }

        for (CompositeStatementObject compositeStatementObject : nonMappedNodesT2) {
            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                if (mapping instanceof CompositeStatementObjectMapping) {
                    if (compositeStatementObject.equals(mapping.getFragment2())) {
                        nonMappedNodesT2ToRemove.add(compositeStatementObject);
                    }
                }
            }
        }

        for (CompositeStatementObject compositeStatementObject : nonMappedNodesT1) {
            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                if (mapping instanceof CompositeStatementObjectMapping) {
                    if (compositeStatementObject.equals(mapping.getFragment1())) {
                        nonMappedNodesT1ToRemove.add(compositeStatementObject);
                    }
                }
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedNodesT2.removeAll(nonMappedNodesT2ToRemove);
        nonMappedNodesT1.removeAll(nonMappedNodesT1ToRemove);
    }

    private static void checkForReplaceAttributeOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            for (Refactoring refactoring : refactorings) {
                if (refactoring.getRefactoringType().equals(RefactoringType.REPLACE_ATTRIBUTE)) {
                    ReplaceAttributeRefactoring replaceAttributeRefactoring = (ReplaceAttributeRefactoring) refactoring;
                    if (replacement.getBefore().equals(replaceAttributeRefactoring.getOriginalAttribute().getName()) &&
                            replacement.getAfter().equals(replaceAttributeRefactoring.getMovedAttribute().getName())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static CheckReplacementResult checkReplacementsPullUpMethod(PullUpOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getReplacements().isEmpty()) {
            return new CheckReplacementResult(true, "");
        }
        String purityComment = "";
        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

        int sizeBefore = replacementsToCheck.size();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
        omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(replacementsToCheck);
        omitReturnRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
        checkForThisPatternReplacement(replacementsToCheck);



        omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);

        int sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "Tolerable Changes in the body" + "\n";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

        int sizeAfterMove = replacementsToCheck.size();

        if (sizeAfterMove != sizeAfter)
            purityComment += "Changes are within the Pull Up Method refactoring mechanics" + "\n";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Replace Attribute-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Inline Variable-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractMethodOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Method-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Method-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Method-";

//        checkForRemoveVariableOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Attribute-";


        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Opposite-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }
        return new CheckReplacementResult(false, "Severe Changes");
    }

    private static PurityCheckResult detectPushDownMethodPurity(PushDownOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
                refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped", "Identical statements", mappingState);

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            int sizeToCheckBefore = replacementsToCheck.size();

            omitThisPatternReplacements(replacementsToCheck);

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "this pattern-has been either added or deleted", "Tolerable changes in the body", mappingState);
            }

            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(replacementsToCheck);
//            omitReturnRelatedReplacements(refactoring, replacementsToCheck);

//            PurityUtils.makeAll(modelDiff, refactoring.getOriginalOperation().getClassName(), null);


            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);

            omitStringRelatedReplacements(replacementsToCheck);
            checkForThisPatternReplacement(replacementsToCheck);


            int sizeToCheckAfter = replacementsToCheck.size();

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variables' type! - all mapped", "Tolerable changes in the body", mappingState);
            }

            if (sizeToCheckAfter != sizeToCheckBefore) {
                purityComment += "Tolerable changes in the body" + "\n";
            }

            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeToCheckAfter)
                purityComment += "Changes are within the Push Down Method refactoring mechanics" + "\n";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Push Down Method specific changes - all mapped", purityComment, mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            int sizeBefore = replacementsToCheck.size();
            checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Replace Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Replace Attribute refactoring on top the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Move Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractMethodOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Move Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck); // This method also handles the MoveAndRename Method on top
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Pull Up Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the push down method - all mapped", purityComment, mappingState);
            }

            Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the push down method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Opposite-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Getter method got replaced with direct access or vice verca - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

        } else {

            CheckReplacementResult checkReplacementResultVar = checkReplacementsPushDownMethod(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                return new PurityCheckResult(false, "Replacements cannot be justified - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            List<AbstractCodeFragment> nonMappedLeavesT2 = refactoring.getBodyMapper().getNonMappedLeavesT2();
            List<AbstractCodeFragment> nonMappedLeavesT1 = refactoring.getBodyMapper().getNonMappedLeavesT1();
            List<CompositeStatementObject> nonMappedNodesT2 = refactoring.getBodyMapper().getNonMappedInnerNodesT2();
            List<CompositeStatementObject> nonMappedNodesT1 = refactoring.getBodyMapper().getNonMappedInnerNodesT1();

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the push down method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " -Rename Method - ", 5);
            }

//            checkForRemoveVariableOnTop_nonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
//            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the push down method - with non-mapped leaves or nodes", "Severe Changes", 5);
//            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the push down method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " -Extract Variable - ", 5);
            }

            checkForExtraBreakStatementsWithinSwitch(nonMappedLeavesT1, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extra break statements within a switch statement - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkForLocalizeParameterOnTop(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Localize Parameter on top of the push down method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + " -Localize Parameter -", 5);
            }

            int size1 = nonMappedLeavesT1.size();
            int size2 = nonMappedLeavesT2.size();
            int returnStatementCounter1 = 0;
            int returnStatementCounter2 = 0;


            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter2++;
                }
            }

            if (size1 == returnStatementCounter1 && size2 == returnStatementCounter2 && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Return expression has been added within the Push Down Method mechanics - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }
            return new PurityCheckResult(false, "Contains non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
        }
    }

    private static CheckReplacementResult checkReplacementsPushDownMethod(PushDownOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getReplacements().isEmpty()) {
            return new CheckReplacementResult(true, "");
        }
        String purityComment = "";
        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

        int sizeBefore = replacementsToCheck.size();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
        omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(replacementsToCheck);
        omitReturnRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());


        omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);
        omitStringRelatedReplacements(replacementsToCheck);
        checkForThisPatternReplacement(replacementsToCheck);

        int sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "Tolerable Changes in the body" + "\n";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

        int sizeAfterMove = replacementsToCheck.size();

        if (sizeAfterMove != sizeAfter)
            purityComment += "Changes are within the Push Down Method refactoring mechanics" + "\n";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForExtractMethodOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Method-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);sizeAfter = replacementsToCheck.size();
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Method-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Method-";

//        checkForRemoveVariableOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Attribute-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Opposite-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }
        return new CheckReplacementResult(false, "Severe Changes");
    }

    private static PurityCheckResult detectInlineMethodPurity(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "There is no replacement! - all mapped", purityComment, mappingState);
            }

            HashSet<Replacement> replacementsToCheck;
            replacementsToCheck = new HashSet<>(refactoring.getReplacements());
            int sizeBefore = replacementsToCheck.size();

            if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Inline Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Inline Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "Changes are within the Inline Method refactoring mechanics" + "\n";

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper());
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(replacementsToCheck);

            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

            checkForTernaryThenReplacement(refactoring, replacementsToCheck);

            checkForVariableReplacedWithMethodInvocationSpecialCases(replacementsToCheck); // For this special case: https://github.com/netty/netty/commit/d31fa31cdcc5ea2fa96116e3b1265baa180df58a#diff-8976fed22cf939e3b9a8a4eba74620d04992dbce5ffb16769df9fcb1019bec7a
            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);

            int sizeAfterTolerable = replacementsToCheck.size();
            if (sizeAfterTolerable != sizeAfter)
                purityComment += "Tolerable changes in the body" + "\n";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }


            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeAfterTolerable)
                purityComment += "Changes are within the Move Method refactoring mechanics" + "\n";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Method specific changes - all mapped", purityComment, mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            sizeBefore = replacementsToCheck.size();
            checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Inline Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Pull Up Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method Refactoring on the top of the Inline Method - all mapped", purityComment, mappingState);
            }

//            checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
//            if (replacementsToCheck.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - all mapped", purityComment, mappingState);
//            }
            sizeBefore = replacementsToCheck.size();
            checkForRemoveParameterOnTopInline(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the inlined method - all mapped", purityComment, mappingState);
            }


            Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForInlineMethodOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Inline Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Method on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Inline Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move and rename class on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Move Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename class on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMergeConditionalOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Merge Conditional-";
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Merge Conditional on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "One of the overlapping cases - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
            sizeAfter = replacementsToCheck.size();
            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Opposite-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Getter method got replaced with direct access or vice verca - all mapped", purityComment, mappingState);
            }

            int size1 = replacementsToCheck.size();
            int numberOfArgumentReplacedWithReturnReplacements = 0;

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                    numberOfArgumentReplacedWithReturnReplacements++;
                }
            }

            if (numberOfArgumentReplacedWithReturnReplacements == size1) {
                return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {
            int mappingState = 2;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());

            int size = nonMappedLeavesT1.size();
            checkForInevitableVariableDeclarationInline(refactoring, nonMappedLeavesT1, refactorings); // This method can also change the state of the refactoring's replacements
            int size2 = nonMappedLeavesT1.size();

            if (size != size2) {
                purityComment += "Severe changes + \n";
            }

            CheckReplacementResult checkReplacementResultVar = checkReplacementsInlineMethod(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement() + " Severe Changes", mappingState);
            }

            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Variable declaration hasn't been inlined, but it doesn't change the logic - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }

            checkForStatementsBeingMappedInTargetOperation(refactoring, refactorings, nonMappedLeavesT1, modelDiff);
            checkForNestedInlineMethod(refactoring, refactorings, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Nested Inline Method or statements being mapped in other refactorings - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement() + " - Inline Method - ", mappingState);
            }

//            checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Remove Attribute change on top of the Inline Method - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }

            checkForInlineVariableNonMappedLeaves(refactorings, nonMappedLeavesT1);


            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the Inline Method - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement() + " - Inline Variable - ", mappingState);
            }

            checkForRenameRefactoringOnTopOfInline_NonMapped(refactoring, refactorings, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the Inline Method - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement() + " - Rename Method - ", mappingState);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT1);
            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on the top of the Inline Method - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement() + " - Extract Variable - ", mappingState);
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (!abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    return new PurityCheckResult(false, "Violating the mechanics of Inline Method refactoring", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
                }
            }

            return new PurityCheckResult(true, "Return statements added", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);


        } else {
            int mappingState = 3;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedInnerNodesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT1());

            CheckReplacementResult checkReplacementResultVar = checkReplacementsInlineMethod(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }

            List<AbstractCodeFragment> nonMappedLeavesT1List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());

            checkForIfCondition(refactoring, nonMappedInnerNodesT1, nonMappedLeavesT1List);

            if (!checkForNonMappedLeavesT1(refactoring, refactorings, nonMappedLeavesT1List, modelDiff)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "non-mapped are not justified - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }

//            checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedInnerNodesT1);

            if (nonMappedInnerNodesT1.isEmpty()) {
                purityComment = "Severe changes";
                return new PurityCheckResult(true, "Remove Attribute change on top of the Inline Method - non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }

            int numberOfWrongNonMappedBlocks = 0;
            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    numberOfWrongNonMappedBlocks++;
                } else {
                    break;
                }
            }

            if (numberOfWrongNonMappedBlocks == nonMappedInnerNodesT1.size()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }
            return new PurityCheckResult(false, "Violating the mechanics of Inline Method refactoring", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
        }
    }

    private static void checkForRemoveVariableOnTop(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        if (bodyMapper.getRemovedVariables() != null) {
            if (bodyMapper.getRemovedVariables().isEmpty()) {
                return;
            }
        } else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (VariableDeclaration removedVariable : bodyMapper.getRemovedVariables()) {
            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getBefore().contains(removedVariable.getVariableName())) {
                    replacementsToRemove.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkTheReplacementsAlreadyHandled(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacements = null;
        if (refactoring instanceof ExtractOperationRefactoring) {
            replacements = ((ExtractOperationRefactoring) (refactoring)).getReplacements();
        } else if (refactoring instanceof InlineOperationRefactoring) {
            replacements = ((InlineOperationRefactoring) (refactoring)).getReplacements();
        } else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        Set<Replacement> handledReplacements = new HashSet<>();

        handledReplacements.addAll(replacements);
        handledReplacements.removeAll(replacementsToCheck);


        for (Replacement replacement : handledReplacements) {
            for (Replacement refactoringReplacement : replacementsToCheck) {
                if (replacement.getBefore().equals(refactoringReplacement.getBefore()) ||
                        replacement.getAfter().equals(refactoringReplacement.getAfter())) {
                    replacementsToRemove.add(refactoringReplacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForVariableReplacedWithMethodInvocationSpecialCases(HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {
                if (((VariableReplacementWithMethodInvocation) replacement).getDirection().name().equals("VARIABLE_TO_INVOCATION")) {
                    if (replacement.getAfter().contains(replacement.getBefore())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForRemoveAttributeOnTop(InlineOperationRefactoring refactoring, UMLModelDiff modelDiff, List<AbstractCodeFragment> nonMappedLeavesT1) {
        String className = refactoring.getInlinedOperation().getClassName();
        if (!modelDiff.getUMLClassDiff(className).getRemovedAttributes().isEmpty()) {

            List<UMLAttribute> removedAttributes = modelDiff.getUMLClassDiff(className).getRemovedAttributes();
            List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                for (UMLAttribute removedAttribute : removedAttributes) {
                    if (abstractCodeFragment.getArgumentizedString().contains(removedAttribute.getName())) {
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                    }
                }
            }
            nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        }
    }


    private static void checkForTernaryThenReplacement(InlineOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN) ||
                    replacement.getType().equals(Replacement.ReplacementType.CONDITIONAL)) {
                AbstractCodeMapping mapping = findTheMapping(replacement, refactoring.getBodyMapper());
                if (!mapping.getFragment1().getTernaryOperatorExpressions().isEmpty()) {
                    for (TernaryOperatorExpression ternaryOperatorExpression : mapping.getFragment1().getTernaryOperatorExpressions()) {
                        AbstractExpression condition = ternaryOperatorExpression.getCondition();
                        if (mapping.getFragment2().getParent().getParent() != null) {
                            if (mapping.getFragment2().getParent().getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                                if (mapping.getFragment2().getParent().getParent().getExpressions().size() == 1) {
                                    if (mapping.getFragment2().getParent().getParent().getExpressions().get(0).getExpression().equals(condition.getExpression()))
                                        replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForIfCondition(InlineOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT1, List<AbstractCodeFragment> nonMappedLeavesT1List) {
        List<AbstractCodeFragment> nonMappedInnerNodesT1ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT1) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                abstractCodeFragment.replaceParametersWithArguments(refactoring.getParameterToArgumentMap());
                abstractCodeFragment.replaceParametersWithArguments(refactoring.getParameterToArgumentMap());
                if (abstractCodeFragment.getArgumentizedString().equals("if(false)")) {
                    nonMappedInnerNodesT1ToRemove.add(abstractCodeFragment);
                    List<AbstractStatement> statements = ((CompositeStatementObject) (abstractCodeFragment)).getStatements();
                    if (statements.size() == 2 || statements.size() == 1) { //it's an if-else condition
                        nonMappedLeavesT1ToRemove.addAll(((CompositeStatementObject) (statements.get(0))).getStatements());
                    }
                } else if (abstractCodeFragment.getArgumentizedString().equals("if(true)")) {
                    nonMappedInnerNodesT1ToRemove.add(abstractCodeFragment);
                    List<AbstractStatement> statements = ((CompositeStatementObject) (abstractCodeFragment)).getStatements();
                    if (statements.size() == 2) {
                        nonMappedLeavesT1ToRemove.addAll(((CompositeStatementObject) (statements.get(1))).getStatements());
                    }
                }
            }
        }
        nonMappedInnerNodesT1.removeAll(nonMappedInnerNodesT1ToRemove);
        nonMappedLeavesT1List.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static boolean checkForNonMappedLeavesT1(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1List, UMLModelDiff modelDiff) {
        if (nonMappedLeavesT1List.isEmpty()) {
            return true;
        }

        checkForInevitableVariableDeclarationInline(refactoring, nonMappedLeavesT1List, refactorings); // This method can also change the state of the refactoring's replacements
        checkForStatementsBeingMappedInTargetOperation(refactoring, refactorings, nonMappedLeavesT1List, modelDiff);
        checkForNestedInlineMethod(refactoring, refactorings, nonMappedLeavesT1List);
        checkForInlineVariableNonMappedLeaves(refactorings, nonMappedLeavesT1List);
        checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT1List);
        checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedLeavesT1List);


        if (nonMappedLeavesT1List.isEmpty()) {
            return true;
        }

        int size = nonMappedLeavesT1List.size();
        int returnStatementCounter = 0;

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1List) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                returnStatementCounter++;
            }
        }

        if (size == returnStatementCounter) {
            return true;
        }

        return false;
    }

    private static void checkForInlineVariableNonMappedLeaves(List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_VARIABLE)) {
                VariableDeclaration variableDeclaration = ((InlineVariableRefactoring) (refactoring1)).getVariableDeclaration();
                for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                    if (abstractCodeFragment.getVariableDeclarations().contains(variableDeclaration)) {
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                    }
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForInlineVariableOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            AbstractCodeMapping mapping = findTheMapping(replacement, bodyMapper);

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_VARIABLE)) {
                    if (((InlineVariableRefactoring) refactoring1).getReferences().contains(mapping)) {
                        replacementsToRemove.add(replacement);
                        break;
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForNestedInlineMethod(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {

            for (AbstractCall methodInvocation : abstractCodeFragment.getMethodInvocations()) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION) && !refactoring1.equals(refactoring)) {
                        if (methodInvocation.getName().equals(((InlineOperationRefactoring) (refactoring1)).getInlinedOperation().getName())) {
                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
            //TODO: Double check code above
//            for (Map.Entry<String, List<AbstractCall>> stringListEntry : abstractCodeFragment.getMethodInvocationMap().entrySet()) {
//                for (Refactoring refactoring1 : refactorings) {
//                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION) && !refactoring1.equals(refactoring)) {
//                        if (stringListEntry.getValue().get(0).getName().equals(((InlineOperationRefactoring) (refactoring1)).getInlinedOperation().getName())) {
//                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
//                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
//                            }
//                        }
//                    }
//                }
//            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForStatementsBeingMappedInTargetOperation(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1, UMLModelDiff modelDiff) {
        UMLClassBaseDiff umlClassDiff = modelDiff.getUMLClassDiff(refactoring.getTargetOperationAfterInline().getClassName());
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        for (UMLOperationBodyMapper umlOperationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            if (umlOperationBodyMapper != null && umlOperationBodyMapper.getOperation2() != null) {
                if (umlOperationBodyMapper.getOperation2().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
                    for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                        for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                            if (mapping.getFragment2().getString().equals(abstractCodeFragment.getString())) {
                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForInlineMethodOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) ||
                    ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getName())))) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
                        if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(invokedOperationAfterName)) {
                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getParameterNameList().size() == ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().size()) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static CheckReplacementResult checkReplacementsInlineMethod(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());
        String purityComment = "";
        int sizeBefore = replacementsToCheck.size();

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
            purityComment = "Changes are within the Inline Method refactoring mechanics";
            return new CheckReplacementResult(true,  purityComment);
        }

        replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
        replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

        if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
            purityComment = "Changes are within the Inline Method refactoring mechanics";
            return new CheckReplacementResult(true,  purityComment);
        }

        int sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "Changes are within the Inline Method refactoring mechanics" + "\n";

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
        omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper());
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(replacementsToCheck);
        omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());
        checkForTernaryThenReplacement(refactoring, replacementsToCheck);
        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);

        int sizeAfterTolerable = replacementsToCheck.size();

        if (sizeAfterTolerable != sizeAfter)
            purityComment += "Tolerable Changes in the body" + "\n";


        omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeAfterTolerable)
            purityComment += "Changes are within the Move Method refactoring mechanics" + "\n";


        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

        sizeBefore = replacementsToCheck.size();
        checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        if (replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForInlineMethodOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Inline Method-";

        sizeBefore = replacementsToCheck.size();
        checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Inline Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Variable-";

//        checkForRemoveVariableOnTop(replacementsToCheck, refactoring.getBodyMapper());
        checkForVariableReplacedWithMethodInvocationSpecialCases(replacementsToCheck);// For this special case: https://github.com/netty/netty/commit/d31fa31cdcc5ea2fa96116e3b1265baa180df58a#diff-8976fed22cf939e3b9a8a4eba74620d04992dbce5ffb16769df9fcb1019bec7a
        if (replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);

        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Method-";

        sizeBefore = replacementsToCheck.size();
        checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Pull Up Method-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForMergeConditionalOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Merge Conditional-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Opposite-";


        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        int size1 = replacementsToCheck.size();
        int numberOfArgumentReplacedWithReturnReplacements = 0;

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                numberOfArgumentReplacedWithReturnReplacements++;
            }
        }

        if (numberOfArgumentReplacedWithReturnReplacements == size1) {
            return new CheckReplacementResult(true, purityComment);
        }

        return new CheckReplacementResult(false, "Severe Changes");
    }

    private static PurityCheckResult detectMoveMethodPurity(MoveOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {

/*
Mapping state for Move Method refactoring purity:

1: When there is no nan-mapped leaves or nodes
2: When the non-mapped leaves in T2
3:
4:
5: When the number of parameters is different between the two targeted methods.

 */

//        if (refactoring.getOriginalOperation().getParameterNameList().size() != refactoring.getMovedOperation().getParameterNameList().size()) {
//
//            return new PurityCheckResult(false, "Number of parameters is different. Strong clue of impurity in case of Move Method refactorings!", "Severe changes", 5);
//        }

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
                refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped", "Identical statements", mappingState);

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            int sizeToCheckBefore = replacementsToCheck.size();

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(replacementsToCheck);
            checkForThisPatternReplacement(replacementsToCheck);

//            omitReturnRelatedReplacements(refactoring, replacementsToCheck);

            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);

            omitStringRelatedReplacements(replacementsToCheck);

            int sizeToCheckAfter = replacementsToCheck.size();

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variables' type! - all mapped", "Tolerable changes in the body", mappingState);
            }

            if (sizeToCheckAfter != sizeToCheckBefore) {
                purityComment += "Tolerable changes in the body" + "\n";
            }


            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

            int sizeToCheckAfterMechanic = replacementsToCheck.size();

            if (sizeToCheckAfter != sizeToCheckAfterMechanic) {
                purityComment += "Changes are within the Move Method refactoring mechanics" + "\n";
            }

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Method specific changes - all mapped", purityComment, mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            int sizeBefore = replacementsToCheck.size();
            checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Replace Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Replace Attribute refactoring on top the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Move Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the moved method - all mapped", purityComment, mappingState);
            }

//            checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
//            if (replacementsToCheck.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - all mapped", purityComment, mappingState);
//            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractMethodOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Move Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck); // This method also handles the MoveAndRename Method on top
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Pull Up Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate Attribute refactoring on top of the moved method - all mapped", purityComment, mappingState);
            }

            Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the moved method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Opposite-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Getter method got replaced with direct access or vice verca - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

        } else {

            List<AbstractCodeFragment> nonMappedLeavesT2 = refactoring.getBodyMapper().getNonMappedLeavesT2();
            List<AbstractCodeFragment> nonMappedLeavesT1 = refactoring.getBodyMapper().getNonMappedLeavesT1();
            List<CompositeStatementObject> nonMappedNodesT2 = refactoring.getBodyMapper().getNonMappedInnerNodesT2();
            List<CompositeStatementObject> nonMappedNodesT1 = refactoring.getBodyMapper().getNonMappedInnerNodesT1();


            checkForInevitableVariableDeclaration(refactoring.getBodyMapper(), nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements

            CheckReplacementResult checkReplacementResultVar = checkReplacementsMoveMethod(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                return new PurityCheckResult(false, "Replacements cannot be justified - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);

            }

            checkForPrint_NonMapped(nonMappedLeavesT2, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Extra print lines - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the moved method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + "-Rename Method", 5);
            }

//            checkForRemoveVariableOnTop_nonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
//            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - with non-mapped leaves or nodes", "Severe Changes", 5);
//            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the moved method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + "-Extract Variable - ", 5);
            }

            checkForExtraBreakStatementsWithinSwitch(nonMappedLeavesT1, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extra break statements within a switch statement - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }

            checkForLocalizeParameterOnTop(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Localize Parameter on top of the moved method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + "-Localize Parameter - ", 5);
            }

            checkForParameterizeVariableOnTop(refactoring, refactorings, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Parameterize Variable on top of the moved method - with non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement() + "-Parameterize Variable", 5);
            }

            checkForInevitableVariableDeclaration(refactoring.getBodyMapper(), nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements


//            checkForThisPatternNonMapped(refactoring, nonMappedLeavesT1, nonMappedLeavesT2);
//            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
//                return new PurityCheckResult(true, "Extra this pattern has been added", "Severe Changes", 5);
//            }

            int size1 = nonMappedLeavesT1.size();
            int size2 = nonMappedLeavesT2.size();
            int size3 = nonMappedNodesT1.size();
            int size4 = nonMappedNodesT2.size();
            int returnStatementCounter1 = 0;
            int returnStatementCounter2 = 0;
            int blockStatementCounter1 = 0;
            int blockStatementCounter2 = 0;


            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter2++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedNodesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    blockStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedNodesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    blockStatementCounter2++;
                }
            }



            if (size1 == returnStatementCounter1 && size2 == returnStatementCounter2 && size4 == blockStatementCounter2 && size3 == blockStatementCounter1) {
                return new PurityCheckResult(true, "Return expression has been added within the Move Method mechanics - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), 5);
            }


            return new PurityCheckResult(false, "Contains non-mapped leaves or nodes", checkReplacementResultVar.getPurityCommentReplacement(), 5);
        }

    }

    private static void checkForParameterizeVariableOnTop(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {
        UMLOperation originalOperation = null;
        UMLOperation movedOperation = null;

        if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)  || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
            originalOperation = ((MoveOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((MoveOperationRefactoring) (refactoring)).getMovedOperation();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            originalOperation = ((PushDownOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((PushDownOperationRefactoring) (refactoring)).getMovedOperation();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
            originalOperation = ((PullUpOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((PullUpOperationRefactoring) (refactoring)).getMovedOperation();
        }


        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.LOCALIZE_PARAMETER) || refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                if (originalOperation.equals(((RenameVariableRefactoring) (refactoring1)).getOperationBefore()) &&
                        movedOperation.equals(((RenameVariableRefactoring) (refactoring1)).getOperationAfter())) {
                    for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                        for (VariableDeclaration variableDeclaration : abstractCodeFragment.getVariableDeclarations()) {
                            if (variableDeclaration.equals(((RenameVariableRefactoring) (refactoring1)).getOriginalVariable())) {
                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForLocalizeParameterOnTop(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {
        UMLOperation originalOperation = null;
        UMLOperation movedOperation = null;

        if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)  || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
            originalOperation = ((MoveOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((MoveOperationRefactoring) (refactoring)).getMovedOperation();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            originalOperation = ((PushDownOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((PushDownOperationRefactoring) (refactoring)).getMovedOperation();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
            originalOperation = ((PullUpOperationRefactoring) (refactoring)).getOriginalOperation();
            movedOperation = ((PullUpOperationRefactoring) (refactoring)).getMovedOperation();
        }


        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.LOCALIZE_PARAMETER) || refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                if (originalOperation.equals(((RenameVariableRefactoring) (refactoring1)).getOperationBefore()) &&
                        movedOperation.equals(((RenameVariableRefactoring) (refactoring1)).getOperationAfter())) {
                    for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                        for (VariableDeclaration variableDeclaration : abstractCodeFragment.getVariableDeclarations()) {
                            if (variableDeclaration.equals(((RenameVariableRefactoring) (refactoring1)).getRenamedVariable())) {
                                nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void checkForEncapsulateAttributeOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.ENCAPSULATE_ATTRIBUTE)) {
                        if (replacement.getBefore().equals(((EncapsulateAttributeRefactoring) refactoring1).getAttributeBefore().getName())) {
                            if (((EncapsulateAttributeRefactoring) refactoring1).getAddedGetter() != null) {
                                if (((EncapsulateAttributeRefactoring) refactoring1).getAddedGetter().getName().equals(((VariableReplacementWithMethodInvocation) replacement).getInvokedOperation().getName())) {
                                    replacementsToRemove.add(replacement);
                                }
                            } else if (((EncapsulateAttributeRefactoring) refactoring1).getAddedSetter() != null) {
                                if (((EncapsulateAttributeRefactoring) refactoring1).getAddedSetter().getName().equals(((VariableReplacementWithMethodInvocation) replacement).getInvokedOperation().getName())) {
                                    replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForExtraBreakStatementsWithinSwitch(List<AbstractCodeFragment> nonMappedLeavesT1, List<AbstractCodeFragment> nonMappedLeavesT2) {
        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BREAK_STATEMENT)) {
                if (abstractCodeFragment.getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_STATEMENT)) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                }
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BREAK_STATEMENT)) {
                if (abstractCodeFragment.getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_CASE)) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void omitStringRelatedReplacements(HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getBefore().replaceAll("\"", "").replaceAll("'", "").equals(replacement.getAfter().replaceAll("\"", "").replaceAll("'", ""))) {
                replacementsToRemove.add(replacement);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitMoveMethodRelatedReplacements(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLModelDiff modelDiff) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {

            String before = "";
            String after = "";

            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {

                before = replacement.getBefore();
                after = replacement.getAfter();

            } else if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(
                        ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()
                )) {
                    if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().equals(
                            ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments()
                    )) {
                        before = replacement.getBefore();
                        after = replacement.getAfter();
                    } else if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().size() ==
                            ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments().size()) {
                        before = replacement.getBefore().substring(0, replacement.getBefore().indexOf("("));
                        after = replacement.getAfter().substring(0, replacement.getAfter().indexOf("("));
                    }
                }
            } else {
                return;
            }

            int foundInAfter = after.indexOf(before);
            int foundInBefore = before.indexOf(after);
            List<String> classBefore = new ArrayList<>();
            List<String> classAfter = new ArrayList<>();
            UMLOperation originalOperation = null;
            UMLOperation refactoredOperation = null;

            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION) || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                String classBeforeString = ((MoveOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName();
                String classAfterString = ((MoveOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName();
                classBefore.add(classBeforeString);
                classAfter.add(classAfterString);
                originalOperation = ((MoveOperationRefactoring) (refactoring)).getOriginalOperation();
                refactoredOperation = ((MoveOperationRefactoring) (refactoring)).getMovedOperation();

                if (modelDiff.findClassInChildModel(classAfterString).getSuperclass() != null) {
                    classAfter.add(modelDiff.findClassInChildModel(classAfterString).getSuperclass().toString().replaceAll("<[^>]*>", ""));
                }

                if (modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces() != null) {
                    for (UMLType implementedInterface : modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces()) {
                        classAfter.add(implementedInterface.toString().replaceAll("<[^>]*>", ""));
                    }
                }

            } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
                String classBeforeString = ((UMLOperation) ((InlineOperationRefactoring) (refactoring)).getTargetOperationBeforeInline()).getNonQualifiedClassName();
                String classAfterString = ((InlineOperationRefactoring) (refactoring)).getInlinedOperation().getNonQualifiedClassName();
                classBefore.add(classBeforeString);
                classAfter.add(classAfterString);
                originalOperation = ((UMLOperation) ((InlineOperationRefactoring) (refactoring)).getTargetOperationBeforeInline());
                refactoredOperation = ((InlineOperationRefactoring) (refactoring)).getInlinedOperation();

                if (modelDiff.findClassInChildModel(classAfterString).getSuperclass() != null) {
                    classAfter.add(modelDiff.findClassInChildModel(classAfterString).getSuperclass().toString().replaceAll("<[^>]*>", ""));
                }

                if (modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces() != null) {
                    for (UMLType implementedInterface : modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces()) {
                        classAfter.add(implementedInterface.toString().replaceAll("<[^>]*>", ""));
                    }
                }

            } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
                String classBeforeString = ((PushDownOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName();
                String classAfterString = ((PushDownOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName();
                classBefore.add(classBeforeString);
                classAfter.add(classAfterString);
                originalOperation = ((PushDownOperationRefactoring) (refactoring)).getOriginalOperation();
                refactoredOperation = ((PushDownOperationRefactoring) (refactoring)).getMovedOperation();

            } else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
                String classBeforeString = ((PullUpOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName();
                String classAfterString = ((PullUpOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName();
                classBefore.add(classBeforeString);
                classAfter.add(classAfterString);
                originalOperation = ((PullUpOperationRefactoring) (refactoring)).getOriginalOperation();
                refactoredOperation = ((PullUpOperationRefactoring) (refactoring)).getMovedOperation();

            } else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
                String classBeforeString = ((UMLOperation) ((ExtractOperationRefactoring) (refactoring)).getSourceOperationBeforeExtraction()).getNonQualifiedClassName();
                String classAfterString = ((ExtractOperationRefactoring) (refactoring)).getExtractedOperation().getNonQualifiedClassName();
                classBefore.add(classBeforeString);
                classAfter.add(classAfterString);
                originalOperation = ((UMLOperation) ((ExtractOperationRefactoring) (refactoring)).getSourceOperationBeforeExtraction());
                refactoredOperation = ((ExtractOperationRefactoring) (refactoring)).getExtractedOperation();

                if (modelDiff.findClassInChildModel(classAfterString).getSuperclass() != null) {
                    classAfter.add(modelDiff.findClassInChildModel(classAfterString).getSuperclass().toString().replaceAll("<[^>]*>", ""));
                }

                if (modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces() != null) {
                    for (UMLType implementedInterface : modelDiff.findClassInChildModel(classAfterString).getImplementedInterfaces()) {
                        classAfter.add(implementedInterface.toString().replaceAll("<[^>]*>", ""));
                    }
                }
            } else {
                return;
            }

            if (foundInBefore != -1 && foundInBefore != 0) {
                String instanceOrVariable = before.substring(0, foundInBefore - 1);
                if (classAfter.contains(instanceOrVariable)) {
                    replacementsToRemove.add(replacement);
                    continue;
                }
                if (searchInVariableDeclarations(originalOperation, instanceOrVariable, classAfter, modelDiff)) {
                    replacementsToRemove.add(replacement);
                    continue;
                }
                //Search in different method related refactorings to see if the class has been changed or not, specially, in an anonymous way.
                if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                    if (relaxSearch(originalOperation, refactorings, instanceOrVariable, ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName(), modelDiff)) {
                        replacementsToRemove.add(replacement);
                    }
                }

            } else if (foundInAfter != -1 && foundInAfter != 0) {
                String instanceOrVariable = after.substring(0, foundInAfter - 1);
                if (classBefore.contains(instanceOrVariable)) {
                    replacementsToRemove.add(replacement);
                    continue;
                }
                if (searchInVariableDeclarations(refactoredOperation, instanceOrVariable, classBefore, modelDiff)) {
                    replacementsToRemove.add(replacement);
                    continue;
                }
                //Search in different method related refactorings to see if the class has been changed or not, specially, in an anonymous way.
                if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                    if (relaxSearch(refactoredOperation, refactorings, instanceOrVariable, ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName(), modelDiff)) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean relaxSearch(UMLOperation movedOperation, List<Refactoring> refactorings, String instanceOrVariable, String method, UMLModelDiff modelDiff) {
        for (Refactoring refactoring : refactorings) {
            if (isMethodRelatedRefactoring(refactoring)) {
                if (refactoring instanceof AddMethodAnnotationRefactoring) {
                    if (((AddMethodAnnotationRefactoring) refactoring).getOperationAfter().getName().equals(method)) {
                        String newClassName = ((AddMethodAnnotationRefactoring) refactoring).getOperationAfter().getNonQualifiedClassName();
                        int newClassNameWithoutNewIndex = newClassName.indexOf("new");
                        if (newClassNameWithoutNewIndex != -1) {
                            List<String> newClassNameWithoutNewList = new ArrayList<>();
                            String newClassNameWithoutNew = newClassName.substring(newClassNameWithoutNewIndex + "new".length() + 1);
                            newClassNameWithoutNewList.add(newClassNameWithoutNew);

                            if (searchInVariableDeclarations(movedOperation, instanceOrVariable, newClassNameWithoutNewList, modelDiff)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isMethodRelatedRefactoring(Refactoring refactoring) {
        if (refactoring instanceof AddMethodAnnotationRefactoring ||
            refactoring instanceof AddMethodModifierRefactoring ||
            refactoring instanceof ChangeOperationAccessModifierRefactoring) {
            return true;
        }
        return false;
    }

    private static boolean searchInVariableDeclarations(UMLOperation operation, String instanceOrVariable, List<String> className, UMLModelDiff umlModelDiff) {
        // This iteration also covers the parameters list
        for (VariableDeclaration variableDeclaration : operation.getAllVariableDeclarations()) {
            if (variableDeclaration.getVariableName().equals(instanceOrVariable)) {
                if (className.contains(variableDeclaration.getType().getClassType())) {
                    return true;
                }
            }
        }

        UMLAbstractClass classInChildModel = umlModelDiff.findClassInChildModel(operation.getClassName());
        if(classInChildModel != null) {
			for (UMLAttribute attribute : classInChildModel.getAttributes()) {
	            if (attribute.getName().equals(instanceOrVariable)) {
	                if (className.contains(attribute.getType().getClassType()))
	                    return true;
	            }
	        }
        }
        return false;
    }

    private static void checkForPullUpMethodOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLModelDiff modelDiff) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(
                        ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()
                )) {
                    if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().equals(
                            ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments()
                    )) { //Taking into account that two lists will be equal in Java when the order of insertion was the same along with the values themselves

                        String before = replacement.getBefore();
                        String after = replacement.getAfter();
                        String methodInvocationName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
                                if (((PullUpOperationRefactoring) refactoring1).getOriginalOperation().getName().equals(methodInvocationName)) {
                                    int foundInAfter = after.indexOf(before);
                                    int foundInBefore = before.indexOf(after);

                                    if (foundInBefore != -1) {
                                        String instanceOrVariable = before.substring(0, foundInBefore - 1);
                                        if (specificCheckForPullUpMethodOnTopFoundInBefore(refactoring, ((PullUpOperationRefactoring) refactoring1), instanceOrVariable, (MethodInvocationReplacement) replacement, modelDiff)) {
                                            replacementsToRemove.add(replacement);
                                        }
                                    } else if (foundInAfter != -1) {
                                        String instanceOrVariable = after.substring(0, foundInAfter - 1);
                                        if (specificCheckForPullUpMethodOnTopFoundInAfter(refactoring, ((PullUpOperationRefactoring) refactoring1), instanceOrVariable, (MethodInvocationReplacement) replacement, modelDiff)) {
                                            replacementsToRemove.add(replacement);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean specificCheckForPullUpMethodOnTopFoundInBefore(Refactoring refactoring, PullUpOperationRefactoring pullUpOperationRefactoring, String instanceOrVariable, MethodInvocationReplacement replacement, UMLModelDiff modelDiff) {
        if (pullUpOperationRefactoring.getOriginalOperation().getClassName().equals(instanceOrVariable)) {
            return true;
        }

        UMLOperation originalOperation = null;

        if (refactoring instanceof MoveOperationRefactoring) {
            originalOperation = ((MoveOperationRefactoring) refactoring).getOriginalOperation();
        } else if (refactoring instanceof ExtractOperationRefactoring) {
            originalOperation = (UMLOperation) (((ExtractOperationRefactoring) refactoring).getSourceOperationBeforeExtraction());
        } else if (refactoring instanceof InlineOperationRefactoring) {
            originalOperation = (UMLOperation) (((InlineOperationRefactoring) refactoring).getTargetOperationBeforeInline());
        }



        for (UMLClass umlClass : modelDiff.getParentModel().getClassList()) {
            if (umlClass.getNonQualifiedName().equals(originalOperation.getNonQualifiedClassName())) {
                for (UMLAttribute attribute : umlClass.getAttributes()) {
                    if (attribute.getName().equals(instanceOrVariable)) {
                        if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getOriginalOperation().getNonQualifiedClassName())) {
                            return true;
                        }
                    }
                }
            }
//        Search in the class or method for the attribute or variable
        }
        return false;
    }

    private static boolean specificCheckForPullUpMethodOnTopFoundInAfter(Refactoring refactoring, PullUpOperationRefactoring pullUpOperationRefactoring, String instanceOrVariable, MethodInvocationReplacement replacement, UMLModelDiff modelDiff) {
        if (pullUpOperationRefactoring.getMovedOperation().getClassName().equals(instanceOrVariable)) {
            return true;
        }

        if (refactoring instanceof InlineOperationRefactoring) {
            InlineOperationRefactoring generalInlinedOperation = (InlineOperationRefactoring) refactoring;
            for (UMLClass umlClass : modelDiff.getChildModel().getClassList()) {
                if (umlClass.getNonQualifiedName().equals(generalInlinedOperation.getInlinedOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getMovedOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            }

//        Search in the class or method for the attribute or variable
        }


        if (refactoring instanceof MoveOperationRefactoring) {
            MoveOperationRefactoring generalMoveOperation = (MoveOperationRefactoring) refactoring;
            for (UMLClass umlClass : modelDiff.getChildModel().getClassList()) {
                if (umlClass.getNonQualifiedName().equals(generalMoveOperation.getMovedOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getMovedOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            }

//        Search in the class or method for the attribute or variable
        }
        return false;
    }

    private static void checkForRemoveVariableOnTop_nonMapped(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1, List<CompositeStatementObject> nonMappedNodesT2, List<CompositeStatementObject> nonMappedNodesT1) {
        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)  || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
            bodyMapper = ((PullUpOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        if (bodyMapper.getRemovedVariables().isEmpty()) {
            return;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT2ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1ToRemove = new ArrayList<>();



        for (VariableDeclaration removedVariable : bodyMapper.getRemovedVariables()) {
            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getString().contains(removedVariable.getVariableName())) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getString().contains(removedVariable.getVariableName())) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                }
            }
            for (CompositeStatementObject compositeStatementObject : nonMappedNodesT2) {
                if (compositeStatementObject.getString().contains(removedVariable.getVariableName())) {
                    nonMappedNodesT2ToRemove.add(compositeStatementObject);
                }
            }

            for (CompositeStatementObject compositeStatementObject : nonMappedNodesT1) {
                if (compositeStatementObject.getString().contains(removedVariable.getVariableName())) {
                    nonMappedNodesT1ToRemove.add(compositeStatementObject);
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedNodesT2.removeAll(nonMappedNodesT2ToRemove);
        nonMappedNodesT1.removeAll(nonMappedNodesT1ToRemove);
    }

    private static CheckReplacementResult checkReplacementsMoveMethod(MoveOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        String purityComment = "";
        if (refactoring.getReplacements().isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

        int sizeToCheckBefore = replacementsToCheck.size();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
        omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(replacementsToCheck);
        omitReturnRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());


        omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);
        checkForThisPatternReplacement(replacementsToCheck);

        int sizeToCheckAfter = replacementsToCheck.size();

        if (sizeToCheckAfter != sizeToCheckBefore) {
            purityComment += "Tolerable changes in the body" + "\n";
        }
        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);
        int sizeToCheckAfterMechanic = replacementsToCheck.size();

        if (sizeToCheckAfterMechanic != sizeToCheckAfter) {
            purityComment += "Changes are within the Move Method refactoring mechanics" + "\n";
        }

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }


        int sizeBefore = replacementsToCheck.size();
        checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        int sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Replace Attribute-";


        sizeBefore = replacementsToCheck.size();
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Variable-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Attribute-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Attribute-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForExtractMethodOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Method-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        sizeBefore = replacementsToCheck.size();
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Move Method-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Method-";

//        checkForRemoveVariableOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Attribute-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        sizeBefore = replacementsToCheck.size();
        relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
        sizeAfter = replacementsToCheck.size();
        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Opposite-";


        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                continue;
            }
            return new CheckReplacementResult(false, "Severe Changes");
        }
        return new CheckReplacementResult(true, purityComment);
    }

    private static void checkForThisPatternReplacement(HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                if (findLongestPrefixSuffix(replacement.getBefore(), replacement.getAfter()).equals("this") ||
                        findLongestPrefixSuffix(replacement.getAfter(), replacement.getBefore()).equals("this")) {
                    replacementsToRemove.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static String findLongestPrefixSuffix(String s1, String s2) {
        for( int i = Math.min(s1.length(), s2.length()); ; i--) {
            if(s2.endsWith(s1.substring(0, i))) {
                return s1.substring(0, i);
            }
        }
    }

    private static void checkForMoveMethodRefactoringOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
                String classNameBeforeRefactoring = ((MoveOperationRefactoring) (refactoring1)).getOriginalOperation().getNonQualifiedClassName();
                String classNameAfterRefactoring = ((MoveOperationRefactoring) (refactoring1)).getMovedOperation().getNonQualifiedClassName();
                String methodNameRefactoring = ((MoveOperationRefactoring) (refactoring1)).getMovedOperation().getName();

                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                        if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()) &&
                                ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(methodNameRefactoring)) {
                            if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression() != null && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression() != null) {
                                if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression().equals(classNameBeforeRefactoring) &&
                                        ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression().equals(classNameAfterRefactoring)) {
                                    replacementsToRemove.add(replacement);
                                }
                            } else if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression() != null && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression() == null) {
                                if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression().equals(classNameBeforeRefactoring)) {
                                    replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    } else if (classNameAfterRefactoring.equals(replacement.getAfter()) && classNameAfterRefactoring.equals(replacement.getAfter())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForMoveClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((MoveClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((MoveClassRefactoring)refactoring).getMovedClass().getNonQualifiedName())) {
                        if (!replacement.getBefore().equals(replacement.getAfter())) //For the cases where the method arguments or the class instance creation arguments have been changed, not the name of the method itself. Also, changing the directory can break the previous check.
                            replacementsToRemove.add(replacement);
//                        TODO handle the anonymous class cases.
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForRenameClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.RENAME_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((RenameClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((RenameClassRefactoring)refactoring).getRenamedClass().getNonQualifiedName())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForMoveAndRenameClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_RENAME_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((MoveAndRenameClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((MoveAndRenameClassRefactoring)refactoring).getRenamedClass().getNonQualifiedName())) {
                        replacementsToRemove.add(replacement);
                        //                        TODO handle the anonymous class cases.
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static PurityCheckResult detectExtractOperationPurity(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()) {
            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "There is no replacement! - all mapped", purityComment, mappingState);
            }

            HashSet<Replacement> replacementsToCheck;

//            This method also checks for the exact matches when we have Type Replacement
            if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            replacementsToCheck = new HashSet<>(refactoring.getReplacements());
            int sizeBefore = replacementsToCheck.size();
            replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            if (replacementsToCheck.isEmpty()) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1 instanceof ExtractOperationRefactoring) {
                    if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getSourceOperationAfterExtraction().getName()) &&
                            !((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getExtractedOperation().getName())) {

                        adjustTheParameterArgumentField(refactoring, (ExtractOperationRefactoring) refactoring1);
                        replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
                    }
                }
            }

            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);
//            omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
            checkForParameterArgumentPair(refactoring, replacementsToCheck);

            adjustTheParameterArgumentFieldSourceOperationAfterExtraction(refactoring);

            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);


            if (replacementsToCheck.isEmpty()) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }

            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "Changes are within the Extract Method refactoring mechanics" + "\n";

            if (replacementsToCheck.size() == 1) {
                for (Replacement replacement: replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                        return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
                    }
                }
            }

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(replacementsToCheck);
            // for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
            omitReplacementsAccordingSupplierGetPattern(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
            omitReturnRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());


            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);


            if (replacementsToCheck.isEmpty()) {
                purityComment += "Tolerable changes in the body" + "\n";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }

            int sizeAfterTolerable = replacementsToCheck.size();

            if (sizeAfterTolerable != sizeAfter)
                purityComment += "Tolerable changes in the body" + "\n";


            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);

            if (replacementsToCheck.isEmpty()) {
                purityComment += "Changes are within the Move Method refactoring mechanics" + "\n";
                return new PurityCheckResult(true, "Move Method specific changes - all mapped", purityComment, mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            sizeBefore = replacementsToCheck.size();
            checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Inline Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForSplitConditionOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Split Condition-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Split Condition on top of the extracted method - all mapped", purityComment, mappingState);
            }


            sizeBefore = replacementsToCheck.size();
            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Pull Up Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate Attribute refactoring on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method Refactoring on the top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Replace Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Replace Attribute on top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the extract method - all mapped", purityComment, mappingState);
            }

            Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());

            sizeBefore = replacementsToCheck.size();
            checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Add Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the extract method (relax mode) - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Remove Parameter-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Attribute-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Move Attribute-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMergeVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Merge Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Merge Variable on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractMethodOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the extracted method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Rename Class-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move and rename class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Move Method-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Extract Variable-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the extract method - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            checkForMergeConditionalOnTop(refactorings, replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Merge Conditional-";

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Merge Conditional on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "One of the overlapping cases - all mapped", purityComment, mappingState);
            }

            sizeBefore = replacementsToCheck.size();
            relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
            sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "- Encapsulate Opposite-";

            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Getter method got replaced with direct access or vice verca - all mapped", purityComment, mappingState);
            }

            int size1 = replacementsToCheck.size();
            int numberOfArgumentReplacedWithReturnReplacements = 0;

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                    numberOfArgumentReplacedWithReturnReplacements++;
                }
            }

            if (numberOfArgumentReplacedWithReturnReplacements == size1) {
                return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
            }



            purityComment += " Severe changes";

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

//        Check non-mapped leaves
        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()){

            int mappingState = 2;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());
            List<AbstractCodeFragment> nonMappedLeavesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());
            int size = nonMappedLeavesT2.size();
            checkForInevitableVariableDeclaration(refactoring.getBodyMapper(), nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements
            int size2 = nonMappedLeavesT2.size();

            if (size != size2) {
                purityComment += "Severe changes + \n";
            }

            CheckReplacementResult checkReplacementResultVar = checkReplacements(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                purityComment = checkReplacementResultVar.getPurityCommentReplacement() + " Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", purityComment, mappingState);
            }

            int size4 = nonMappedLeavesT2.size();
            int returnStatementCounter0 = 0;

            if (size4 == returnStatementCounter0) {
                return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", checkReplacementResultVar.getPurityCommentReplacement(), mappingState);
            }


            purityComment = checkReplacementResultVar.getPurityCommentReplacement() + "\n" + "Overlapped refactoring - can be identical by undoing the overlapped refactoring";

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty()) {
                purityComment = purityComment + "- Rename Method-";
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - with non-mapped leaves", purityComment, mappingState);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                purityComment = purityComment + "- Extract Variable-";
                return new PurityCheckResult(true, "Extract Variable on the top of the extracted method - with non-mapped leaves", purityComment, mappingState);
            }

//            TODO - MoveAndRenameMethod refactoring on top of the extract method can cause a non-mapped leaf.
            purityComment = checkReplacementResultVar.getPurityCommentReplacement() + " Severe changes";

            checkForPrint_NonMapped(nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Extra print lines - with non-mapped leaves", purityComment, mappingState);
            }

            checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Mapped statements in other refactorings - with non-mapped leaves", purityComment, mappingState);
            }

            checkVariableDeclarationUsage(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "The new variable declared has not been used within the program logic - with non-mapped leaves", purityComment, mappingState);
            }

            int size3 = nonMappedLeavesT2.size();
            int returnStatementCounter = 0;

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter++;
                }
            }

            if (size3 == returnStatementCounter) {
                return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", purityComment, mappingState);
            }


            return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves", purityComment, mappingState);
        } else {

            int mappingState = 3;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT2List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());
            List<AbstractCodeFragment> nonMappedLeavesT1List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());


            CheckReplacementResult checkReplacementResultVar = checkReplacements(refactoring, refactorings, modelDiff);
            if (!checkReplacementResultVar.isReplacementResult()) {
                purityComment = checkReplacementResultVar.getPurityCommentReplacement() + " Severe changes";
                return new PurityCheckResult(false, "Replacements are not justified - non-mapped inner nodes", purityComment, mappingState);
            }

            purityComment = checkReplacementResultVar.getPurityCommentReplacement() + " Severe changes";

            if (!checkNonMappedLeaves(refactoring, refactorings, nonMappedLeavesT2List, nonMappedLeavesT1List)) {
                return new PurityCheckResult(false, "Non-mapped leaves are not justified - non-mapped inner nodes", purityComment, mappingState);
            }



            List<AbstractCodeFragment> nonMappedInnerNodesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT2());

            int numberOfWrongNonMappedBlocks = 0;
            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    numberOfWrongNonMappedBlocks++;
                }else {
                    break;
                }
            }

            if (numberOfWrongNonMappedBlocks == nonMappedInnerNodesT2.size()) {
                return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", purityComment, mappingState);
            }



            checkForNodesBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedInnerNodesT2);

            if (nonMappedInnerNodesT2.isEmpty()) {
                return new PurityCheckResult(true, "Nodes being mapped with other nodes in other refactorings - with non-mapped leaves", purityComment, mappingState);
            }

            checkForIfTrueCondition(refactoring, nonMappedInnerNodesT2);

            if (nonMappedInnerNodesT2.isEmpty()) {
                return new PurityCheckResult(true, "Non-changing if statement has been added - with non-mapped leaves", purityComment, mappingState);
            }

            int sizeBefore = nonMappedInnerNodesT2.size();
            checkForSplitConditionOnTopNonMapped(refactoring, refactorings, nonMappedInnerNodesT2);
            int sizeAfter = nonMappedInnerNodesT2.size();

            if (sizeAfter != sizeBefore)
                purityComment += " - Split Conditional - ";

//            checkForIfCondition(refactoring, nonMappedInnerNodesT2); //For the big commit - https://github.com/robovm/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07 - and the cryptoOperation extract method

            int size = nonMappedInnerNodesT2.size();
            int blockStatementCounter = 0;

            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    blockStatementCounter++;
                }
            }

            if (size == blockStatementCounter) {
                return new PurityCheckResult(true, "Empty block - with non-mapped leaves", purityComment, mappingState);
            }


            return new PurityCheckResult(false, "Contains non-mapped inner nodes", purityComment, mappingState);
        }
    }

    private static void checkForSplitConditionOnTopNonMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedInnerNodesT2) {
        List<AbstractCodeFragment> nonMappedInnerNodesT2ToRemove = new ArrayList<>();
        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1 instanceof SplitConditionalRefactoring) {
                        if (((SplitConditionalRefactoring) refactoring1).getSplitConditionals().contains(abstractCodeFragment)) {
                            nonMappedInnerNodesT2ToRemove.add(abstractCodeFragment);
                        }
                    }
                }
            }
        }
        nonMappedInnerNodesT2.removeAll(nonMappedInnerNodesT2ToRemove);
    }

    private static void checkForSplitConditionOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        String probableBeforeReplacement = "";
        Set<String> probableAfterReplacements = new HashSet<>();
        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Refactoring refactoring : refactorings) {
            if (refactoring instanceof SplitConditionalRefactoring) {
                probableBeforeReplacement = ((SplitConditionalRefactoring) refactoring).getOriginalConditional().getString();
                probableAfterReplacements =  ((SplitConditionalRefactoring) refactoring).getSplitConditionals().stream().map(AbstractCodeFragment::getString).collect(Collectors.toSet());
            }
        }

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getBefore().equals(probableBeforeReplacement)) {
                for (String probableAfterReplacement : probableAfterReplacements) {
                    if (probableAfterReplacement.equals(replacement.getAfter())) {
                        replacementsToRemove.add(replacement);
                        break;
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void relaxCheckForGetterMethodReplacedWithDirectAccess(HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {

            String before = replacement.getBefore();
            String after = replacement.getAfter();
            String probableGetterMethod = "";

            int indexOfRightInLeft = before.toLowerCase().indexOf(after.toLowerCase());
            int indexOfLeftInRight = after.toLowerCase().indexOf(before.toLowerCase());
            int theDotBeforeIndex;
            int theDotAfterIndex;

            if (indexOfRightInLeft != -1) {
                theDotBeforeIndex = before.substring(0, indexOfRightInLeft).lastIndexOf(".");
                theDotAfterIndex = before.substring(indexOfRightInLeft).indexOf(".");

                if (theDotBeforeIndex != -1 && theDotAfterIndex != -1) {
                    probableGetterMethod = before.substring(theDotBeforeIndex + 1, theDotAfterIndex + before.substring(0, indexOfRightInLeft).length());
                } else if (theDotAfterIndex != -1) { //The DotBeforeIndex is -1
                    probableGetterMethod = before.substring(0, theDotAfterIndex + before.substring(0, indexOfRightInLeft).length());
                } else if (theDotBeforeIndex != -1) {
                    probableGetterMethod = before.substring(theDotBeforeIndex);
                } else {
                    probableGetterMethod = before;
                }

                if (probableGetterMethod.contains("get") && !after.contains("get")) { //Relax Search - It has been added because this change happened a lot
                    replacementsToRemove.add(replacement);
                }


            } else if (indexOfLeftInRight != -1) {
                theDotBeforeIndex = after.substring(0, indexOfLeftInRight).lastIndexOf(".");
                theDotAfterIndex = after.substring(indexOfLeftInRight).indexOf(".");

                if (theDotBeforeIndex != -1 && theDotAfterIndex != -1) {
                    probableGetterMethod = after.substring(theDotBeforeIndex, theDotAfterIndex + after.substring(0, indexOfLeftInRight).length());
                } else if (theDotAfterIndex != -1) { //The DotBeforeIndex is -1
                    probableGetterMethod = after.substring(0, theDotAfterIndex + after.substring(0, indexOfLeftInRight).length());
                } else if (theDotBeforeIndex != -1) {
                    probableGetterMethod = after.substring(theDotBeforeIndex);
                } else {
                    probableGetterMethod = after;
                }

                if (probableGetterMethod.contains("get") && !before.contains("get")) { //Relax Search - It has been added because this change happened a lot
                    replacementsToRemove.add(replacement);
                }

            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitAnonymousClassDeclarationReplacements(HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ANONYMOUS_CLASS_DECLARATION)) {
                replacementsToRemove.add(replacement);
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitReturnRelatedReplacements(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            AbstractCodeMapping mapping = findTheMapping(replacement, bodyMapper);
            if (mapping.getFragment1().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT) &&
                    mapping.getFragment2().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) { //Relax check
                if (mapping.getFragment1().getMethodInvocations().size() == mapping.getFragment2().getMethodInvocations().size()
                        && mapping.getFragment1().getCreations().size() == mapping.getFragment2().getCreations().size()) { //Strict check
                    if (mapping.getFragment1().getVariables().size() == mapping.getFragment2().getVariables().size()) { //More strict check
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForMergeVariableOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.MERGE_VARIABLES)) {
                for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                    for (Replacement mappingReplacement : mapping.getReplacements()) {
                        if (mappingReplacement.equals(replacement)) {
                            if (!mapping.getFragment2().getTypes().isEmpty()) {
                                for (Refactoring refactoring1 : refactorings) {
                                    if (refactoring1.getRefactoringType().equals(RefactoringType.MERGE_PARAMETER)) {
                                        if (mapping.getFragment2().getTypes().contains(((MergeVariableRefactoring) (refactoring1)).getOperationAfter().getName()) ||
                                                mapping.getFragment2().getTypes().contains(((MergeVariableRefactoring) (refactoring1)).getOperationBefore().getName())) {
                                            replacementsToRemove.add(replacement);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void adjustTheParameterArgumentFieldSourceOperationAfterExtraction(ExtractOperationRefactoring refactoring) {
        Map<String, String> mp = Map.copyOf(refactoring.getParameterToArgumentMap());
        for (Map.Entry<String, String> entry :mp.entrySet()) {
            if (entry.getValue().equals(entry.getKey())) {
                if (refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()) != null && refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()).getInitializer() != null) {
                    refactoring.getParameterToArgumentMap().put(entry.getKey(), refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()).getInitializer().getExpression());
                }
            }
        }
    }

    private static Set<Replacement> checkForAddParameterInSubExpressionOnTop(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToAdd = new HashSet<>();
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                String subExpressionMethodInvocation = candidateForAddParameter(replacement);
                if (subExpressionMethodInvocation != null) {
                    AbstractCodeMapping mapping = findTheMapping(replacement, bodyMapper);

                    String before = null;
                    String after = null;
                    AbstractCall invokedOperationBefore = null;
                    AbstractCall invokedOperationAfter = null;
                    boolean methodOrCreationFlag = false;


                    for (AbstractCall entry: mapping.getFragment1().getMethodInvocations()) {
                        if (entry.getName().equals(subExpressionMethodInvocation)) {
                            before = entry.actualString();
                            invokedOperationBefore = entry;
                        }
                    }

                    for (AbstractCall entry: mapping.getFragment2().getMethodInvocations()) {
                        if (entry.getName().equals(subExpressionMethodInvocation)) {
                            after = entry.actualString();
                            invokedOperationAfter = entry;
                        }
                    }

                    if (before == null || after == null || invokedOperationBefore == null || invokedOperationAfter == null) {

                        methodOrCreationFlag = true;

                        for (AbstractCall entry: mapping.getFragment1().getCreations()) {
                            if (entry.getName().equals(subExpressionMethodInvocation)) {
                                before = entry.actualString();
                                invokedOperationBefore = entry;
                            }
                        }

                        for (AbstractCall entry: mapping.getFragment2().getCreations()) {
                            if (entry.getName().equals(subExpressionMethodInvocation)) {
                                after = entry.actualString();
                                invokedOperationAfter = entry;
                            }
                        }
                    }

                    if (before == null || after == null || invokedOperationBefore == null || invokedOperationAfter == null) {
                        break;
                    }

                    replacementsToRemove.add(replacement);
                    if (!methodOrCreationFlag)
                        replacementsToAdd.add(new MethodInvocationReplacement(before, after, invokedOperationBefore, invokedOperationAfter, Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT));
                    else
                        replacementsToAdd.add(new MethodInvocationReplacement(before, after, invokedOperationBefore, invokedOperationAfter, Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT));

                }
            }
        }
        replacementsToCheck.addAll(replacementsToAdd);
        replacementsToCheck.removeAll(replacementsToRemove);
        return replacementsToRemove;
    }

    private static AbstractCodeMapping findTheMapping(Replacement replacement, UMLOperationBodyMapper bodyMapper) {
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping.getReplacements().contains(replacement)) {
                return mapping;
            }
        }
        return null;
    }

    private static String candidateForAddParameter(Replacement replacement) {
        if (!((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getName().equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getName())) {
            return null;
        }

        if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().size() != ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().size()) {
            return null;
        }

        for (int i = 0; i < ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().size(); i++) {
            if (!((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().get(i))) {
                if (isMethodInvocation(((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i), ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().get(i))) {

                    int parIndex = ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).indexOf("(");
                    String subExpressionMethodInvocation = ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).substring(0, parIndex);
                    if (subExpressionMethodInvocation.split(" ").length == 2) {
                        String[] nameSplitted = subExpressionMethodInvocation.split(" ");
                        if (nameSplitted[0].toLowerCase().equals("new")) {
                            return nameSplitted[1];
                        }
                    }else {
                        return subExpressionMethodInvocation;
                    }

                }
            }
        }
        return null;
    }

    private static boolean isMethodInvocation(String s, String s1) {
        return s1.contains("(") && s.contains("(");
    }


    private static void omitReplacementRegardingInvertCondition(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        Map<String, String> patterns = new HashMap<>();

        patterns.put(">", "<=");
        patterns.put("<", ">=");
        patterns.put(">=", "<");
        patterns.put("<=", ">");
        patterns.put("=", "!=");
        patterns.put("!=", "=");

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.INFIX_OPERATOR)) {
                if (invertingCheck(replacement, patterns)) {
                    AbstractCodeMapping mapping = findCorrespondingMappingForAReplacement(bodyMapper, replacement);
//                    I don't check for the non-emptiness of the non-mapped leaves because there would be some cases that the return statements mapped with other unrelated return statements.
                    if (((CompositeStatementObject) (mapping.getFragment2())).getAllStatements().size() == 2) {
                        for (AbstractStatement allStatement : ((CompositeStatementObject) (mapping.getFragment2())).getAllStatements()) {
                            if (allStatement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }

                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static AbstractCodeMapping findCorrespondingMappingForAReplacement(UMLOperationBodyMapper bodyMapper, Replacement replacement) {
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (replacement.equals(mappingReplacement)) {
                    return mapping;
                }
            }
        }
        return null;
    }


    private static boolean invertingCheck(Replacement replacement, Map<String, String> patterns) {
        for (Map.Entry<String, String> entry: patterns.entrySet()) {
            if (replacement.getBefore().equals(entry.getKey()) && replacement.getAfter().equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static void omitThisPatternReplacements(HashSet<Replacement> replacementsToCheck) throws StringIndexOutOfBoundsException {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
                if (replacement.getBefore().contains("this") || replacement.getAfter().contains("this")) {
                    int findSimilar1 = replacement.getAfter().indexOf(replacement.getBefore());
                    int findSimilar2 = replacement.getBefore().indexOf(replacement.getAfter());
                    if (findSimilar1 != -1) {
                        try {
	                        if (replacement.getAfter().substring(0, findSimilar1 - 1).equals("this")) {
	                            replacementsToRemove.add(replacement);
	                        }
                        } catch (StringIndexOutOfBoundsException ignored) {
                            //System.out.println("ignored");
                        }
                    } else if (findSimilar2 != -1) {
                        try {
                            String temp = replacement.getBefore().substring(0, findSimilar2 - 1);
                            if (temp.equals("this")) {
                                replacementsToRemove.add(replacement);
                            }
                        } catch (StringIndexOutOfBoundsException ignored) {
                            //System.out.println("ignored");
                        }
                    }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void adjustTheParameterArgumentField(ExtractOperationRefactoring refactoring, ExtractOperationRefactoring refactoring1) {
        for (Map.Entry<String, String> stringStringEntry : refactoring.getParameterToArgumentMap().entrySet()) {
            for (Map.Entry<String, String> stringEntry : refactoring1.getParameterToArgumentMap().entrySet()) {
                if (stringStringEntry.getValue().equals(stringEntry.getKey())) {
                    refactoring.getParameterToArgumentMap().put(stringStringEntry.getKey(), stringEntry.getValue());
                    break;
                }
            }
        }
    }

    private static void checkForInevitableVariableDeclaration(UMLOperationBodyMapper bodyMapper, List<AbstractCodeFragment> nonMappedLeavesT2, List<Refactoring> refactorings) {
        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment nonMappedLeaf : nonMappedLeavesT2) {
            if (!nonMappedLeaf.getVariableDeclarations().isEmpty()) {
                List<VariableDeclaration> variableDeclarations = nonMappedLeaf.getVariableDeclarations();
                for (VariableDeclaration variableDeclaration : variableDeclarations) {
                    if (checkUsageOfTheNewVariableDeclaration(variableDeclaration, bodyMapper, refactorings)) {
                        nonMappedLeavesT2ToRemove.add(nonMappedLeaf);
                        for (Replacement replacement : bodyMapper.getReplacements()) {
                            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                            replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                                bodyMapper.getReplacements().remove(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void checkForInevitableVariableDeclarationInline(InlineOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedLeavesT1, List<Refactoring> refactorings) {
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment nonMappedLeaf : nonMappedLeavesT1) {
            if (!nonMappedLeaf.getVariableDeclarations().isEmpty()) {
                List<VariableDeclaration> variableDeclarations = nonMappedLeaf.getVariableDeclarations();
                for (VariableDeclaration variableDeclaration : variableDeclarations) {
                    if (checkForTheSameDeclarationInTheTargetMethod(refactoring, variableDeclaration)) {
                        nonMappedLeavesT1ToRemove.add(nonMappedLeaf);
                        continue;
                    }
                    if (checkUsageOfTheNewVariableDeclaration(variableDeclaration, refactoring.getBodyMapper(), refactorings)) {
                        nonMappedLeavesT1ToRemove.add(nonMappedLeaf);
                        for (Replacement replacement : refactoring.getBodyMapper().getReplacements()) {
                            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                                    replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                                refactoring.getReplacements().remove(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static boolean checkForTheSameDeclarationInTheTargetMethod(InlineOperationRefactoring refactoring, VariableDeclaration variableDeclaration) {
        for (VariableDeclaration variableDeclaration1: refactoring.getTargetOperationAfterInline().getBody().getAllVariableDeclarations()) {
            if (variableDeclaration.equalVariableDeclarationType(variableDeclaration1) && variableDeclaration.equalQualifiedType(variableDeclaration1) && (variableDeclaration.getInitializer().equalFragment(variableDeclaration1.getInitializer()) || variableDeclaration1.getInitializer() == null )) {
                return true;
            }
        }
        return false;
    }


    private static boolean checkUsageOfTheNewVariableDeclaration(VariableDeclaration variableDeclaration, UMLOperationBodyMapper bodyMapper, List<Refactoring> refactorings) {
        boolean existFlag = false;
        boolean checkFlag = false;

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping.getFragment2().getString().contains(variableDeclaration.getVariableName())) {
                existFlag = true;
                if (variableDeclaration.getInitializer() == null || variableDeclaration.getInitializer().getExpression().equals("null")
                || variableDeclaration.getInitializer().getExpression().equals("0"))
                    checkFlag = checkMappingReplacements(mapping, variableDeclaration, refactorings);
                if (!checkFlag) {
                    break;
                }
            }
        }
        if (!existFlag) {
            return true;
        }
        return checkFlag;
    }

    private static boolean checkMappingReplacements(AbstractCodeMapping mapping, VariableDeclaration variableDeclaration, List<Refactoring> refactorings) {
        if (mapping.getReplacements().isEmpty()) {
            return true;
        }

        for (Replacement replacement : mapping.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                if (replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                    return true;
                }

                Set<Replacement> tempReplacement = new HashSet<>();
                tempReplacement.add(replacement);

                checkForRenameVariableOnTop(refactorings, tempReplacement);
                if (tempReplacement.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkForExtractVariableOnTop_NonMapped(List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeaves) {
        List<AbstractCodeFragment> nonMappedLeavesToRemove = new ArrayList<>();
        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeaves) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                    if (abstractCodeFragment.getVariableDeclarations().size() == 1) {
                        if (abstractCodeFragment.getVariableDeclarations().get(0).equals(((ExtractVariableRefactoring) (refactoring1)).getVariableDeclaration())) {
                            nonMappedLeavesToRemove.add(abstractCodeFragment);
                        }
                    }
                }
            }
        }
        nonMappedLeaves.removeAll(nonMappedLeavesToRemove);
    }

    private static void checkForExtractVariableOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Map<String, String> parameterToArgumentMap = null;
        if (refactoring instanceof ExtractOperationRefactoring) {
            parameterToArgumentMap = ((ExtractOperationRefactoring) (refactoring)).getParameterToArgumentMap();
        } else if (refactoring instanceof InlineOperationRefactoring) {
            parameterToArgumentMap = ((InlineOperationRefactoring) (refactoring)).getParameterToArgumentMap();;
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION) ||
                refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION) ||
                refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)  || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {

            Set<Replacement> replacementsToRemove = new HashSet<>();

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                    if (((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer() != null) {
                        for (Replacement replacement : replacementsToCheck) {
                            if (replacement.getBefore().equals((((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getExpression())) &&
                            replacement.getAfter().equals(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName())) {
                                replacementsToRemove.add(replacement);
                            }
                        }
                    }
                }
            }
            replacementsToCheck.removeAll(replacementsToRemove);
            return;
        }else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        Map<String, String> patterns = new HashMap<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                if (((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer() != null) {
                    ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().replaceParametersWithArguments(parameterToArgumentMap);
                    patterns.put(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getArgumentizedString(), ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName());
                    patterns.put(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getString(), ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName());
                }
            }
        }

        for (Replacement replacement : replacementsToCheck) {
            for (Map.Entry<String, String> entry: patterns.entrySet()) {
                if (replacement.getBefore().equals(entry.getKey()) && replacement.getAfter().equals(entry.getValue())) {
                    replacementsToRemove.add(replacement);
                    break;
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForIfTrueCondition(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT2) {
        List<AbstractCodeFragment> nonMappedNodesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            abstractCodeFragment.replaceParametersWithArguments(refactoring.getParameterToArgumentMap());
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                if (abstractCodeFragment.getArgumentizedString().equals("if(true)")) {
                    nonMappedNodesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }
        nonMappedInnerNodesT2.removeAll(nonMappedNodesT2ToRemove);
    }

    private static void checkForNodesBeingMappedInOtherRefactorings(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedInnerNodesT2) {
        List<AbstractCodeFragment> nonMappedNodesT2ToRemove = new ArrayList<>();
        String sourceOperation = refactoring.getSourceOperationBeforeExtraction().getName();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) && !refactoring1.equals(refactoring)) {
                    for (AbstractCodeMapping mapping : ((ExtractOperationRefactoring) (refactoring1)).getBodyMapper().getMappings()) {
                        if (mapping.getFragment2().equals(abstractCodeFragment)) {
                            if (mapping.getOperation1().getName().equals(sourceOperation) && mapping.getReplacements().isEmpty()) {
                                nonMappedNodesT2ToRemove.add(mapping.getFragment2());
                                break;
                            }
                        }
                    }
                }
            }
        }
        nonMappedInnerNodesT2.removeAll(nonMappedNodesT2ToRemove);
    }

    private static void omitEqualStringLiteralsReplacement(HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getBefore().replaceAll("\"", "").trim().toLowerCase().equals(replacement.getAfter().replaceAll("\"", "").trim().toLowerCase())) {
                replacementsToRemove.add(replacement);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkVariableDeclarationUsage(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {
        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (VariableDeclaration variableDeclaration : abstractCodeFragment.getVariableDeclarations()) {
                String variableName = variableDeclaration.getVariableName();
                if (checkUsageWithinTheRefactoring(variableName, refactoring, abstractCodeFragment)) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static boolean checkUsageWithinTheRefactoring(String variableName, ExtractOperationRefactoring refactoring, AbstractCodeFragment abstractCodeFragment) {
        List<AbstractStatement> statementList = refactoring.getExtractedOperation().getBody().getCompositeStatement().getAllStatements();
        for (AbstractStatement abstractStatement : statementList) {
            if (abstractStatement.getString().contains(variableName) && !abstractStatement.equalFragment(abstractCodeFragment)) {
                if (!isPrintOrLog(abstractStatement.getString())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void checkForStatementsBeingMappedInOtherRefactorings(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {
        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        String sourceOperation = refactoring.getSourceOperationBeforeExtraction().getName();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) && !refactoring1.equals(refactoring)) {
                    for (AbstractCodeMapping mapping : ((ExtractOperationRefactoring) (refactoring1)).getBodyMapper().getMappings()) {
                        if (mapping.getFragment2().getString().equals(abstractCodeFragment.getString())) {
                            if (mapping.getOperation1().getName().equals(sourceOperation)) {
                                nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                                break;
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void checkForPrint_NonMapped(List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1) {
        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (isPrintOrLog(abstractCodeFragment.getString())) {
                nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
            if (isPrintOrLog(abstractCodeFragment.getString())) {
                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void omitPrintAndLogMessagesRelatedReplacements(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (isPrintOrLog(mapping.getFragment1().getString()) && isPrintOrLog(mapping.getFragment2().getString())) {
                if (!mapping.getReplacements().isEmpty()) {
                    replacementsToRemove.addAll(mapping.getReplacements());
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean isPrintOrLog(String fragmentString) {
        return fragmentString.contains("System.out.println") || fragmentString.contains("System.err.println") ||
                fragmentString.contains("System.out.print") || fragmentString.contains("System.err.print") ||
                fragmentString.contains("System.out.printf") || fragmentString.contains("System.err.printf") ||
                fragmentString.contains("log.trace") || fragmentString.contains("log.tracef") ||
                fragmentString.contains("log.info") || fragmentString.contains("fail");
    }

    private static void checkForExtractMethodOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                        if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(invokedOperationAfterName)) {
                            if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getParameterNameList().size() == ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().arguments().size()) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

//    private static void checkForIfCondition(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT2) {
//
//        List<AbstractCodeFragment> nonMappedInnerNodesToRemove = new ArrayList<>();
//        List<String> conditionVariables = new ArrayList<>();
//
//        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
//            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
//                for (AbstractExpression expression : ((CompositeStatementObject) abstractCodeFragment).getExpressions()) {
//                    conditionVariables.addAll(expression.getVariables());
//                }
//                if (refactoring.getExtractedOperation().getParameterNameList().containsAll(conditionVariables)) {
//                    nonMappedInnerNodesToRemove.add(abstractCodeFragment);
//                }
//            }
//        }
//
//        nonMappedInnerNodesT2.removeAll(nonMappedInnerNodesToRemove);
//
//    }

    private static void checkForRemoveParameterOnTopInline(Refactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring instanceof ExtractOperationRefactoring) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring instanceof InlineOperationRefactoring) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }


        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement: new LinkedHashSet<>(replacementsToCheck)) {
            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForRemoveParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck);
            }
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
//                temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());

                for (String argument : ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments()) {
                    for (String s : temp1) {
                        if (argument.equals(s)) {
                            temp1.remove(s);
                            break;
                        }
                    }
                }
                ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                for (int i = 0; i < temp1.size(); i++) {
                    for (int j = 0; j < temp2.size(); j++) {
                        if (temp1.get(i).equals(temp2.get(j)))
                            removedArgumentsLocationInReplacement.add(j);
                    }
                }

                String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        removeParameterRefactoringList.add(refactoring1);
                    }
                }

                ArrayList<Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                for (Refactoring ref : removeParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RemoveParameterRefactoring) ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring) ref).getParameter().getName());
                            removedArgumentLocationInRefactoring.add(ind);
                        }
                    }
                }
                Collections.sort(removedArgumentsLocationInReplacement);
                Collections.sort(removedArgumentLocationInRefactoring);
                if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                    replacementsToRemove.add(replacement);
                }

            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForRemoveParameterOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement: new LinkedHashSet<>(replacementsToCheck)) {
            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForRemoveParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck);
            }
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {
                Replacement replacement1 = specialRelaxSearchForRemoveParameterOnTop(bodyMapper, ((VariableReplacementWithMethodInvocation) (replacement)), refactorings);
                if (replacement1 != null) {
                    replacementsToRemove.add(replacement1);
                }
            }
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName())) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                    ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                    ArrayList<String> temp2 = new ArrayList<>(temp1);
                    temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());
                    ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                    for (int i = 0; i < temp1.size(); i++) {
                        for (int j = 0; j < temp2.size(); j++) {
                            if (temp1.get(i).equals(temp2.get(j)))
                                removedArgumentsLocationInReplacement.add(j);
                        }
                    }

                    String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                    List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                    for (Refactoring refactoring1 : refactorings) {
                        if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                            removeParameterRefactoringList.add(refactoring1);
                        }
                    }

                    ArrayList<Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                    for (Refactoring ref : removeParameterRefactoringList) {
                        if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                            if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((RemoveParameterRefactoring) ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring) ref).getParameter().getName());
                                removedArgumentLocationInRefactoring.add(ind);
                            }
                        }
                    }
                    Collections.sort(removedArgumentsLocationInReplacement);
                    Collections.sort(removedArgumentLocationInRefactoring);
                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                        replacementsToRemove.add(replacement);
                    }

            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void relaxCheckForRemoveParameterOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName())) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                int numberOfParametersBefore = ((MethodInvocationReplacement)replacement).getInvokedOperationBefore().arguments().size();
                int numberOfParametersAfter = ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().arguments().size();

                String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        removeParameterRefactoringList.add(refactoring1);
                    }
                }

                int counter = 0;
                for (Refactoring ref : removeParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            counter++;
                        }
                    }
                }

                if (counter == (numberOfParametersBefore - numberOfParametersAfter) && counter != 0) {
                    replacementsToRemove.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static Replacement specialRelaxSearchForRemoveParameterOnTop(UMLOperationBodyMapper bodyMapper, VariableReplacementWithMethodInvocation replacement, List<Refactoring> refactorings) {
        String pattern = "(\\w+)\\((.*)\\)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(replacement.getAfter());
        if (m.find()) {
            String methodName = m.group(1);
            if (methodName.equals(replacement.getInvokedOperation().getName())) {
                String argsString = m.group(2);
                String[] args = argsString.split(",");
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                ArrayList<String> temp1 = new ArrayList<>(replacement.getInvokedOperation().arguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
                temp1.removeAll(List.of(args));


                ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                for (int i = 0; i < temp1.size(); i++) {
                    for (int j = 0; j < temp2.size(); j++) {
                        if (temp1.get(i).equals(temp2.get(j)))
                            removedArgumentsLocationInReplacement.add(j);
                    }
                }

                List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        removeParameterRefactoringList.add(refactoring1);
                    }
                }

                ArrayList<Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                for (Refactoring ref : removeParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RemoveParameterRefactoring) ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring) ref).getParameter().getName());
                            removedArgumentLocationInRefactoring.add(ind);
                        }
                    }
                }
                Collections.sort(removedArgumentsLocationInReplacement);
                Collections.sort(removedArgumentLocationInRefactoring);
                if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                    return replacement;
                }
            }
        }else {
            return null;
        }
        return null;
    }

    private static void checkForRemoveParameterOnTopConstructorVersion(UMLOperationBodyMapper bodyMapper, Replacement replacement, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (mappingReplacement.equals(replacement)) {
                    Optional<AbstractCall> actualValue1 = mapping.getFragment1().getCreations()
                            .stream()
                            .findFirst();

                    if (actualValue1.isPresent()) {
                        ArrayList<String> temp1 = new ArrayList<>(actualValue1.get().arguments());
                        ArrayList<String> temp2 = new ArrayList<>(temp1);
                        Optional<AbstractCall> actualValue2 = mapping.getFragment2().getCreations()
                                .stream()
                                .findFirst();
                        actualValue2.ifPresent(stringListEntry -> temp1.removeAll(stringListEntry.arguments()));

                        ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                        for (int i = 0; i < temp1.size(); i++) {
                            for (int j = 0; j < temp2.size(); j++) {
                                if (temp1.get(i).equals(temp2.get(j)))
                                    removedArgumentsLocationInReplacement.add(j);
                            }
                        }

                        String methodName = actualValue1.get().getName();
                        List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                                removeParameterRefactoringList.add(refactoring1);
                            }
                        }

                        ArrayList <Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                        for (Refactoring ref : removeParameterRefactoringList) {
                            if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                                if (((RemoveParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                                    int ind = ((RemoveParameterRefactoring)ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring)ref).getParameter().getName());
                                    removedArgumentLocationInRefactoring.add(ind);
                                }
                            }
                        }


//                    In case of presence of constructors, it might there are multiple constructors. Like calling overloaded methods. So, in this case the location of
//                    the removed parameter may not be deterministic. So, in this case, we just check the size of the two lists.

//                    Collections.sort(removedArgumentsLocationInReplacement);
//                    Collections.sort(removedArgumentLocationInRefactoring);
//                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
//                        replacementsToCheck.remove(replacement);
//                    }

                        if ((removedArgumentsLocationInReplacement.size() == removedArgumentLocationInRefactoring.size()) && !removedArgumentsLocationInReplacement.isEmpty()) {
                            replacementsToCheck.remove(replacement);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void checkForParameterArgumentPair(ExtractOperationRefactoring refactoring, Set<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
                List<String> invokedAfterArguments = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments();
                List<String> extractedOperationArguments = refactoring.getExtractedOperation().getParameterNameList();

                if (invokedAfterArguments.containsAll(extractedOperationArguments) && extractedOperationArguments.containsAll(invokedAfterArguments) &&
                ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().size() == ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments().size()) {
                    replacementsToRemove.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitReplacementsRegardingInvocationArguments(ExtractOperationRefactoring refactoring, Set<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        List<String> extractedOperationInvocationArguments = new ArrayList<>(refactoring.getParameterToArgumentMap().values());
        for (Replacement replacement : replacementsToCheck) {
            if (extractedOperationInvocationArguments.contains(replacement.getAfter()) && (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) ||
                    (extractedOperationInvocationArguments.contains(replacement.getAfter()) && replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION))) {
                replacementsToRemove.add(replacement);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean checkNonMappedLeaves(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1) {
        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
        checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);

        checkForInevitableVariableDeclaration(refactoring.getBodyMapper(), nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements



        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForPrint_NonMapped(nonMappedLeavesT2, nonMappedLeavesT1);
        checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);

        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        int size = nonMappedLeavesT2.size();
        int returnStatementCounter = 0;

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                returnStatementCounter++;
            }
        }

        if (size == returnStatementCounter) {
            return true;
        }

//        if (nonMappedLeavesT2.size() == 1) {
//            AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
//            if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
//                if (((StatementObject) nonMappedLeave).getTernaryOperatorExpressions().isEmpty())
//                    return true;
//            }
//        }
        return false;
    }

    private static void checkForExtractClassOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        Set<Replacement> handledReplacements = new HashSet<>();
        Map<String, String> patterns = findPatternsExtractClass(refactorings);

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {

                if (((replacement.getBefore().lastIndexOf("(") == -1)) || ((replacement.getAfter().lastIndexOf("(") == -1))) {
                    if (patterns.containsKey(replacement.getBefore()) && patterns.containsValue(replacement.getAfter())) {
                        handledReplacements.add(replacement);
                    }
                } else if (patterns.containsKey(replacement.getBefore().substring(0,replacement.getBefore().lastIndexOf("("))) && patterns.containsValue(replacement.getAfter().substring(0,replacement.getAfter().lastIndexOf("(")))) {
                    handledReplacements.add(replacement);
                }

            } else if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {

            }else if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {

            } else if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                for (Map.Entry<String, String> entry: patterns.entrySet()) {
                    if (entry.getKey().equals(replacement.getBefore()) && entry.getValue().equals(replacement.getAfter())) {
                        handledReplacements.add(replacement);
                    }
                }
            }

        }
        replacementsToCheck.removeAll(handledReplacements);
    }

    private static Map<String, String> findPatternsExtractClass(List<Refactoring> refactorings) {
        Map<String, String> patterns = new HashMap<>();

        for (Refactoring refactoring: refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_CLASS)) {

                String extractedClassName = ((ExtractClassRefactoring) refactoring).getExtractedClass().getNonQualifiedName();

                for (Map.Entry<UMLOperation, UMLOperation> operation: ((ExtractClassRefactoring) refactoring).getExtractedOperations().entrySet()) {
                    patterns.put(operation.getKey().getName(), operation.getValue().getName());
                    patterns.put(operation.getKey().getName(), extractedClassName + "." + operation.getValue().getName());
                    patterns.put(((ExtractClassRefactoring) refactoring).getOriginalClass().getNonQualifiedName(), ((ExtractClassRefactoring) refactoring).getExtractedClass().getNonQualifiedName());
                    // TODO: 8/3/2022 Think about more possible patterns
                }
            } else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_SUPERCLASS)) {

                String extractedClassName = ((ExtractSuperclassRefactoring) refactoring).getExtractedClass().getNonQualifiedName();
                for (UMLClass subClassBefore : ((ExtractSuperclassRefactoring) refactoring).getUMLSubclassSetBefore()) {
                    patterns.put(subClassBefore.getNonQualifiedName(), extractedClassName);
                }
            }
        }
        return patterns;
    }

    private static void checkForMoveAttributeOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        Set<Replacement> handledReplacements = new HashSet<>();
        ArrayList<String> patterns = findPatternsMoveAttribute(refactorings);

        if (!patterns.isEmpty()) {

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                    String pattern = replacement.getBefore() + replacement.getAfter();
                    if (patterns.contains(pattern)) {
                        handledReplacements.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(handledReplacements);
    }

    private static ArrayList<String> findPatternsMoveAttribute(List<Refactoring> refactorings) {
        ArrayList<String> patterns = new ArrayList<>();

        for (Refactoring refactoring: refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_ATTRIBUTE) ||
                    refactoring.getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE)) {

                String classNameBefore = ((MoveAttributeRefactoring) refactoring).getOriginalAttribute().getNonQualifiedClassName();
                String before = ((MoveAttributeRefactoring) refactoring).getOriginalAttribute().getName();

                String classNameAfter = ((MoveAttributeRefactoring) refactoring).getMovedAttribute().getNonQualifiedClassName();
                String after = ((MoveAttributeRefactoring) refactoring).getMovedAttribute().getName();

                patterns.add(classNameBefore + "." + before + after);
                patterns.add("this." + before + after);
                patterns.add(before + after);
                patterns.add(before + classNameAfter + "." + after);
                patterns.add(classNameBefore + "." + before + after);
                patterns.add(classNameBefore + "." + before + classNameAfter + "." + after);
                patterns.add("this." + before + classNameAfter + "." + after);
            }
        }
        return patterns;
    }

    private static void checkForRenameAttributeOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                for (Refactoring refactoring1: refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE)) {
                        if (replacement.getBefore().equals(((RenameAttributeRefactoring)refactoring1).getOriginalAttribute().getName()) &&
                                replacement.getAfter().equals(((RenameAttributeRefactoring)refactoring1).getRenamedAttribute().getName())) {
                            handledReplacements.add(replacement);
                            break;
                        }

//                        This second check is kind of a relax check to cover the cases contain "this." pattern. It can happen in cases of Rename Attribute.
                        if (replacement.getBefore().contains(((RenameAttributeRefactoring)refactoring1).getOriginalAttribute().getName()) &&
                                replacement.getAfter().contains(((RenameAttributeRefactoring)refactoring1).getRenamedAttribute().getName())) {
                            handledReplacements.add(replacement);
                            break;
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(handledReplacements);
    }

    private static CheckReplacementResult checkReplacements(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        HashSet<Replacement> replacementsToCheck;
        String purityComment = "";

//            This method also checks for the exact matches when we have Type Replacement
        if (allMappingsArePurelyMatched(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap())) {
            purityComment = "Changes are within the Extract Method refactoring mechanics";
            return new CheckReplacementResult(true, purityComment);
        }
        else {
            replacementsToCheck = new HashSet<>(refactoring.getReplacements());
            int sizeBefore = replacementsToCheck.size();
            replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getSourceOperationAfterExtraction().getName()) &&
                            !((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getExtractedOperation().getName())) {

                        adjustTheParameterArgumentField(refactoring, (ExtractOperationRefactoring) refactoring1);
                        replacementsToCheck = omitReplacementsRegardingExactMappings(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);
                    }
                }
            }

//            omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
            replacementsToCheck = omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            int sizeAfter = replacementsToCheck.size();

            if (sizeAfter != sizeBefore)
                purityComment += "Changes are within the Extract Method refactoring mechanics \n";

            omitThisPatternReplacements(replacementsToCheck);
            checkForParameterArgumentPair(refactoring, replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitBooleanVariableDeclarationReplacement(replacementsToCheck, refactoring.getBodyMapper()); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);

            omitReplacementRegardingInvertCondition(replacementsToCheck, refactoring.getBodyMapper());
//            omitReturnRelatedReplacements(replacementsToCheck, refactoring.getBodyMapper());
            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);
            omitPrimitiveTypeReplacements(replacementsToCheck);
            checkForThisPatternReplacement(replacementsToCheck);

            int sizeAfterTolarable = replacementsToCheck.size();

            if (sizeAfterTolarable != sizeAfter)
                purityComment += "Tolerable Changes in the body \n";


            omitMoveMethodRelatedReplacements(refactoring, refactorings, replacementsToCheck, modelDiff);


        }


        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring"  + "\n";

        int sizeBefore = replacementsToCheck.size();
        checkForInlineVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        int sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Inline Variable-";

        sizeBefore = replacementsToCheck.size();
        checkForSplitConditionOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Split Condition-";

        if (replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);


        sizeBefore = replacementsToCheck.size();
        checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Pull Up Method-";

        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsualte Attribute-";


        sizeBefore = replacementsToCheck.size();
        checkForReplaceAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Replace Attribute-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, Collections.emptySet(), refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        Set<Replacement> removedReplacements = checkForAddParameterInSubExpressionOnTop(replacementsToCheck, refactoring.getBodyMapper());
        sizeBefore = replacementsToCheck.size();
        checkForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        relaxCheckForParametrizationOrAddParameterOnTop(refactorings, replacementsToCheck, removedReplacements, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Add Parameter-";

        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        sizeBefore = replacementsToCheck.size();
        checkForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        relaxCheckForRemoveParameterOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Remove Parameter-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Variable-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Method-";

        sizeBefore = replacementsToCheck.size();
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Attribute-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Move Attribute-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Attribute-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

//        checkForRemoveVariableOnTop(replacementsToCheck, refactoring.getBodyMapper());
//        if (replacementsToCheck.isEmpty())
//            return true;
        sizeBefore = replacementsToCheck.size();
        checkForMergeVariableOnTop(refactorings, replacementsToCheck, refactoring.getBodyMapper());
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Merge Variable-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Rename Class-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForExtractMethodOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Method-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Extract Variable-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        sizeBefore = replacementsToCheck.size();
        checkForMergeConditionalOnTop(refactorings, replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Merge Conditional-";

        if(replacementsToCheck.isEmpty())
            return new CheckReplacementResult(true, purityComment);

        omitReplacementsAccordingSupplierGetPattern(refactoring.getBodyMapper().getMappings(), refactoring.getParameterToArgumentMap(), replacementsToCheck);

        checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
        sizeBefore = replacementsToCheck.size();
        relaxCheckForGetterMethodReplacedWithDirectAccess(replacementsToCheck);
        sizeAfter = replacementsToCheck.size();

        if (sizeAfter != sizeBefore)
            purityComment += "- Encapsulate Opposite-";

//            for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
        if (replacementsToCheck.isEmpty()) {
            return new CheckReplacementResult(true, purityComment);
        }

        int size1 = replacementsToCheck.size();
        int numberOfArgumentReplacedWithReturnReplacements = 0;

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                numberOfArgumentReplacedWithReturnReplacements++;
            }
        }

        if (numberOfArgumentReplacedWithReturnReplacements == size1) {
            return new CheckReplacementResult(true, purityComment);
        }

        return new CheckReplacementResult(false, purityComment);
    }

    private static void checkForMergeConditionalOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.MERGE_CONDITIONAL)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.COMPOSITE)
                            || replacement.getType().equals(Replacement.ReplacementType.CONDITIONAL)) {
                        if (replacement.getAfter().equals(((MergeConditionalRefactoring) (refactoring1)).getNewConditional().getString()) ||
                                replacement.getAfter().equals(((MergeConditionalRefactoring) (refactoring1)).getNewConditional().getArgumentizedString())) {
                            replacementsToRemove.add(replacement);
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitBooleanVariableDeclarationReplacement(HashSet<Replacement> replacementsToCheck, UMLOperationBodyMapper bodyMapper) {
        // For the runTests commit, boolean result = false need to map with boolean result = true
        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.BOOLEAN_LITERAL)) {
                for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                    for (Replacement mappingReplacement : mapping.getReplacements()) {
                        if (mappingReplacement.equals(replacement)) {
                            if (checkForBooleanLiteralChangeInDeclaration(mapping)) {
                                replacementsToRemove.add(mappingReplacement);
                                break;
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean checkForBooleanLiteralChangeInDeclaration(AbstractCodeMapping mapping) {
        if (mapping.getFragment1().getVariableDeclarations().isEmpty() || mapping.getFragment2().getVariableDeclarations().isEmpty()) {
            return false;
        }

        if (mapping.getFragment1().getVariableDeclarations().size() == 1) {
            VariableDeclaration declaration = mapping.getFragment1().getVariableDeclarations().get(0);
            return declaration.getType().getClassType().equals("boolean");
        }

        return false;
    }

    private static void checkForRenameVariableOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_VARIABLE) || refactoring1.getRefactoringType().equals(RefactoringType.RENAME_PARAMETER) ||
                            refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_ATTRIBUTE) || refactoring1.getRefactoringType().equals(RefactoringType.REPLACE_ATTRIBUTE_WITH_VARIABLE) ||
                            refactoring1.getRefactoringType().equals(RefactoringType.LOCALIZE_PARAMETER) || refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                        if (replacement.getBefore().equals(((RenameVariableRefactoring) refactoring1).getOriginalVariable().getVariableName()) &&
                                replacement.getAfter().equals(((RenameVariableRefactoring) refactoring1).getRenamedVariable().getVariableName())) {
                            handledReplacements.add(replacement);
                            break;
                        }
                        if (replacement.getBefore().startsWith("this.") && replacement.getAfter().startsWith("this.")) {
                            try {
                                if (replacement.getBefore().substring(5).equals(((RenameVariableRefactoring) refactoring1).getOriginalVariable().getVariableName()) &&
                                        replacement.getAfter().substring(5).equals(((RenameVariableRefactoring) refactoring1).getRenamedVariable().getVariableName())) {
                                    handledReplacements.add(replacement);
                                    break;
                                }
                            } catch (StringIndexOutOfBoundsException e) {
                                System.out.println(e);
                            }
                        } else if (replacement.getBefore().startsWith("this.")) {
                            try {
                                if (replacement.getBefore().substring(5).equals(((RenameVariableRefactoring) refactoring1).getOriginalVariable().getVariableName()) &&
                                        replacement.getAfter().equals(((RenameVariableRefactoring) refactoring1).getRenamedVariable().getVariableName())) {
                                    handledReplacements.add(replacement);
                                    break;
                                }
                            } catch (StringIndexOutOfBoundsException e) {
                                System.out.println(e);
                            }
                        } else if (replacement.getAfter().startsWith("this.")) {
                            try {
                                if (replacement.getBefore().equals(((RenameVariableRefactoring) refactoring1).getOriginalVariable().getVariableName()) &&
                                        replacement.getAfter().substring(5).equals(((RenameVariableRefactoring) refactoring1).getRenamedVariable().getVariableName())) {
                                    handledReplacements.add(replacement);
                                    break;
                                }
                            } catch (StringIndexOutOfBoundsException e) {
                                System.out.println(e);
                            }
                        }
                    }
                }
            }

//        For handling: https://github.com/crashub/crash/commit/2801269c7e47bd6e243612654a74cee809d20959. When we have extracted some part of an expression.
//        for (Replacement replacement: replacementsToCheck) {
//            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
//                if (refactoring.getExtractedOperation().getParameterNameList().contains(replacement.getAfter())) {
//                    handledReplacements.add(replacement);
//                }
//            }
//        }

        }
        replacementsToCheck.removeAll(handledReplacements);
    }


    private static void relaxCheckForParametrizationOrAddParameterOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, Set<Replacement> removedReplacements, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        Set<Replacement> replacementsToAddSpecific = new HashSet<>();
        Set<Replacement> replacementsToRemoveSpecific = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ADD_VARIABLE)) {
                AbstractCodeMapping mapping = findTheMapping(replacement, bodyMapper);
                for (AbstractCall methodInvocation : mapping.getFragment2().getMethodInvocations()) {
                    for (String addedVariable : (((AddVariableReplacement) replacement).getAddedVariables())) {
                        if (methodInvocation.arguments().contains(addedVariable)) {
                            for (AbstractCall invocation : mapping.getFragment1().getMethodInvocations()) {
                                if (methodInvocation.getName().equals(invocation.getName())) {
                                    replacementsToAddSpecific.add(new MethodInvocationReplacement(invocation.actualString(), methodInvocation.actualString(), invocation, methodInvocation, Replacement.ReplacementType.METHOD_INVOCATION));
                                    replacementsToRemoveSpecific.add(replacement);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.addAll(replacementsToAddSpecific);
        replacementsToCheck.removeAll(replacementsToRemoveSpecific);

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                relaxCheckForAddParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck, removedReplacements);
            }

            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) || (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT))) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());
                ArrayList<String> temp2 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                ArrayList<Integer> addedArgumentsLocation = new ArrayList<>();

                for (int i = temp1.size() - 1; i >= temp2.size(); i--) {
                    addedArgumentsLocation.add(i);
                }

                String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
//                List<RenameVariableRefactoring> renameVariableRefactoringList = getSpecificTypeRefactoring(refactorings,RenameVariableRefactoring.class);
                List<Refactoring> parametrizeVariableAndAddParameterRefactoringList = new ArrayList<>();

                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE) ||
                            refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                        parametrizeVariableAndAddParameterRefactoringList.add(refactoring1);
                    }
                }

                ArrayList<Integer> parameterizedAndAddedLocation = new ArrayList<>();

                for (Refactoring ref : parametrizeVariableAndAddParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                        if (((RenameVariableRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RenameVariableRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((RenameVariableRefactoring) ref).getRenamedVariable().getVariableName());
                            if (!parameterizedAndAddedLocation.contains(ind))
                                parameterizedAndAddedLocation.add(ind);
                        }
                    } else {
                        if (((AddParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((AddParameterRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring) ref).getParameter().getName());
                            if (!parameterizedAndAddedLocation.contains(ind))
                                parameterizedAndAddedLocation.add(ind);
                        }
                    }
                }
                Collections.sort(addedArgumentsLocation);
                Collections.sort(parameterizedAndAddedLocation);
                if (addedArgumentsLocation.equals(parameterizedAndAddedLocation) && !addedArgumentsLocation.isEmpty()) {
                    replacementsToRemove.add(replacement);
                }

            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void relaxCheckForAddParameterOnTopConstructorVersion(UMLOperationBodyMapper bodyMapper, Replacement replacement, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, Set<Replacement> removedReplacements) {
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (mappingReplacement.equals(replacement) || removedReplacements.contains(mappingReplacement)) {
                    Optional<AbstractCall> actualValue1 = mapping.getFragment2().getCreations()
                            .stream()
                            .findFirst();

                    Optional<AbstractCall> actualValue2 = mapping.getFragment1().getCreations()
                            .stream()
                            .findFirst();

                    if (actualValue1.isPresent() && actualValue2.isPresent()) {
                        ArrayList<String> temp1 = new ArrayList<>(actualValue1.get().arguments());
                        ArrayList<String> temp2 = new ArrayList<>(actualValue2.get().arguments());

                        ArrayList<Integer> addedArgumentsLocationInReplacement = new ArrayList<>();

                        for (int i = temp1.size() - 1; i >= (temp1.size() - temp2.size()); i--) {
                            addedArgumentsLocationInReplacement.add(i);
                        }

                        String methodName = actualValue1.get().getName();
                        List<Refactoring> addParameterRefactoringList = new ArrayList<>();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                addParameterRefactoringList.add(refactoring1);
                            }
                        }

                        ArrayList <Integer> addedArgumentLocationInRefactoring = new ArrayList<>();

                        for (Refactoring ref : addParameterRefactoringList) {
                            if (ref.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                if (((AddParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                                    int ind = ((AddParameterRefactoring)ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring)ref).getParameter().getName());
                                    addedArgumentLocationInRefactoring.add(ind);
                                }
                            }
                        }


//                    In case of presence of constructors, it might there are multiple constructors. Like calling overloaded methods. So, in this case the location of
//                    the removed parameter may not be deterministic. So, in this case, we just check the size of the two lists.

//                    Collections.sort(removedArgumentsLocationInReplacement);
//                    Collections.sort(removedArgumentLocationInRefactoring);
//                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
//                        replacementsToCheck.remove(replacement);
//                    }

                        if ((addedArgumentsLocationInReplacement.size() == addedArgumentLocationInRefactoring.size()) && !addedArgumentsLocationInReplacement.isEmpty()) {
                            replacementsToCheck.remove(replacement);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void checkForParametrizationOrAddParameterOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, Set<Replacement> removedReplacements, UMLOperationBodyMapper bodyMapper) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        Set<Replacement> replacementsToAddSpecific = new HashSet<>();
        Set<Replacement> replacementsToRemoveSpecific = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ADD_VARIABLE)) {
                AbstractCodeMapping mapping = findTheMapping(replacement, bodyMapper);
                for (AbstractCall methodInvocation : mapping.getFragment2().getMethodInvocations()) {
                    for (String addedVariable : (((AddVariableReplacement) replacement).getAddedVariables())) {
                        if (methodInvocation.arguments().contains(addedVariable)) {
                            for (AbstractCall invocation : mapping.getFragment1().getMethodInvocations()) {
                                if (methodInvocation.getName().equals(invocation.getName())) {
                                    replacementsToAddSpecific.add(new MethodInvocationReplacement(invocation.actualString(), methodInvocation.actualString(), invocation, methodInvocation, Replacement.ReplacementType.METHOD_INVOCATION));
                                    replacementsToRemoveSpecific.add(replacement);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.addAll(replacementsToAddSpecific);
        replacementsToCheck.removeAll(replacementsToRemoveSpecific);

        for (Replacement replacement: new LinkedHashSet<>(replacementsToCheck)) {

            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForAddParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck, removedReplacements);
            }


            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) || (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT))) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                    ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());
                    ArrayList<String> temp2 = new ArrayList<>(temp1);
                    temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                    ArrayList<Integer> addedArgumentsLocation = new ArrayList<>();

                    for (int i = 0; i < temp1.size(); i++) {
                        for (int j = 0; j < temp2.size(); j++) {
                            if (temp1.get(i).equals(temp2.get(j)))
                                addedArgumentsLocation.add(j);
                        }
                    }

                    String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
//                List<RenameVariableRefactoring> renameVariableRefactoringList = getSpecificTypeRefactoring(refactorings,RenameVariableRefactoring.class);
                    List<Refactoring> parametrizeVariableAndAddParameterRefactoringList = new ArrayList<>();

                    for (Refactoring refactoring1 : refactorings) {
                        if (refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE) ||
                                refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                            parametrizeVariableAndAddParameterRefactoringList.add(refactoring1);
                        }
                    }

                    ArrayList<Integer> parameterizedAndAddedLocation = new ArrayList<>();

                    for (Refactoring ref : parametrizeVariableAndAddParameterRefactoringList) {
                        if (ref.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                            if (((RenameVariableRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((RenameVariableRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((RenameVariableRefactoring) ref).getRenamedVariable().getVariableName());
                                if (!parameterizedAndAddedLocation.contains(ind))
                                    parameterizedAndAddedLocation.add(ind);
                            }
                        } else {
                            if (((AddParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((AddParameterRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring) ref).getParameter().getName());
                                if (!parameterizedAndAddedLocation.contains(ind))
                                    parameterizedAndAddedLocation.add(ind);
                            }
                        }
                    }
                    Collections.sort(addedArgumentsLocation);
                    Collections.sort(parameterizedAndAddedLocation);
                    if (addedArgumentsLocation.equals(parameterizedAndAddedLocation) && !addedArgumentsLocation.isEmpty()) {
                        replacementsToRemove.add(replacement);
                    }

            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForAddParameterOnTopConstructorVersion(UMLOperationBodyMapper bodyMapper, Replacement replacement, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, Set<Replacement> removedReplacements) {
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (mappingReplacement.equals(replacement) || removedReplacements.contains(mappingReplacement)) {
                    Optional<AbstractCall> actualValue1 = mapping.getFragment2().getCreations()
                            .stream()
                            .findFirst();

                    if (actualValue1.isPresent()) {
                        ArrayList<String> temp1 = new ArrayList<>(actualValue1.get().arguments());
                        ArrayList<String> temp2 = new ArrayList<>(temp1);
                        Optional<AbstractCall> actualValue2 = mapping.getFragment1().getCreations()
                                .stream()
                                .findFirst();
                        actualValue2.ifPresent(stringListEntry -> temp1.removeAll(stringListEntry.arguments()));

                        ArrayList<Integer> addedArgumentsLocationInReplacement = new ArrayList<>();

                        for (int i = 0; i < temp1.size(); i++) {
                            for (int j = 0; j < temp2.size(); j++) {
                                if (temp1.get(i).equals(temp2.get(j)))
                                    addedArgumentsLocationInReplacement.add(j);
                            }
                        }

                        String methodName = actualValue1.get().getName();
                        List<Refactoring> addParameterRefactoringList = new ArrayList<>();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                addParameterRefactoringList.add(refactoring1);
                            }
                        }

                        ArrayList <Integer> addedArgumentLocationInRefactoring = new ArrayList<>();

                        for (Refactoring ref : addParameterRefactoringList) {
                            if (ref.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                if (((AddParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                                    int ind = ((AddParameterRefactoring)ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring)ref).getParameter().getName());
                                    addedArgumentLocationInRefactoring.add(ind);
                                }
                            }
                        }


//                    In case of presence of constructors, it might there are multiple constructors. Like calling overloaded methods. So, in this case the location of
//                    the removed parameter may not be deterministic. So, in this case, we just check the size of the two lists.

//                    Collections.sort(removedArgumentsLocationInReplacement);
//                    Collections.sort(removedArgumentLocationInRefactoring);
//                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
//                        replacementsToCheck.remove(replacement);
//                    }

                        if ((addedArgumentsLocationInReplacement.size() == addedArgumentLocationInRefactoring.size()) && !addedArgumentsLocationInReplacement.isEmpty()) {
                            replacementsToCheck.remove(replacement);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void checkForRenameMethodRefactoringOnTop_Mapped(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {
        // This method also handles the MoveAndRename Method on top
        // TODO: 8/3/2022 handle "Variable Replaced With Method Invocation case" replacement also
        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME))  {
                if (isRenameWithName(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName(), ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName(), renameOperationRefactoringList)) {
                    handledReplacements.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(handledReplacements);
    }

    private static void checkForRenameRefactoringOnTopOfInline_NonMapped(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {
        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        if (renameOperationRefactoringList.isEmpty()) {
            return;
        }

        for(AbstractCodeFragment abstractCodeFragment2 : refactoring.getBodyMapper().getNonMappedLeavesT1()) {
            List<AbstractCall> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocations();
            List<String> methodCalls2 = methodInvocationMap2.stream().map(l -> l.getName()).collect(Collectors.toList());
            if (!methodCalls2.isEmpty()) {
                for (AbstractCodeFragment abstractCodeFragment : refactoring.getBodyMapper().getNonMappedLeavesT2()) {
                    List<AbstractCall> methodInvocationMap = abstractCodeFragment.getMethodInvocations();
                    List<String> methodCalls = methodInvocationMap.stream().map(l -> l.getName()).collect(Collectors.toList());
                    boolean check = checkRenameMethodCallsPossibility(methodCalls2, methodCalls, renameOperationRefactoringList);
                    if (check) {
                        nonMappedLeavesT1.remove(abstractCodeFragment2);
                        break;
                    }
                }
            }
        }
    }

    private static void checkForRenameRefactoringOnTop_NonMapped(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1) {
        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        if (renameOperationRefactoringList.isEmpty()) {
            return;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)  || refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
            bodyMapper = ((PullUpOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        for(AbstractCodeFragment abstractCodeFragment2 : bodyMapper.getNonMappedLeavesT2()) {
            List<AbstractCall> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocations();
            List<String> methodCalls2 = methodInvocationMap2.stream().map(l -> l.getName()).collect(Collectors.toList());
            if (!methodCalls2.isEmpty())
                for (AbstractCodeFragment abstractCodeFragment : bodyMapper.getNonMappedLeavesT1()) {
                    List<AbstractCall> methodInvocationMap = abstractCodeFragment.getMethodInvocations();
                    List<String> methodCalls = methodInvocationMap.stream().map(l -> l.getName()).collect(Collectors.toList());
                    boolean check = checkRenameMethodCallsPossibility(methodCalls, methodCalls2, renameOperationRefactoringList);
                    if (check) {
                        nonMappedLeavesT2ToRemove.add(abstractCodeFragment2);
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                        break;
                    }
                }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static boolean checkRenameMethodCallsPossibility(List<String> methodCalls1, List<String> methodCalls2, List<Refactoring> renameOperationRefactoringList) {
        if (methodCalls2.size() != methodCalls1.size())
            return false;
        ArrayList<String> mc1Temp = new ArrayList<>(methodCalls1);
        ArrayList<String> mc2Temp = new ArrayList<>(methodCalls2);
        mc1Temp.removeAll(methodCalls2);
        mc2Temp.removeAll(methodCalls1);
        int _renameCounter = mc2Temp.size();
        if (_renameCounter == 0)
            return false;

        for (String call1 : mc1Temp) {
            boolean _met = false;
            for (String call2 : mc2Temp) {
                boolean _check = isRenameWithName(call1,call2,renameOperationRefactoringList);
                if (_check)
                {
                    _met = true;
                    break;
                }
            }
            if (_met)
                _renameCounter -= 1;
        }
        return (_renameCounter == 0);
    }

    private static boolean isRenameWithName(String call1, String call2, List<Refactoring> renameOperationRefactoringList) {
        for(Refactoring renameOperationRefactoring : renameOperationRefactoringList) {
            if (renameOperationRefactoring instanceof RenameOperationRefactoring) {
                if (((RenameOperationRefactoring) (renameOperationRefactoring)).getOriginalOperation().getName().equals(call1)
                        &&
                        ((RenameOperationRefactoring) (renameOperationRefactoring)).getRenamedOperation().getName().equals(call2)) {
                    return true;
                }
            } else {
                if (((MoveOperationRefactoring) (renameOperationRefactoring)).getOriginalOperation().getName().equals(call1)
                        &&
                        ((MoveOperationRefactoring) (renameOperationRefactoring)).getMovedOperation().getName().equals(call2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void omitPrimitiveTypeReplacements(Set<Replacement> replacementsToCheck) {
        Set<Replacement> replacementsToRemove = new HashSet<>();
        for (Replacement rep: replacementsToCheck) {
            if (rep.getType().equals(Replacement.ReplacementType.TYPE)) {
                replacementsToRemove.add(rep);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

	private static boolean allMappingsArePurelyMatched(Set<AbstractCodeMapping> mappings, Map<String, String> parameterToArgumentMap) {
		for (AbstractCodeMapping mapping: mappings) {
			if (!isPurelyExact(mapping, parameterToArgumentMap)) {
				return false;
			}
		}
		return true;
	}

	private static HashSet<Replacement> omitReplacementsAccordingToArgumentization(Map<String, String> parameterToArgumentMap, HashSet<Replacement> replacementsToCheck) {
		Set<Replacement> replacementsToRemove = new HashSet<>();
		for (Replacement replacement: replacementsToCheck) {
			for (Map.Entry<String, String> parameterToArgument: parameterToArgumentMap.entrySet()) {
				if (replacement.getBefore().equals(parameterToArgument.getValue()) &&
				replacement.getAfter().equals(parameterToArgument.getKey())) {
					replacementsToRemove.add(replacement);
				}
			}
		}
		replacementsToCheck.removeAll(replacementsToRemove);
		return replacementsToCheck;
	}

	private static HashSet<Replacement> omitReplacementsRegardingExactMappings(Set<AbstractCodeMapping> mappings, Map<String, String> parameterToArgumentMap, HashSet<Replacement> replacementsToCheck) {
		for (AbstractCodeMapping mapping: mappings) {
			if (isPurelyExact(mapping, parameterToArgumentMap)) {
				replacementsToCheck.removeAll(mapping.getReplacements());
			}
		}
		return replacementsToCheck;
	}

	private static void omitReplacementsAccordingSupplierGetPattern(Set<AbstractCodeMapping> mappings, Map<String, String> parameterToArgumentMap, HashSet<Replacement> replacementsToCheck) {
		Set<Replacement> replacementsToRemove = new HashSet<>();
		for (AbstractCodeMapping mapping : mappings) {
			for (Replacement replacement : mapping.getReplacements()) {
				for (Replacement replacement1 : replacementsToCheck) {
					if (replacement.equals(replacement1)) {
						if (checkForSupplierPattern(mapping, replacement, parameterToArgumentMap)) {
							replacementsToRemove.add(replacement);
						}
					}
				}
			}
		}
		replacementsToCheck.removeAll(replacementsToRemove);
	}

	private static boolean isPurelyExact(AbstractCodeMapping mapping, Map<String, String> parameterToArgumentMap) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		fragment2.replaceParametersWithArguments(parameterToArgumentMap);
		fragment1.replaceParametersWithArguments(parameterToArgumentMap);
		return mapping.isExact();
	}

	private static boolean checkForSupplierPattern(AbstractCodeMapping mapping, Replacement replacement, Map<String, String> parameterToArgumentMap) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		fragment2.replaceParametersWithArguments(parameterToArgumentMap);

		String s1 = fragment1.getString();
		String s2 = fragment2.getArgumentizedString();
		String lambdaArrow = "()" + JAVA.LAMBDA_ARROW;
		String supplierGet = ".get()";
		String tmp = s2.replace(supplierGet, "");
		tmp = tmp.replace(lambdaArrow, "");
		if(s1.equals(tmp)) {
			return true;
		}
		return false;
	}

    private static class CheckReplacementResult {
        public boolean replacementResult;
        public String purityCommentReplacement;

        public CheckReplacementResult(boolean replacementResult, String purityCommentReplacement) {
            this.replacementResult = replacementResult;
            this.purityCommentReplacement = purityCommentReplacement;
        }

        public boolean isReplacementResult() {
            return replacementResult;
        }

        public String getPurityCommentReplacement() {
            return purityCommentReplacement;
        }
    }

    private static class ReplacementJustificationResult {
        private boolean justificationState;
        private String justificationComment;

        public boolean isJustificationState() {
            return justificationState;
        }

        public void setJustificationState(boolean justificationState) {
            this.justificationState = justificationState;
        }

        public String getJustificationComment() {
            return justificationComment;
        }

        public void appendJustificationComment(String comment) {
            this.justificationComment += "\n" + comment;
        }
    }
}