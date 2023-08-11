package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

public interface SingleStatementRefactoring {
    AbstractCodeFragment getStatementBefore();
    AbstractCodeFragment getStatementAfter();
}
