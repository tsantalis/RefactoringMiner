package gr.uom.java.xmi.diff;

import java.util.Set;

public class UMLClassMergeDiff {
	private Set<UMLClassRenameDiff> classRenameDiffs;

	public UMLClassMergeDiff(Set<UMLClassRenameDiff> classRenameDiffs) {
		this.classRenameDiffs = classRenameDiffs;
	}

	public Set<UMLClassRenameDiff> getClassRenameDiffs() {
		return classRenameDiffs;
	}

	public boolean samePackage() {
		UMLClassRenameDiff renameDiff = classRenameDiffs.iterator().next();
		return renameDiff.samePackage();
	}
}
