package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public abstract class ChangeAccessModifierRefactoring extends AbstractRefactoring {
	public abstract Visibility getOriginalAccessModifier();
	public abstract Visibility getChangedAccessModifier();
	public String getTemplateParameter() {return getChangedAccessModifier().toString();}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getOriginalAccessModifier());
		sb.append(" to ");
		sb.append(getChangedAccessModifier());
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
