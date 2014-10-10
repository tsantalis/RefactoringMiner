package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

public class UMLClassRenameDiff implements Comparable<UMLClassRenameDiff> {
	private UMLClass originalClass;
	private UMLClass renamedClass;
	
	public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass) {
		this.originalClass = originalClass;
		this.renamedClass = renamedClass;
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getRenamedClass() {
		return renamedClass;
	}

	//return true if "classMoveDiff" represents the move of a class that is inner to this.originalClass
	public boolean isInnerClassMove(UMLClassMoveDiff classMoveDiff) {
		if(this.originalClass.isInnerClass(classMoveDiff.getOriginalClass()) && this.renamedClass.isInnerClass(classMoveDiff.getMovedClass()))
			return true;
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ");
		sb.append(originalClass.getName());
		sb.append(" was renamed to ");
		sb.append(renamedClass.getName());
		sb.append("\n");
		return sb.toString();
	}

	public int compareTo(UMLClassRenameDiff other) {
		return this.originalClass.getName().compareTo(other.originalClass.getName());
	}
}
