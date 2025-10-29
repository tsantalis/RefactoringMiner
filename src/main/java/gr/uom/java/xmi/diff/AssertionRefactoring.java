package gr.uom.java.xmi.diff;

import java.util.Set;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public interface AssertionRefactoring {
	Set<AbstractCodeMapping> getMappings();
	AbstractCall getCall();
}
