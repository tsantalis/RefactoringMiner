package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDType;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDRenameClass extends SDRefactoring {

    private final SDType typeBefore;
    private final SDType typeAfter;
    
    public SDRenameClass(SDType typeBefore, SDType typeAfter) {
        super(RefactoringType.RENAME_CLASS, typeAfter);
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(typeBefore.fullName());
        sb.append(" renamed to ");
        sb.append(typeAfter.fullName());
        return sb.toString();
    }
}
