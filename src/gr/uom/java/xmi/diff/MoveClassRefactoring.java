package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class MoveClassRefactoring implements Refactoring {
	private UMLClass originalClass;
	private UMLClass movedClass;
	
	public MoveClassRefactoring(UMLClass originalClass,  UMLClass movedClass) {
		this.originalClass = originalClass;
		this.movedClass = movedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass.getName());
		sb.append(" moved to ");
		sb.append(movedClass.getName());
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return originalClass.getName();
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getName(), movedClass.getName());
		if (separatorPos == -1) {
			return new RenamePattern(originalClass.getName(), movedClass.getName());
		}
		String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
		String movedPath = movedClass.getName().substring(0, movedClass.getName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath);
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getMovedClassName() {
		return movedClass.getName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getMovedClass() {
		return movedClass;
	}
}
