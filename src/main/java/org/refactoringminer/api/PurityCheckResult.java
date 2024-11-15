package org.refactoringminer.api;

public class PurityCheckResult {
    private boolean isPure;

    String purityComment;
    String description;

    /*
    1: All statements have been mapped
    2: There is at least one non-mapped leaf
    3: There is at least one non-mapped node
 */
    int mappingState;


    public PurityCheckResult(boolean isPure, String description, String purityComment) {
        this.isPure = isPure;
        this.description = description;
        this.purityComment = purityComment;
    }

    public PurityCheckResult(boolean isPure, String description) {
        this.isPure = isPure;
        this.description = description;
    }

    PurityCheckResult(boolean isPure, String description, String purityComment, int mappingState) {
        this.isPure = isPure;
        this.description = description;
        this.purityComment = purityComment;
        this.mappingState = mappingState;
    }

    public boolean isPure() {
        return isPure;
    }

    public String getDescription() {
        return description;
    }

    public String getPurityComment() {
        return purityComment;
    }

    public int getMappingState() {
        return mappingState;
    }

    @Override
    public String toString() {
        return "PurityCheckResult{" +
                "isPure=" + isPure +
                ", description='" + description + '\'' +
                '}';
    }
}
