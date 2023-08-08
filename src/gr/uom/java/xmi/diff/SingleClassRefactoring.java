package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

public interface SingleClassRefactoring {
    UMLAbstractClass getClassBefore();

    UMLAbstractClass getClassAfter();
}
