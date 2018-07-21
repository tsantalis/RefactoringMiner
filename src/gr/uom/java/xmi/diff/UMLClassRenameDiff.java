package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

public class UMLClassRenameDiff extends UMLClassBaseDiff implements Comparable<UMLClassRenameDiff> {
	
	public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass) {
		super(originalClass, renamedClass);
	}

	public UMLClass getRenamedClass() {
		return nextClass;
	}

	public boolean samePackage() {
		return originalClass.getPackageName().equals(nextClass.getPackageName());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ");
		sb.append(originalClass.getName());
		sb.append(" was renamed to ");
		sb.append(nextClass.getName());
		sb.append("\n");
		return sb.toString();
	}

	public int compareTo(UMLClassRenameDiff other) {
		return this.originalClass.getName().compareTo(other.originalClass.getName());
	}
}
