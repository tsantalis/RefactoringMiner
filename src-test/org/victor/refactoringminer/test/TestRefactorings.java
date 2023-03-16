package org.victor.refactoringminer.test;

import org.junit.Test;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator;
import org.refactoringminer.test.TestBuilder;

import java.util.List;
import java.util.Map;

public class TestRefactorings {
    @Test
    public void testAllRefactoringsWithinOneCommit_StringList() {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, "tmp1", RefactoringPopulator.Refactorings.All.getValue());
        test.expect("https://github.com/checkstyle/checkstyle.git", "master" , "0a1a4c6e94c9b3b73b21b323f14ae7b7337b1b44")
                .toHave(List.of("Rename Method private markFinalVariableCandidateAsAssignInIfBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedInIfBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
                        "Merge Method [private isInIfBlock(node DetailAST) : boolean, private isInElseBlock(node DetailAST) : boolean] to private isInSpecificCodeBlock(node DetailAST, blockType int) : boolean in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
                        "Rename Method private markFinalVariableCandidateAsAssignOutsideIfOrElseBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedOutsideIfOrElseBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
                        "Rename Method private markFinalVariableCandidateAsAssignInElseBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedInElseBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck"));
    }

    @Test
    public void testAllRefactoringsWithinOneCommit_RefactoringTypeMap() {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, "tmp1", RefactoringPopulator.Refactorings.All.getValue());
        test.expect("https://github.com/checkstyle/checkstyle.git", "master" , "0a1a4c6e94c9b3b73b21b323f14ae7b7337b1b44")
                .toHave(Map.of(
                        RefactoringType.MERGE_OPERATION, 1,
                        RefactoringType.RENAME_METHOD, 3));
    }
}
