package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;

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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(codeElementDescription(getProviderBefore()));
		sb.append(" from class ");
		sb.append(getProviderBefore().getClassName());
		sb.append(" to ");
		sb.append(codeElementDescription(getProviderAfter()));
		sb.append(" from class ");
		sb.append(getProviderAfter().getClassName());
		return sb.toString();
	}
}
