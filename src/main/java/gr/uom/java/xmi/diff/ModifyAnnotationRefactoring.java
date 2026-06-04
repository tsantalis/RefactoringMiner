package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAnnotation;

public abstract class ModifyAnnotationRefactoring extends AnnotationRefactoring {
	public abstract UMLAnnotation getAnnotationBefore();
	public abstract UMLAnnotation getAnnotationAfter();
	public UMLAnnotation getAnnotation() {return getAnnotationAfter();}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getAnnotationBefore());
		sb.append(" to ");
		sb.append(getAnnotationAfter());
		AnnotationProvider provider = getProviderAfter();
		String codeElementType = codeElementType(provider);
		sb.append(" in ").append(codeElementType).append(" ");
		sb.append(codeElementDescription(provider));
		if(!codeElementType.equals("class")) {
			sb.append(" from class ");
			sb.append(provider.getClassName());
		}
		return sb.toString();
	}
}
