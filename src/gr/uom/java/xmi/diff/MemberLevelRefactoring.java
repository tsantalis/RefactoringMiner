package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

import java.util.Collections;
import java.util.List;

public interface MemberLevelRefactoring extends SingleMemberRefactoring, MultiMemberRefactoring {

    @Override
    default List<? extends VariableDeclarationContainer> getMembersBefore() {
        return Collections.singletonList(getMemberBefore());
    }

    @Override
    default List<? extends VariableDeclarationContainer> getMembersAfter() {
        return Collections.singletonList(getMemberAfter());
    }
}
