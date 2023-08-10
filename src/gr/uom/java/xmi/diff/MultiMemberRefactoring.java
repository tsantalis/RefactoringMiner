package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

import java.util.List;

public interface MultiMemberRefactoring {
    List<? extends VariableDeclarationContainer> getMembersBefore();

    List<? extends VariableDeclarationContainer> getMembersAfter();
}
