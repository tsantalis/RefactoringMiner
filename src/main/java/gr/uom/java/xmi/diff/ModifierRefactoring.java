package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLModifier;

public abstract class ModifierRefactoring extends AbstractRefactoring {
	public abstract UMLModifier getModifier();
	public abstract String getModifierAsString();
	public String getTemplateParameter() {return getModifierAsString();}
}
