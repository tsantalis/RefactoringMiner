package gr.uom.java.xmi.diff;

public class RenameClassRefactoring implements Refactoring {
	private String originalClassName;
	private String renamedClassName;
	
	public RenameClassRefactoring(String originalClassName,  String renamedClassName) {
		this.originalClassName = originalClassName;
		this.renamedClassName = renamedClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClassName);
		sb.append(" renamed to ");
		sb.append(renamedClassName);
		return sb.toString();
	}

	@Override
	public String getName() {
		return "Rename Class";
	}
}
