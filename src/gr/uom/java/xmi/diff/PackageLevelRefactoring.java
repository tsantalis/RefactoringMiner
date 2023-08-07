package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;

import java.util.Set;

public interface PackageLevelRefactoring {
    String getPackageBefore();
    Set<UMLAbstractClass> getInvolvedClassesBefore();
    String getPackageAfter();
    Set<UMLAbstractClass> getInvolvedClassesAfter();
}
