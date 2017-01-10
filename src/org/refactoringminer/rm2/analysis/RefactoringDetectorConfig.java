package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;

public class RefactoringDetectorConfig {

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

    private CodeSimilarityStrategy codeSimilarityStrategy = CodeSimilarityStrategy.TFIDF;

    public double getMoveTypeThreshold() {
        return moveTypeThreshold;
    }

    public void setMoveTypeThreshold(double moveTypeThreshold) {
        this.moveTypeThreshold = moveTypeThreshold;
    }

    public double getRenameTypeThreshold() {
        return renameTypeThreshold;
    }

    public void setRenameTypeThreshold(double renameTypeThreshold) {
        this.renameTypeThreshold = renameTypeThreshold;
    }

    public double getMoveAndRenameTypeThreshold() {
        return moveAndRenameTypeThreshold;
    }

    public void setMoveAndRenameTypeThreshold(double moveAndRenameTypeThreshold) {
        this.moveAndRenameTypeThreshold = moveAndRenameTypeThreshold;
    }

    public double getExtractSupertypeThreshold() {
        return extractSupertypeThreshold;
    }

    public void setExtractSupertypeThreshold(double extractSupertypeThreshold) {
        this.extractSupertypeThreshold = extractSupertypeThreshold;
    }

    public double getRenameMethodThreshold() {
        return renameMethodThreshold;
    }

    public void setRenameMethodThreshold(double renameMethodThreshold) {
        this.renameMethodThreshold = renameMethodThreshold;
    }

    public double getMoveMethodThreshold() {
        return moveMethodThreshold;
    }

    public void setMoveMethodThreshold(double moveMethodThreshold) {
        this.moveMethodThreshold = moveMethodThreshold;
    }

    public double getPullUpMethodThreshold() {
        return pullUpMethodThreshold;
    }

    public void setPullUpMethodThreshold(double pullUpMethodThreshold) {
        this.pullUpMethodThreshold = pullUpMethodThreshold;
    }

    public double getPushDownMethodThreshold() {
        return pushDownMethodThreshold;
    }

    public void setPushDownMethodThreshold(double pushDownMethodThreshold) {
        this.pushDownMethodThreshold = pushDownMethodThreshold;
    }

    public double getExtractMethodThreshold() {
        return extractMethodThreshold;
    }

    public void setExtractMethodThreshold(double extractMethodThreshold) {
        this.extractMethodThreshold = extractMethodThreshold;
    }

    public double getInlineMethodThreshold() {
        return inlineMethodThreshold;
    }

    public void setInlineMethodThreshold(double inlineMethodThreshold) {
        this.inlineMethodThreshold = inlineMethodThreshold;
    }

    public double getMoveAttributeThreshold() {
        return moveAttributeThreshold;
    }

    public void setMoveAttributeThreshold(double moveAttributeThreshold) {
        this.moveAttributeThreshold = moveAttributeThreshold;
    }

    public double getPullUpAttributeThreshold() {
        return pullUpAttributeThreshold;
    }

    public void setPullUpAttributeThreshold(double pullUpAttributeThreshold) {
        this.pullUpAttributeThreshold = pullUpAttributeThreshold;
    }

    public double getPushDownAttributeThreshold() {
        return pushDownAttributeThreshold;
    }

    public void setPushDownAttributeThreshold(double pushDownAttributeThreshold) {
        this.pushDownAttributeThreshold = pushDownAttributeThreshold;
    }

    public CodeSimilarityStrategy getCodeSimilarityStrategy() {
        return codeSimilarityStrategy;
    }

    public void setCodeSimilarityStrategy(CodeSimilarityStrategy codeSimilarityStrategy) {
        this.codeSimilarityStrategy = codeSimilarityStrategy;
    }

}