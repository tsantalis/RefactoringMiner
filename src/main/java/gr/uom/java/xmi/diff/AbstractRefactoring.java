package gr.uom.java.xmi.diff;

import java.util.Optional;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.VariableDeclarationContainer;

public abstract class AbstractRefactoring implements Refactoring {
	public abstract AnnotationProvider getProviderBefore();
	public abstract AnnotationProvider getProviderAfter();
	public abstract Optional<String> getTemplateParameterBefore();
	public abstract String getTemplateParameterAfter();
	// this method should be overridden by subclasses to return false if code element description is not needed
	public boolean addCodeElementDescription() {return true;}

	protected static String codeElementType(AnnotationProvider provider) {
		if (provider instanceof UMLEnumConstant)
			return "enum constant";
		else if (provider instanceof UMLAbstractClass)
			return "class";
		else if (provider instanceof UMLOperation)
			return "method";
		else if (provider instanceof VariableDeclaration vd)
			if (vd.isParameter())
				return "parameter";
			else
				return "variable";
		return "attribute";
	}

	private static String codeElementDescription(AnnotationProvider provider) {
		if (provider instanceof UMLOperation op)
			return op.toQualifiedString();
		else if (provider instanceof UMLAbstractClass clazz)
			return clazz.getName();
		return provider.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		if(getTemplateParameterBefore().isPresent()) {
			sb.append(getTemplateParameterBefore().get());
			sb.append(" to ");
		}
		sb.append(getTemplateParameterAfter());
		AnnotationProvider provider = getName().startsWith("Remove") ? getProviderBefore() : getProviderAfter();
		String codeElementType = codeElementType(provider);
		if(addCodeElementDescription()) {
			sb.append(" in ").append(codeElementType).append(" ");
			sb.append(codeElementDescription(provider));
		}
		String className = null;
		if (provider instanceof VariableDeclaration) {
			MethodLevelRefactoring methodLevelRef = (MethodLevelRefactoring) this;
			VariableDeclarationContainer container = getName().startsWith("Remove")
					? methodLevelRef.getOperationBefore()
					: methodLevelRef.getOperationAfter();
			String elementType = container.getElementType();
			sb.append(" in " + elementType + " ");
			sb.append(container.toQualifiedString());
			className = container.getClassName();
		} else {
			className = provider.getClassName();
		}
		if (!codeElementType.equals("class")) {
			if(getRefactoringType().equals(RefactoringType.CHANGE_ATTRIBUTE_TYPE))
				sb.append(" in class ");
			else
				sb.append(" from class ");
			sb.append(className);
		}
		return sb.toString();
	}
}