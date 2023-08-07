package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.Collections;
import java.util.List;

public interface ClassLevelRefactoring {
    UMLAbstractClass getClassBefore();

    UMLAbstractClass getClassAfter();

    default List<UMLAbstractClass> getClassesBefore() {
        return Collections.singletonList(getClassBefore());
    }

    default List<UMLAbstractClass> getClassesAfter() {
        return Collections.singletonList(getClassAfter());
    }
}
