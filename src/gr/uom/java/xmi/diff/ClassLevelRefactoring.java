package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.Collections;
import java.util.List;

public interface ClassLevelRefactoring extends SingleClassRefactoring, MultiClassRefactoring {

    @Override
    default List<UMLAbstractClass> getClassesBefore() {
        return Collections.singletonList(getClassBefore());
    }

    @Override
    default List<UMLAbstractClass> getClassesAfter() {
        return Collections.singletonList(getClassAfter());
    }
}
