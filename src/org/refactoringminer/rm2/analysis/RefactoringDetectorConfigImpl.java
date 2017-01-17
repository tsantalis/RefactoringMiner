package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;

public class RefactoringDetectorConfigImpl implements RefactoringDetectorConfig {

    private String id = "rm2-default";

    private double moveTypeThreshold = 0.4;
    private double renameTypeThreshold = 0.4;
    private double moveAndRenameTypeThreshold = 0.5;
    private double extractSupertypeThreshold = 0.5;

    private double renameMethodThreshold = 0.5;
    private double moveMethodThreshold = 0.5;
    private double pullUpMethodThreshold = 0.5;
    private double pushDownMethodThreshold = 0.5;
    private double extractMethodThreshold = 0.5;
    private double inlineMethodThreshold = 0.5;

    private double moveAttributeThreshold = 0.2;
    private double pullUpAttributeThreshold = 0.1;
    private double pushDownAttributeThreshold = 0.1;

    private CodeSimilarityStrategy codeSimilarityStrategy = CodeSimilarityStrategy.BIGRAM;

    public String getId() {
        return id;
    }

    public RefactoringDetectorConfig setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public double getMoveTypeThreshold() {
        return moveTypeThreshold;
    }

    public RefactoringDetectorConfig setMoveTypeThreshold(double moveTypeThreshold) {
        this.moveTypeThreshold = moveTypeThreshold;
        return this;
    }

    @Override
    public double getRenameTypeThreshold() {
        return renameTypeThreshold;
    }

    public RefactoringDetectorConfig setRenameTypeThreshold(double renameTypeThreshold) {
        this.renameTypeThreshold = renameTypeThreshold;
        return this;
    }

    @Override
    public double getMoveAndRenameTypeThreshold() {
        return moveAndRenameTypeThreshold;
    }

    public RefactoringDetectorConfig setMoveAndRenameTypeThreshold(double moveAndRenameTypeThreshold) {
        this.moveAndRenameTypeThreshold = moveAndRenameTypeThreshold;
        return this;
    }

    @Override
    public double getExtractSupertypeThreshold() {
        return extractSupertypeThreshold;
    }

    public RefactoringDetectorConfig setExtractSupertypeThreshold(double extractSupertypeThreshold) {
        this.extractSupertypeThreshold = extractSupertypeThreshold;
        return this;
    }

    @Override
    public double getRenameMethodThreshold() {
        return renameMethodThreshold;
    }

    public RefactoringDetectorConfig setRenameMethodThreshold(double renameMethodThreshold) {
        this.renameMethodThreshold = renameMethodThreshold;
        return this;
    }

    @Override
    public double getMoveMethodThreshold() {
        return moveMethodThreshold;
    }

    public RefactoringDetectorConfig setMoveMethodThreshold(double moveMethodThreshold) {
        this.moveMethodThreshold = moveMethodThreshold;
        return this;
    }

    @Override
    public double getPullUpMethodThreshold() {
        return pullUpMethodThreshold;
    }

    public RefactoringDetectorConfig setPullUpMethodThreshold(double pullUpMethodThreshold) {
        this.pullUpMethodThreshold = pullUpMethodThreshold;
        return this;
    }

    @Override
    public double getPushDownMethodThreshold() {
        return pushDownMethodThreshold;
    }

    public RefactoringDetectorConfig setPushDownMethodThreshold(double pushDownMethodThreshold) {
        this.pushDownMethodThreshold = pushDownMethodThreshold;
        return this;
    }

    @Override
    public double getExtractMethodThreshold() {
        return extractMethodThreshold;
    }

    public RefactoringDetectorConfig setExtractMethodThreshold(double extractMethodThreshold) {
        this.extractMethodThreshold = extractMethodThreshold;
        return this;
    }

    @Override
    public double getInlineMethodThreshold() {
        return inlineMethodThreshold;
    }

    public RefactoringDetectorConfig setInlineMethodThreshold(double inlineMethodThreshold) {
        this.inlineMethodThreshold = inlineMethodThreshold;
        return this;
    }

    @Override
    public double getMoveAttributeThreshold() {
        return moveAttributeThreshold;
    }

    public RefactoringDetectorConfig setMoveAttributeThreshold(double moveAttributeThreshold) {
        this.moveAttributeThreshold = moveAttributeThreshold;
        return this;
    }

    @Override
    public double getPullUpAttributeThreshold() {
        return pullUpAttributeThreshold;
    }

    public RefactoringDetectorConfig setPullUpAttributeThreshold(double pullUpAttributeThreshold) {
        this.pullUpAttributeThreshold = pullUpAttributeThreshold;
        return this;
    }

    @Override
    public double getPushDownAttributeThreshold() {
        return pushDownAttributeThreshold;
    }

    public RefactoringDetectorConfig setPushDownAttributeThreshold(double pushDownAttributeThreshold) {
        this.pushDownAttributeThreshold = pushDownAttributeThreshold;
        return this;
    }

    @Override
    public CodeSimilarityStrategy getCodeSimilarityStrategy() {
        return codeSimilarityStrategy;
    }

    public RefactoringDetectorConfig setCodeSimilarityStrategy(CodeSimilarityStrategy codeSimilarityStrategy) {
        this.codeSimilarityStrategy = codeSimilarityStrategy;
        return this;
    }

}