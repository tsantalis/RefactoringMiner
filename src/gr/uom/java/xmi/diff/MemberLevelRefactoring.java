package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

import java.util.Collections;
import java.util.List;

public interface MemberLevelRefactoring {
    VariableDeclarationContainer getMemberBefore();

    VariableDeclarationContainer getMemberAfter();

    default List<? super VariableDeclarationContainer> getMembersBefore() {
        return Collections.singletonList(getMemberBefore());
    }

    default List<? super VariableDeclarationContainer> getMembersAfter() {
        return Collections.singletonList(getMemberAfter());
    }
}
