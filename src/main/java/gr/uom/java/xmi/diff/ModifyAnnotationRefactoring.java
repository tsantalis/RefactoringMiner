package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.UMLAnnotation;

public abstract class ModifyAnnotationRefactoring extends AnnotationRefactoring {
	public abstract UMLAnnotation getAnnotationBefore();
	public abstract UMLAnnotation getAnnotationAfter();
	public Optional<String> getTemplateParameterBefore() {return Optional.of(getAnnotationBefore().toString());}
	public UMLAnnotation getAnnotation() {return getAnnotationAfter();}
}
