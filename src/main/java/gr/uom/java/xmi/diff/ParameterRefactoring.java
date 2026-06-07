package gr.uom.java.xmi.diff;

import java.util.Optional;

public abstract class ParameterRefactoring extends AbstractRefactoring {

	public Optional<String> getTemplateParameterBefore() {
		return Optional.empty();
	}
}
