package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDMethod;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDPullUpMethod extends SDMoveMethod {

    public SDPullUpMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.PULL_UP_OPERATION, methodBefore, methodAfter);
    }
}
