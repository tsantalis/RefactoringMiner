package org.refactoringminer.rm2.model.refactoring;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.SDMethod;

public class SDRenameMethod extends SDRefactoring {

    private final SDMethod methodBefore;
    private final SDMethod methodAfter;
    
    public SDRenameMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.RENAME_METHOD, methodBefore, methodBefore, methodAfter);
        this.methodBefore = methodBefore;
        this.methodAfter = methodAfter;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(methodBefore.getVerboseSimpleName());
        sb.append(" renamed to ");
        sb.append(methodAfter.getVerboseSimpleName());
        sb.append(" in class ");
        sb.append(methodAfter.container().fullName());
        return sb.toString();
    }
}
