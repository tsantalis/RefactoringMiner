package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;

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
		return !(provider instanceof UMLAbstractClass) && !refactoringType.equals(RefactoringType.MOVE_RENAME_ATTRIBUTE);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(codeElementDescription(getProviderBefore()));
		if(addClassName(getProviderBefore(), getRefactoringType())) {
			sb.append(" from class ");
			sb.append(getProviderBefore().getClassName());
		}
		if(getRefactoringType().equals(RefactoringType.MOVE_CLASS))
			sb.append(" moved to ");
		else if(getName().startsWith("Replace"))
			sb.append(" with ");
		else if(getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE))
			sb.append(" renamed to ");
		else
			sb.append(" to ");
		sb.append(codeElementDescription(getProviderAfter()));
		if(addClassName(getProviderAfter(), getRefactoringType())) {
			sb.append(" from class ");
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
}
