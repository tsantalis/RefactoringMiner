package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

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
		String className = null;
		if(provider instanceof VariableDeclaration) {
			MethodLevelRefactoring methodLevelRef = (MethodLevelRefactoring)this;
			VariableDeclarationContainer container = methodLevelRef.getOperationAfter();
			String elementType = container.getElementType();
			sb.append(" in " + elementType + " ");
			sb.append(container.toQualifiedString());
			className = container.getClassName();
		}
		else {
			className = provider.getClassName();
		}
		if(!codeElementType.equals("class")) {
			sb.append(" from class ");
			sb.append(className);
		}
		return sb.toString();
	}
}
