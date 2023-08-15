package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

import java.util.Set;

public interface MultiStatementRefactoring {
    Set<AbstractCodeFragment> getStatementsBefore();

    Set<AbstractCodeFragment> getStatementsAfter();
}
