package gr.uom.java.xmi.diff;

import java.util.Set;

import gr.uom.java.xmi.UMLClass;

public class ExtractSuperclassRefactoring implements Refactoring {
	private UMLClass extractedClass;
	private Set<String> subclassSet;
	
	public ExtractSuperclassRefactoring(UMLClass extractedClass, Set<String> subclassSet) {
		this.extractedClass = extractedClass;
		this.subclassSet = subclassSet;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedClass);
		sb.append(" from classes ");
		sb.append(subclassSet);
		return sb.toString();
	}

	@Override
	public String getName() {
		if(extractedClass.isInterface())
			return "Extract Interface";
		else
			return "Extract Superclass";
	}
}
