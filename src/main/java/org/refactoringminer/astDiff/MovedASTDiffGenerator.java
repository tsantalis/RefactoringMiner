package org.refactoringminer.astDiff;

import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;

public abstract class MovedASTDiffGenerator {
    protected final ProjectASTDiff projectASTDiff;
    protected final UMLModelDiff modelDiff;
    public MovedASTDiffGenerator(UMLModelDiff modelDiff, ProjectASTDiff projectASTDiff) {
        this.modelDiff = modelDiff;
        this.projectASTDiff = projectASTDiff;
    }
    public abstract void generate();
}
