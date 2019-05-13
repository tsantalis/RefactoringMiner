package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveAndRenameClassRefactoring implements Refactoring {

	private UMLClass originalClass;
	private UMLClass renamedClass;
	
	public MoveAndRenameClassRefactoring(UMLClass originalClass,  UMLClass renamedClass) {
		this.originalClass = originalClass;
		this.renamedClass = renamedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass.getName());
		sb.append(" moved and renamed to ");
		sb.append(renamedClass.getName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getRenamedClassName() {
		return renamedClass.getName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getRenamedClass() {
		return renamedClass;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getOriginalClass().getName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getRenamedClass().getName());
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalClass.codeRange()
				.setDescription("original type declaration")
				.setCodeElement(originalClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedClass.codeRange()
				.setDescription("moved and renamed type declaration")
				.setCodeElement(renamedClass.getName()));
		return ranges;
	}
}
