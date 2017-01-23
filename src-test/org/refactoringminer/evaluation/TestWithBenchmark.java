package org.refactoringminer.evaluation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.rm2.analysis.RefactoringDetectorConfigImpl;
import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import org.refactoringminer.rm2.model.RelationshipType;
import org.refactoringminer.utils.ResultComparator;

public class TestWithBenchmark {

    public static void main(String[] args) {
        RefactoringDetectorConfigImpl config = new RefactoringDetectorConfigImpl();
        config.setThreshold(RelationshipType.MOVE_TYPE, 0.6);
        config.setThreshold(RelationshipType.RENAME_TYPE, 0.5);
        
        BenchmarkDataset oracle = new BenchmarkDataset();
        
        callibrate(oracle, RelationshipType.EXTRACT_SUPERTYPE, RefactoringType.EXTRACT_SUPERCLASS, RefactoringType.EXTRACT_INTERFACE);
    }

    private static void callibrate(BenchmarkDataset oracle, RelationshipType relType, RefactoringType refType, RefactoringType ... refTypes) {
        ResultComparator rc1 = new ResultComparator();
        rc1.expect(oracle.all());
        
        for (GitHistoryRefactoringMiner rm : generateRmConfigurations(relType)) {
            rc1.compareWith(rm.getConfigId(), ResultComparator.collectRmResult(rm, oracle.all()));
        }
        rc1.printSummary(System.out, false, EnumSet.of(refType, refTypes));
        rc1.printDetails(System.out, false, EnumSet.of(refType, refTypes));
    }

    public static List<GitHistoryRefactoringMiner> generateRmConfigurations() {
        List<GitHistoryRefactoringMiner> list = new ArrayList<>();
        list.add(new GitHistoryRefactoringMiner2());
        
        RefactoringDetectorConfigImpl config = new RefactoringDetectorConfigImpl();
        config.setId("rm2-idf");
        config.setCodeSimilarityStrategy(CodeSimilarityStrategy.TFIDF);
        
        list.add(new GitHistoryRefactoringMiner2(config));
        return list;
    }

    public static List<GitHistoryRefactoringMiner> generateRmConfigurations(RelationshipType relationshipType) {
        List<GitHistoryRefactoringMiner> list = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            double t = 0.1 * i;
            RefactoringDetectorConfigImpl config = new RefactoringDetectorConfigImpl();
            config.setId("rm2-idf-" + relationshipType + "-" + i);
            config.setThreshold(relationshipType, t);
            config.setCodeSimilarityStrategy(CodeSimilarityStrategy.TFIDF);
            list.add(new GitHistoryRefactoringMiner2(config));
        }
        return list;
    }
}
