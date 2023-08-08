package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

public interface SingleMemberRefactoring {
    VariableDeclarationContainer getMemberBefore();

    VariableDeclarationContainer getMemberAfter();
}
