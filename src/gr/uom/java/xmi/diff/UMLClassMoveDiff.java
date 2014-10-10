package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

public class UMLClassMoveDiff implements Comparable<UMLClassMoveDiff> {
	private UMLClass originalClass;
	private UMLClass movedClass;
	
	public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass) {
		this.originalClass = originalClass;
		this.movedClass = movedClass;
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getMovedClass() {
		return movedClass;
	}

	//return true if "classMoveDiff" represents the move of a class that is inner to this.originalClass
	public boolean isInnerClassMove(UMLClassMoveDiff classMoveDiff) {
		if(this.originalClass.isInnerClass(classMoveDiff.originalClass) && this.movedClass.isInnerClass(classMoveDiff.movedClass))
			return true;
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ");
		sb.append(originalClass.getName());
		sb.append(" was moved to ");
		sb.append(movedClass.getName());
		sb.append("\n");
		return sb.toString();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
		
		if(o instanceof UMLClassMoveDiff) {
			UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff)o;
			return this.originalClass.equals(classMoveDiff.originalClass) && this.movedClass.equals(classMoveDiff.movedClass);
		}
		return false;
	}

	public int compareTo(UMLClassMoveDiff other) {
		return this.originalClass.getName().compareTo(other.originalClass.getName());
	}
}
