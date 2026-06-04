package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.Visibility;

public abstract class ChangeAccessModifierRefactoring extends AbstractRefactoring {
	public abstract Visibility getOriginalAccessModifier();
	public abstract Visibility getChangedAccessModifier();
	public Optional<String> getTemplateParameterBefore() {return Optional.of(getOriginalAccessModifier().toString());}
	public String getTemplateParameterAfter() {return getChangedAccessModifier().toString();}
}
