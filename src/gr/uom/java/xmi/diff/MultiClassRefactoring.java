package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.List;

public interface MultiClassRefactoring {
    List<UMLAbstractClass> getClassesBefore();

    List<UMLAbstractClass> getClassesAfter();
}
