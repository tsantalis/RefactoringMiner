package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.UMLAnnotation;

public abstract class AnnotationRefactoring extends AbstractRefactoring {
	public abstract UMLAnnotation getAnnotation();
	public Optional<String> getTemplateParameterBefore() {return Optional.empty();}
	public String getTemplateParameterAfter() {return getAnnotation().toString();}
}
