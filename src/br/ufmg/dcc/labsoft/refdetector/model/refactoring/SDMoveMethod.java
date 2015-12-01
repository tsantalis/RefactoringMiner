package br.ufmg.dcc.labsoft.refdetector.model.refactoring;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;

public class SDMoveMethod extends SDRefactoring {

    private final SDMethod methodBefore;
    private final SDMethod methodAfter;
    
    protected SDMoveMethod(RefactoringType refType, SDMethod methodBefore, SDMethod methodAfter) {
        super(refType, methodAfter);
        this.methodBefore = methodBefore;
        this.methodAfter = methodAfter;
    }
    
    public SDMoveMethod(SDMethod methodBefore, SDMethod methodAfter) {
        this(RefactoringType.MOVE_OPERATION, methodBefore, methodAfter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(methodBefore.getVerboseSimpleName());
        sb.append(" from class ");
        sb.append(methodBefore.container().fullName());
        sb.append(" to ");
        sb.append(methodAfter.getVerboseSimpleName());
        sb.append(" from class ");
        sb.append(methodAfter.container().fullName());
        return sb.toString();
    }
}
