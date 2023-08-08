package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

import java.util.List;

public interface MultiMemberRefactoring {
    List<? super VariableDeclarationContainer> getMembersBefore();

    List<? super VariableDeclarationContainer> getMembersAfter();
}
