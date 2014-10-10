package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;

public class ConvertAnonymousClassToTypeRefactoring implements Refactoring {
	private UMLAnonymousClass anonymousClass;
	private UMLClass addedClass;
	
	public ConvertAnonymousClassToTypeRefactoring(UMLAnonymousClass anonymousClass, UMLClass addedClass) {
		this.anonymousClass = anonymousClass;
		this.addedClass = addedClass;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(anonymousClass);
		sb.append(" was converted to ");
		sb.append(addedClass);
		return sb.toString();
	}

	public String getName() {
		return "Convert Anonymous Class to Type";
	}

}
