package org.refactoringminer.evaluation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.rm2.analysis.RefactoringDetectorConfigImpl;
import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import org.refactoringminer.rm2.model.RelationshipType;
import org.refactoringminer.utils.ResultComparator;
import org.refactoringminer.utils.ResultComparator.CompareResult;

public class TestWithBenchmark {

    public static void main(String[] args) {
        RefactoringDetectorConfigImpl config = new RefactoringDetectorConfigImpl();
        BenchmarkDataset oracle = new BenchmarkDataset();
        
        
//        config.setThreshold(RelationshipType.MOVE_TYPE, 0.6);
//        config.setThreshold(RelationshipType.RENAME_TYPE, 0.5);
        config = calibrate(oracle, config, RelationshipType.MOVE_TYPE, RefactoringType.MOVE_CLASS);
        config = calibrate(oracle, config, RelationshipType.RENAME_TYPE, RefactoringType.RENAME_CLASS);
        config = calibrate(oracle, config, RelationshipType.EXTRACT_SUPERTYPE, RefactoringType.EXTRACT_SUPERCLASS, RefactoringType.EXTRACT_INTERFACE);
        
        config.setId("rm2-idf-final");
        System.out.println(config.toString());
    }

    private static RefactoringDetectorConfigImpl calibrate(BenchmarkDataset oracle, RefactoringDetectorConfigImpl baseConfig, RelationshipType relType, RefactoringType refType, RefactoringType ... refTypes) {
        ResultComparator rc1 = new ResultComparator();
        rc1.expect(oracle.all());
        EnumSet<RefactoringType> refTypeSet = EnumSet.of(refType, refTypes);
        
        List<RefactoringDetectorConfigImpl> configurations = generateRmConfigurations(baseConfig, relType);
        double maxF1 = 0.0;
        RefactoringDetectorConfigImpl maxConfig = configurations.get(0);
        
        for (RefactoringDetectorConfigImpl config : configurations) {
            rc1.compareWith(config.getId(), ResultComparator.collectRmResult(new GitHistoryRefactoringMiner2(config), oracle.all()));
            CompareResult result = rc1.getCompareResult(config.getId(), false, refTypeSet);
            double f1 = result.getF1();
            if (f1 >= maxF1) {
                maxF1 = f1;
                maxConfig = config;
            }
        }
        rc1.printSummary(System.out, false, refTypeSet);
        rc1.printDetails(System.out, false, refTypeSet);
        return maxConfig;
    }

    public static List<RefactoringDetectorConfigImpl> generateRmConfigurations(RefactoringDetectorConfigImpl baseConfig, RelationshipType relationshipType) {
        List<RefactoringDetectorConfigImpl> list = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            double t = 0.1 * i;
            RefactoringDetectorConfigImpl config = baseConfig.clone();
            config.setId("rm2-idf-" + relationshipType + "-" + i);
            config.setThreshold(relationshipType, t);
            config.setCodeSimilarityStrategy(CodeSimilarityStrategy.TFIDF);
            list.add(config);
        }
        return list;
    }

}
