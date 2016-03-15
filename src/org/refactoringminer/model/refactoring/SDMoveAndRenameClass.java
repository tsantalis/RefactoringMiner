package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDType;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDMoveAndRenameClass extends SDMoveClass {

    public SDMoveAndRenameClass(SDType typeBefore, SDType typeAfter) {
        super(RefactoringType.MOVE_RENAME_CLASS, typeBefore, typeAfter);
    }

}
