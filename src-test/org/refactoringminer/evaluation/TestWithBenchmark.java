package org.refactoringminer.evaluation;

import java.util.EnumSet;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.utils.RefactoringSet;
import org.refactoringminer.utils.ResultComparator;

public class TestWithBenchmark {

    public static void main(String[] args) {
        EnumSet<RefactoringType> allRefTypes = EnumSet.allOf(RefactoringType.class);
//
        BenchmarkDataset oracle = new BenchmarkDataset();
        GitHistoryRefactoringMiner rm2 = new GitHistoryRefactoringMiner2();

        RefactoringSet[] rm2Results = ResultComparator.collectRmResult(rm2, oracle.all());

        ResultComparator rc1 = new ResultComparator();
        rc1.expect(oracle.all());
        rc1.compareWith("RM2", rm2Results);
        rc1.printSummary(System.out, false, allRefTypes);
        rc1.printDetails(System.out, false, allRefTypes);
    }

}
