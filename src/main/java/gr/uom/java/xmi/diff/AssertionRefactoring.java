package gr.uom.java.xmi.diff;

import java.util.Set;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public interface AssertionRefactoring extends Refactoring {
	Set<AbstractCodeMapping> getMappings();
	AbstractCall getCall();
}
