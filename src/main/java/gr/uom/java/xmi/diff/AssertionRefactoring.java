package gr.uom.java.xmi.diff;

import java.util.Optional;
import java.util.Set;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public abstract class AssertionRefactoring extends AbstractRefactoring {
	public abstract Set<AbstractCodeMapping> getMappings();
	public abstract AbstractCall getCall();

	@Override
	public Optional<String> getTemplateParameterBefore() {
		return Optional.empty();
	}

	@Override
	public String getTemplateParameterAfter() {
		String string = getCall().actualString();
		return string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string;
	}
}
