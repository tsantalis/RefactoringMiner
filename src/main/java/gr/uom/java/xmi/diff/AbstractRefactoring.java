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
		return toString(Refactoring.Decorator.PLAIN);
	}

	public String toHTMLString() {
		return toString(Refactoring.Decorator.HTML);
	}

	public String toMarkupString() {
		return toString(Refactoring.Decorator.MARKUP);
	}

	private String toString(Refactoring.Decorator decorator) {
		StringBuilder sb = new StringBuilder();
		sb.append(decorator.BOLD_OPEN).append(getName()).append(decorator.BOLD_CLOSE).append("\t");
		if(getTemplateParameterBefore().isPresent()) {
			String templateParameterBefore = getTemplateParameterBefore().get();
			if(decorator.equals(Decorator.HTML)) {
				templateParameterBefore = escapeHTML(templateParameterBefore);
			}
			sb.append(decorator.CODE_OPEN).append(templateParameterBefore).append(decorator.CODE_CLOSE);
			appendTextForTemplateParameterBefore(sb);
		}
		String templateParameterAfter = getTemplateParameterAfter();
		if(decorator.equals(Decorator.HTML)) {
			templateParameterAfter = escapeHTML(templateParameterAfter);
		}
		sb.append(decorator.CODE_OPEN).append(templateParameterAfter).append(decorator.CODE_CLOSE);
		boolean removeOrInline = getName().startsWith("Remove") || getName().startsWith("Inline") || getName().startsWith("Move And Inline");
		AnnotationProvider provider = removeOrInline ? getProviderBefore() : getProviderAfter();
		String codeElementType = codeElementType(provider);
		String providerDescription = codeElementDescription(provider);
		if(decorator.equals(Decorator.HTML)) {
			providerDescription = escapeHTML(providerDescription);
		}
		if(addCodeElementDescription()) {
			String finalCodeElementType = getRefactoringType().equals(RefactoringType.CHANGE_TYPE_DECLARATION_KIND) ? "type" : codeElementType;
			sb.append(" in ").append(finalCodeElementType).append(" ");
			sb.append(decorator.CODE_OPEN).append(providerDescription).append(decorator.CODE_CLOSE);
		}
		if(getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
			sb.append(" in class ");
			sb.append(decorator.LINK_OPEN).append(getProviderBefore().getClassName()).append(decorator.LINK_CLOSE);
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_ANNOTATION)) {
			sb.append(" from ");
			String providerBeforeDescription = codeElementDescription(getProviderBefore());
			if(decorator.equals(Decorator.HTML)) {
				providerBeforeDescription = escapeHTML(providerBeforeDescription);
			}
			sb.append(decorator.CODE_OPEN).append(providerBeforeDescription).append(decorator.CODE_CLOSE);
		}
		String className = null;
		if (provider instanceof VariableDeclaration || provider instanceof UMLType) {
			MethodLevelRefactoring methodLevelRef = (MethodLevelRefactoring) this;
			VariableDeclarationContainer container = removeOrInline
					? methodLevelRef.getOperationBefore()
					: methodLevelRef.getOperationAfter();
			String elementType = container.getElementType();
			sb.append(" in " + elementType + " ");
			String containerDescription = container.toQualifiedString();
			if(decorator.equals(Decorator.HTML)) {
				containerDescription = escapeHTML(containerDescription);
			}
			sb.append(decorator.CODE_OPEN).append(containerDescription).append(decorator.CODE_CLOSE);
			className = container.getClassName();
		} else if (provider != null) {
			className = provider.getClassName();
		}
		if (className != null && !codeElementType.equals("class") && !getRefactoringType().equals(RefactoringType.MOVE_ANNOTATION)) {
			appendTextForClassName(sb);
			sb.append(decorator.LINK_OPEN).append(className).append(decorator.LINK_CLOSE);
		}
		if(getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
			sb.append(" & inlined to ");
			sb.append(decorator.CODE_OPEN).append(providerDescription).append(decorator.CODE_CLOSE);
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_ANNOTATION)) {
			sb.append(" to ");
			sb.append(decorator.CODE_OPEN).append(providerDescription).append(decorator.CODE_CLOSE);
		}
		return sb.toString();
	}

	private static String escapeHTML(String codeElement) {
		return codeElement.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
	}

	private void appendTextForClassName(StringBuilder sb) {
		if(getRefactoringType().equals(RefactoringType.CHANGE_ATTRIBUTE_TYPE) ||
				getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE) ||
				getRefactoringType().equals(RefactoringType.SPLIT_ATTRIBUTE) ||
				getRefactoringType().equals(RefactoringType.MERGE_ATTRIBUTE) ||
				getRefactoringType().equals(RefactoringType.SPLIT_OPERATION) ||
				getRefactoringType().equals(RefactoringType.MERGE_OPERATION) ||
				getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) ||
				getRefactoringType().equals(RefactoringType.INLINE_OPERATION) ||
				getRefactoringType().equals(RefactoringType.PARAMETERIZE_TEST) ||
				getRefactoringType().equals(RefactoringType.RENAME_METHOD))
			sb.append(" in class ");
		else if(getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION))
			sb.append(" & moved to class ");
		else if(getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION))
			sb.append(" to class ");
		else
			sb.append(" from class ");
	}

	private void appendTextForTemplateParameterBefore(StringBuilder sb) {
		if(getRefactoringType().equals(RefactoringType.RENAME_METHOD) ||
				getRefactoringType().equals(RefactoringType.RENAME_CLASS))
			sb.append(" renamed to ");
		else if(getRefactoringType().equals(RefactoringType.MOVE_RENAME_CLASS))
			sb.append(" moved and renamed to ");
		else if(getRefactoringType().equals(RefactoringType.MERGE_CLASS))
			sb.append(" merged to ");
		else if(getRefactoringType().equals(RefactoringType.SPLIT_CLASS))
			sb.append(" split to ");
		else if(getRefactoringType().equals(RefactoringType.INLINE_OPERATION))
			sb.append(" inlined to ");
		else if(getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) ||
				getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION))
			sb.append(" extracted from ");
		else if(getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION))
			sb.append(" moved from class ");
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
}