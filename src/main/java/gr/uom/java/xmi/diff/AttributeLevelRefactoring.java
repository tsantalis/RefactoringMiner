package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;

public interface AttributeLevelRefactoring {
    UMLAttribute getAttributeBefore();

    UMLAttribute getAttributeAfter();
}
