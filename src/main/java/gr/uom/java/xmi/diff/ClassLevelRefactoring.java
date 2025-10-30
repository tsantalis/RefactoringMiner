package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

public interface ClassLevelRefactoring {
    UMLAbstractClass getClassBefore();

    UMLAbstractClass getClassAfter();
}
