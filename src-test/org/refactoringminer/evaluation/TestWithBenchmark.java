package org.refactoringminer.evaluation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.rm2.analysis.RefactoringDetectorConfigImpl;
import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import org.refactoringminer.utils.ResultComparator;

public class TestWithBenchmark {

    public static void main(String[] args) {
        EnumSet<RefactoringType> allRefTypes = EnumSet.allOf(RefactoringType.class);
        BenchmarkDataset oracle = new BenchmarkDataset();
        ResultComparator rc1 = new ResultComparator();
        rc1.expect(oracle.all());

        for (GitHistoryRefactoringMiner rm : generateRmConfigurations()) {
            rc1.compareWith(rm.getConfigId(), ResultComparator.collectRmResult(rm, oracle.all()));
        }

        rc1.printSummary(System.out, false, allRefTypes);
        rc1.printDetails(System.out, false, allRefTypes);
    }

    public static List<GitHistoryRefactoringMiner> generateRmConfigurations() {
        List<GitHistoryRefactoringMiner> list = new ArrayList<>();
        list.add(new GitHistoryRefactoringMiner2());
        
//        RefactoringDetectorConfigImpl config = new RefactoringDetectorConfigImpl();
//        config.setId("rm2-idf");
//        config.setCodeSimilarityStrategy(CodeSimilarityStrategy.TFIDF);
//        
//        list.add(new GitHistoryRefactoringMiner2(config));
        return list;
    }

}
