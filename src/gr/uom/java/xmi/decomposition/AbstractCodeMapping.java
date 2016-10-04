package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.Set;

import gr.uom.java.xmi.UMLOperation;

public abstract class AbstractCodeMapping {

	private AbstractCodeFragment fragment1;
	private AbstractCodeFragment fragment2;
	private UMLOperation operation1;
	private UMLOperation operation2;
	private Set<Replacement> replacements;
	
	public AbstractCodeMapping(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2,
			UMLOperation operation1, UMLOperation operation2) {
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public AbstractCodeFragment getFragment1() {
		return fragment1;
	}

	public AbstractCodeFragment getFragment2() {
		return fragment2;
	}

	public boolean isExact() {
		return fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString()) && !isKeyword();
	}

	public boolean isInExact() {
		return !fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString());
	}

	private boolean isKeyword() {
		return fragment1.getString().startsWith("return;") ||
				fragment1.getString().startsWith("break;") ||
				fragment1.getString().startsWith("continue;") ||
				fragment1.getString().startsWith("return null;");
	}

	public void addReplacement(Replacement replacement) {
		this.replacements.add(replacement);
	}

	public void addReplacements(Set<Replacement> replacements) {
		this.replacements.addAll(replacements);
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public String toString() {
		return fragment1.toString() + fragment2.toString();
	}
}
