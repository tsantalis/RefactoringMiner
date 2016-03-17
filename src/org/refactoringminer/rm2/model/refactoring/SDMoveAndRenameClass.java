package org.refactoringminer.rm2.model.refactoring;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.SDType;

public class SDMoveAndRenameClass extends SDMoveClass {

    public SDMoveAndRenameClass(SDType typeBefore, SDType typeAfter) {
        super(RefactoringType.MOVE_RENAME_CLASS, typeBefore, typeAfter);
    }

}
