package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.Set;

public interface ModelLevelRefactoring {
    String getPackageBefore();
    Set<UMLAbstractClass> getInvolvedClassesBefore();
    String getPackageAfter();
    Set<UMLAbstractClass> getInvolvedClassesAfter();
}
