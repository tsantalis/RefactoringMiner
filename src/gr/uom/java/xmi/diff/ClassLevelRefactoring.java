package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.VariableDeclarationContainer;

import java.util.Collections;
import java.util.List;

public interface ClassLevelRefactoring<B extends VariableDeclarationContainer, A extends VariableDeclarationContainer> {
    public interface Default<T extends VariableDeclarationContainer> extends ClassLevelRefactoring<T, T> {}
    public interface Generic extends ClassLevelRefactoring.Default<VariableDeclarationContainer> {}
    B getMemberBefore();
    A getMemberAfter();
    default List<? super B> getMembersBefore() {
        return Collections.singletonList(getMemberBefore());
    }
    default List<? super A> getMembersAfter() {
        return Collections.singletonList(getMemberAfter());
    }
}
