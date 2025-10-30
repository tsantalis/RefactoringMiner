package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;

public interface ModifyAnnotationRefactoring extends AnnotationRefactoring {
    UMLAnnotation getAnnotationBefore();
    UMLAnnotation getAnnotationAfter();
    default UMLAnnotation getAnnotation() {return getAnnotationAfter();}
}
