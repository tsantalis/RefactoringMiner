package gr.uom.java.xmi.diff;

import java.util.Set;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public interface ReferenceBasedRefactoring {
	Set<AbstractCodeMapping> getReferences();
}
