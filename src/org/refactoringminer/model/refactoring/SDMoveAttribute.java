package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDAttribute;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDMoveAttribute extends SDRefactoring {

    protected final SDAttribute attributeBefore;
    protected final SDAttribute attributeAfter;
    
    protected SDMoveAttribute(RefactoringType refType, SDAttribute attributeBefore, SDAttribute attributeAfter) {
        super(refType, attributeAfter);
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }
    
    public SDMoveAttribute(SDAttribute attributeBefore, SDAttribute attributeAfter) {
        this(RefactoringType.MOVE_ATTRIBUTE, attributeBefore, attributeAfter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(attributeAfter.getVerboseSimpleName());
        sb.append(" from class ");
        sb.append(attributeBefore.container().fullName());
        sb.append(" to class ");
        sb.append(attributeAfter.container().fullName());
        return sb.toString();
    }
}
