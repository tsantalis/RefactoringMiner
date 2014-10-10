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

	@Override
	public String getName() {
		return "Move Class";
	}
}
