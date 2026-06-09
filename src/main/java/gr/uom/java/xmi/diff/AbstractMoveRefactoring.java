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
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		if(getRefactoringType().equals(RefactoringType.MOVE_CODE)) {
			sb.append("from ");
		}
		sb.append(codeElementDescription(getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE) ? getProviderAfter() : getProviderBefore()));
		if(addClassName(getProviderBefore(), getRefactoringType())) {
			sb.append(" from class ");
			sb.append(getProviderBefore().getClassName());
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_CODE) && ((MoveCodeRefactoring)this).getMoveType().equals(Type.MOVE_BETWEEN_FILES)) {
			sb.append(" in class ");
			sb.append(getProviderBefore().getClassName());
		}
		appendTextBetweenMovedElements(sb);
		sb.append(codeElementDescription(getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE) ? getProviderBefore() : getProviderAfter()));
		if(addClassName(getProviderAfter(), getRefactoringType())) {
			sb.append(" from class ");
			sb.append(getProviderAfter().getClassName());
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_CODE) || getRefactoringType().equals(RefactoringType.EXTRACT_FIXTURE)) {
			sb.append(" in class ");
			sb.append(getProviderAfter().getClassName());
		}
		if(getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE)) {
			sb.append(" and moved from class ");
			sb.append(getProviderBefore().getClassName());
			sb.append(" to class ");
			sb.append(getProviderAfter().getClassName());
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
