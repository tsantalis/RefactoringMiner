package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.UMLModifier;

public abstract class ModifierRefactoring extends AbstractRefactoring {
	public abstract UMLModifier getModifier();
	public abstract String getModifierAsString();
	public Optional<String> getTemplateParameterBefore() {return Optional.empty();}
	public String getTemplateParameterAfter() {return getModifierAsString();}
}
