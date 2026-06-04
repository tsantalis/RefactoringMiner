package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLOperation;

public interface ModifyAnnotationRefactoring extends AnnotationRefactoring {
	UMLAnnotation getAnnotationBefore();
	UMLAnnotation getAnnotationAfter();
	default UMLAnnotation getAnnotation() {return getAnnotationAfter();}

	default String toPlainString() {
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

	public static String codeElementDescription(AnnotationProvider provider) {
		if (provider instanceof UMLOperation op)
			return op.toQualifiedString();
		else if(provider instanceof UMLAbstractClass clazz)
			return clazz.getName();
		return provider.toString();
	}

	public static String codeElementType(AnnotationProvider provider) {
		if(provider instanceof UMLEnumConstant)
			return "enum constant";
		else if (provider instanceof UMLAbstractClass)
			return "class";
		else if (provider instanceof UMLOperation)
			return "method";
		return "attribute";
	}
}
