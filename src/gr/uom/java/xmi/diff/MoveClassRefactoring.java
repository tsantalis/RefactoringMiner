package gr.uom.java.xmi.diff;

public class MoveClassRefactoring implements Refactoring {
	private String originalClassName;
	private String movedClassName;
	
	public MoveClassRefactoring(String originalClassName,  String movedClassName) {
		this.originalClassName = originalClassName;
		this.movedClassName = movedClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClassName);
		sb.append(" moved to ");
		sb.append(movedClassName);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CLASS;
	}

	public String getOriginalClassName() {
		return originalClassName;
	}

	public String getMovedClassName() {
		return movedClassName;
	}
	
}
