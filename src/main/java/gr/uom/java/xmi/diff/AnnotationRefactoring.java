package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;

public abstract class AnnotationRefactoring extends AbstractRefactoring {
	public abstract UMLAnnotation getAnnotation();
	public String getTemplateParameter() {return getAnnotation().toString();}
}
