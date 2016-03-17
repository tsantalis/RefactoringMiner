package org.refactoringminer.rm2.model.refactoring;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.SDMethod;

public class SDPullUpMethod extends SDMoveMethod {

    public SDPullUpMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.PULL_UP_OPERATION, methodBefore, methodAfter);
    }
}
