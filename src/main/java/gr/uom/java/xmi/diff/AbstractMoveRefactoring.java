package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.MoveCodeRefactoring.Type;

public abstract class AbstractMoveRefactoring implements Refactoring {
	public abstract AnnotationProvider getProviderBefore();
	public abstract AnnotationProvider getProviderAfter();

	private static String codeElementDescription(AnnotationProvider provider) {
		if (provider instanceof UMLOperation op)
			return op.toQualifiedString();
		else if (provider instanceof UMLAttribute attr)
			return attr.toQualifiedString();
		else if (provider instanceof UMLAbstractClass clazz)
			return clazz.getName();
		return provider.toString();
	}

	private static boolean addClassName(AnnotationProvider provider, RefactoringType refactoringType) {
		return !(provider instanceof UMLAbstractClass) && !refactoringType.equals(RefactoringType.MOVE_RENAME_ATTRIBUTE) &&
				!refactoringType.equals(RefactoringType.MOVE_CODE) && !refactoringType.equals(RefactoringType.EXTRACT_FIXTURE);
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
		if(getRefactoringType().equals(RefactoringType.MOVE_CODE)) {
			sb.append("from ");
		}
		sb.append(decorator.CODE_OPEN).append(codeElementDescription(getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE) ? getProviderAfter() : getProviderBefore())).append(decorator.CODE_CLOSE);
		if(addClassName(getProviderBefore(), getRefactoringType())) {
			sb.append(" from class ");
			sb.append(decorator.LINK_OPEN).append(getProviderBefore().getClassName()).append(decorator.LINK_CLOSE);
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_CODE) && ((MoveCodeRefactoring)this).getMoveType().equals(Type.MOVE_BETWEEN_FILES)) {
			sb.append(" in class ");
			sb.append(decorator.LINK_OPEN).append(getProviderBefore().getClassName()).append(decorator.LINK_CLOSE);
		}
		appendTextBetweenMovedElements(sb);
		sb.append(decorator.CODE_OPEN).append(codeElementDescription(getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE) ? getProviderBefore() : getProviderAfter())).append(decorator.CODE_CLOSE);
		if(addClassName(getProviderAfter(), getRefactoringType())) {
			sb.append(" from class ");
			sb.append(decorator.LINK_OPEN).append(getProviderAfter().getClassName()).append(decorator.LINK_CLOSE);
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_CODE) || getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE)) {
			sb.append(" in class ");
			sb.append(decorator.LINK_OPEN).append(getProviderAfter().getClassName()).append(decorator.LINK_CLOSE);
		}
		if(getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE)) {
			sb.append(" and moved from class ");
			sb.append(decorator.LINK_OPEN).append(getProviderBefore().getClassName()).append(decorator.LINK_CLOSE);
			sb.append(" to class ");
			sb.append(decorator.LINK_OPEN).append(getProviderAfter().getClassName()).append(decorator.LINK_CLOSE);
		}
		return sb.toString();
	}

	private void appendTextBetweenMovedElements(StringBuilder sb) {
		if(getRefactoringType().equals(RefactoringType.MOVE_CLASS))
			sb.append(" moved to ");
		else if(getName().startsWith("Replace"))
			sb.append(" with ");
		else if(getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE))
			sb.append(" renamed to ");
		else if(getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE))
			sb.append(" extracted from ");
		else
			sb.append(" to ");
	}
}
