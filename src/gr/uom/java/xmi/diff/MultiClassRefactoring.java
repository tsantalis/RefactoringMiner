package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.List;

public interface MultiClassRefactoring {
    List<? extends UMLAbstractClass> getClassesBefore();

    List<? extends UMLAbstractClass> getClassesAfter();
}
