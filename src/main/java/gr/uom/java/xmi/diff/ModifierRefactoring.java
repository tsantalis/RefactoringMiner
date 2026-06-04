package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLModifier;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public abstract class ModifierRefactoring implements Refactoring {
	public abstract UMLModifier getModifier();
	public abstract String getModifierAsString();
	public abstract AnnotationProvider getProviderBefore();
	public abstract AnnotationProvider getProviderAfter();

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getModifierAsString());
		AnnotationProvider provider = getName().startsWith("Remove") ? getProviderBefore() : getProviderAfter();
		String codeElementType = codeElementType(provider);
		sb.append(" in ").append(codeElementType).append(" ");
		sb.append(codeElementDescription(provider));
		String className = null;
		if(provider instanceof VariableDeclaration) {
			MethodLevelRefactoring methodLevelRef = (MethodLevelRefactoring)this;
			VariableDeclarationContainer container = getName().startsWith("Remove") ? methodLevelRef.getOperationBefore() : methodLevelRef.getOperationAfter();
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
		else if(provider instanceof VariableDeclaration vd)
			if(vd.isParameter())
				return "parameter";
			else
				return "variable";
		return "attribute";
	}
}
