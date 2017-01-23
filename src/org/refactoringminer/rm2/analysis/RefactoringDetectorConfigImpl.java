package org.refactoringminer.rm2.analysis;

import java.util.HashMap;
import java.util.Map;

import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import org.refactoringminer.rm2.model.RelationshipType;

public class RefactoringDetectorConfigImpl implements RefactoringDetectorConfig {

    private String id = "rm2-default";

    private double defaultThreshold = 0.5;
    
    private Map<RelationshipType, Double> thresholds = new HashMap<>();
    
    private CodeSimilarityStrategy codeSimilarityStrategy = CodeSimilarityStrategy.BIGRAM;

    public RefactoringDetectorConfigImpl() {
//        setThreshold(RelationshipType.MOVE_TYPE, 0.6);
//        setThreshold(RelationshipType.RENAME_TYPE, 0.4);
//        setThreshold(RelationshipType.MOVE_FIELD, 0.2);
//        setThreshold(RelationshipType.PULL_UP_FIELD, 0.1);
//        setThreshold(RelationshipType.PUSH_DOWN_FIELD, 0.1);
    }

    @Override
    public double getThreshold(RelationshipType relationshipType) {
        if (thresholds.containsKey(relationshipType)) {
            return thresholds.get(relationshipType);
        }
        return defaultThreshold;
    }

    public void setThreshold(RelationshipType relationshipType, double value) {
        thresholds.put(relationshipType, value);
    }

    public double getDefaultThreshold() {
        return defaultThreshold;
    }

    public void setDefaultThreshold(double defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public CodeSimilarityStrategy getCodeSimilarityStrategy() {
        return codeSimilarityStrategy;
    }

    public RefactoringDetectorConfig setCodeSimilarityStrategy(CodeSimilarityStrategy codeSimilarityStrategy) {
        this.codeSimilarityStrategy = codeSimilarityStrategy;
        return this;
    }

    @Override
    protected RefactoringDetectorConfigImpl clone() throws CloneNotSupportedException {
        RefactoringDetectorConfigImpl c = new RefactoringDetectorConfigImpl();
        c.id = id;
        c.defaultThreshold = defaultThreshold;
        c.codeSimilarityStrategy = codeSimilarityStrategy;
        c.thresholds = new HashMap<>(thresholds);
        return c;
    }

}