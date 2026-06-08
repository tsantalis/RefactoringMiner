package gr.uom.java.xmi.diff;

import java.util.Optional;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.PrimaryConstructor;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
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
		else if (provider instanceof PrimaryConstructor)
			return "primary constructor";
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
			if(getRefactoringType().equals(RefactoringType.RENAME_METHOD))
				sb.append(" renamed to ");
			else if(getRefactoringType().equals(RefactoringType.MERGE_CLASS))
				sb.append(" merged to ");
			else if(getRefactoringType().equals(RefactoringType.SPLIT_CLASS))
				sb.append(" split to ");
			else if(getRefactoringType().equals(RefactoringType.REPLACE_ANONYMOUS_WITH_LAMBDA) ||
					getRefactoringType().equals(RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS) ||
					getRefactoringType().equals(RefactoringType.REPLACE_LOOP_WITH_PIPELINE) ||
					getRefactoringType().equals(RefactoringType.REPLACE_PIPELINE_WITH_LOOP) ||
					getRefactoringType().equals(RefactoringType.REPLACE_GENERIC_WITH_DIAMOND))
				sb.append(" with ");
			else if(getRefactoringType().equals(RefactoringType.EXTRACT_CLASS) ||
					getRefactoringType().equals(RefactoringType.EXTRACT_SUBCLASS) ||
					getRefactoringType().equals(RefactoringType.EXTRACT_SUPERCLASS)) {
				if(getTemplateParameterAfter().startsWith("[") && getTemplateParameterAfter().endsWith("]"))
					sb.append(" from classes ");
				else
					sb.append(" from class ");
			}
			else if(getRefactoringType().equals(RefactoringType.EXTRACT_INTERFACE))
				sb.append(" from classes ");
			else
				sb.append(" to ");
		}
		sb.append(getTemplateParameterAfter());
		boolean removeOrInline = getName().startsWith("Remove") || getName().startsWith("Inline");
		AnnotationProvider provider = removeOrInline ? getProviderBefore() : getProviderAfter();
		String codeElementType = codeElementType(provider);
		if(addCodeElementDescription()) {
			String finalCodeElementType = getRefactoringType().equals(RefactoringType.CHANGE_TYPE_DECLARATION_KIND) ? "type" : codeElementType;
			sb.append(" in ").append(finalCodeElementType).append(" ");
			sb.append(codeElementDescription(provider));
		}
		String className = null;
		if (provider instanceof VariableDeclaration || provider instanceof UMLType) {
			MethodLevelRefactoring methodLevelRef = (MethodLevelRefactoring) this;
			VariableDeclarationContainer container = removeOrInline
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
			if(getRefactoringType().equals(RefactoringType.CHANGE_ATTRIBUTE_TYPE) ||
					getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE) ||
					getRefactoringType().equals(RefactoringType.SPLIT_ATTRIBUTE) ||
					getRefactoringType().equals(RefactoringType.MERGE_ATTRIBUTE) ||
					getRefactoringType().equals(RefactoringType.SPLIT_OPERATION) ||
					getRefactoringType().equals(RefactoringType.MERGE_OPERATION) ||
					getRefactoringType().equals(RefactoringType.RENAME_METHOD))
				sb.append(" in class ");
			else
				sb.append(" from class ");
			sb.append(className);
		}
		return sb.toString();
	}
}