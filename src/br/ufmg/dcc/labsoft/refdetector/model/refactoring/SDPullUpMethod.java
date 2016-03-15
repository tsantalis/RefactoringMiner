package br.ufmg.dcc.labsoft.refdetector.model.refactoring;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;

public class SDPullUpMethod extends SDMoveMethod {

    public SDPullUpMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.PULL_UP_OPERATION, methodBefore, methodAfter);
    }
}
