package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiMemberRefactoringTest extends MockMultiMemberRefactoringDependencies {

    protected Set<UMLAttribute> attributesBefore;
    protected Set<UMLAttribute> attributesAfter;
    protected Set<VariableDeclarationContainer> operationsBefore;
    protected Set<VariableDeclarationContainer> operationsAfter;
    private List<MultiMemberRefactoring> refactorings;

    @BeforeEach
    void setUp() {
        /* Initialize Mocks */
        MockitoAnnotations.initMocks(this);
        when(mapper.getOperation1()).thenReturn(extractedMethod);
        when(mapper.getContainer2()).thenReturn(operationAfter);
        /* Initialize Mock Collections */
        attributesBefore = new ArraySet<>(Collections.singletonList(attributeBefore));
        attributesAfter = new ArraySet<>(Collections.singletonList(attributeAfter));
        operationsBefore = new ArraySet<>(Collections.singletonList(operationBefore));
        operationsAfter = new ArraySet<>(Collections.singletonList(operationAfter));
        /* Create List of Refactorings */
        refactorings = new ArrayList<>();
        /* Mock dependencies */
        /* Create ExtractOperationRefactoring and add to list */
        refactorings.add(new ExtractOperationRefactoring(mapper, extractedMethod, operationBefore, operationAfter, operationInvocations));
        /* Create EncapsulateAttributeRefactoring and add to list */
        refactorings.add(new EncapsulateAttributeRefactoring(attributeBefore, attributeAfter, operationBefore, operationAfter));
        /* Create ExtractAttributeRefactoring and add to list */
        refactorings.add(new ExtractAttributeRefactoring(attributeAfter, classBefore, classAfter, insideExtractedOrInlinedMethod));
        /* Create InlineOperationRefactoring and add to list */
        refactorings.add(new InlineOperationRefactoring(mapper, operationBefore, operationInvocations));
        /* Create InlineAttributeRefactoring and add to list */
        refactorings.add(new InlineAttributeRefactoring(attributeBefore, classBefore, classAfter, insideExtractedOrInlinedMethod));
        /* Create MergeAttributeRefactoring and add to list */
        refactorings.add(new MergeAttributeRefactoring(attributesBefore, attributeAfter, classBefore.getName(), classAfter.getName(), candidatesMerge));
        /* Create MergeOperationRefactoring and add to list */
        refactorings.add(new MergeOperationRefactoring(operationsBefore, operationAfter, classBefore.getName(), classAfter.getName(), mappers));
        /* Create SplitAttributeRefactoring and add to list */
        refactorings.add(new SplitAttributeRefactoring(attributeBefore, attributesAfter, classBefore.getName(), classAfter.getName(),candidatesSplit));
        /* Create SplitOperationRefactoring and add to list */
        refactorings.add(new SplitOperationRefactoring(operationBefore, operationsAfter, classBefore.getName(),classAfter.getName(),mappers));
    }

    @Test
    void testGroupByRefactoredMember_And_Count() {
        Map<? super VariableDeclarationContainer, Set<? super MultiMemberRefactoring>> map = mapMemberToRefactoring(refactorings);
        Assertions.assertEquals(5, map.size(), "Expected 5 members to be refactored");
        Assertions.assertEquals(0, map.getOrDefault(null, Collections.emptySet()).size(), "Null should never be a key");
        Assertions.assertEquals(5, map.get(operationBefore).size(), "Expected 5 refactorings to be applied to operations");
        Assertions.assertEquals(5, map.get(operationAfter).size(), "Expected 5 refactored operations");
        Assertions.assertEquals(2, map.get(extractedMethod).size(), "Expected 2 extracted method");
        Assertions.assertEquals(4, map.get(attributeBefore).size(), "Expected 4 refactorings to be applied to attributes");
        Assertions.assertEquals(4, map.get(attributeAfter).size(), "Expected 4 refactored attributes");
    }
}
