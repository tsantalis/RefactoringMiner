package br.ufmg.dcc.labsoft.refdetector.model.refactoring;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;

public class SDPushDownMethod extends SDMoveMethod {

    public SDPushDownMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.PUSH_DOWN_OPERATION, methodBefore, methodAfter);
    }

}
