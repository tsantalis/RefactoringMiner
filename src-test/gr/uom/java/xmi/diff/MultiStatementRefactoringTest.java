package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class MultiStatementRefactoringTest extends MockMultiStatementRefactoringDependencies {
    protected Set<UMLAttribute> attributesBefore = new ArraySet(Collections.singletonList(attributeBefore));
    protected Set<UMLAttribute> attributesAfter = new ArraySet(Collections.singletonList(attributeAfter));
    protected Set<VariableDeclarationContainer> operationsBefore = new ArraySet(Collections.singletonList(operationBefore));
    protected Set<VariableDeclarationContainer> operationsAfter = new ArraySet(Collections.singletonList(operationAfter));
    private List<MultiStatementRefactoring> refactorings;

    @BeforeEach
    void setUp() {
        /* Create List of Refactorings */
        refactorings = new ArrayList<>();
        statementsBefore = new ArraySet<>(Collections.singletonList(statementBefore));
        statementsAfter = new ArraySet<>(Collections.singletonList(statementAfter));
        /* Create MoveCodeRefactoring and add to list
        * TODO: Uncomment after implementing MoveCodeRefactoring
        *  */
//        refactorings.add(new MoveCodeRefactoring(operationBefore, operationAfter, mapper));
        /* Create MergeConditionalRefactoring and add to list */
        refactorings.add(new MergeConditionalRefactoring(statementsBefore, statementAfter, operationBefore, operationAfter));
        /* Create ReplaceLoopWithPipelineRefactoring and add to list */
        refactorings.add(new ReplaceLoopWithPipelineRefactoring(statementsBefore, statementsAfter, operationBefore, operationAfter));
        /* Create SplitConditionalRefactoring and add to list */
        refactorings.add(new SplitConditionalRefactoring(statementBefore, statementsAfter, operationBefore, operationAfter));
        /* Create MergeCatchRefactoring and add to list */
        refactorings.add(new MergeCatchRefactoring(statementsBefore, statementAfter, operationBefore, operationAfter));
    }

    @Test
    void testGroupByRefactoredMember_And_Count() {
        Map<? super AbstractCodeFragment, Set<? super MultiStatementRefactoring>> map = this.mapMemberToRefactoring(refactorings);
        Assertions.assertEquals(2, map.size(), "Expected 2 members to be refactored");
        Assertions.assertEquals(0, map.getOrDefault(null, Collections.emptySet()).size(), "Null should never be a key");
        Assertions.assertEquals(4, map.get(statementBefore).size(), "Expected 4 refactorings to be applied to statements");
        Assertions.assertEquals(4, map.get(statementAfter).size(), "Expected 4 refactored operations");
    }
}
