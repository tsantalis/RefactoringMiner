package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDMethod;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDPushDownMethod extends SDMoveMethod {

    public SDPushDownMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.PUSH_DOWN_OPERATION, methodBefore, methodAfter);
    }

}
