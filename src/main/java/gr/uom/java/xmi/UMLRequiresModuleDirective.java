package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.RequiresDirective;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;

public class UMLRequiresModuleDirective extends UMLAbstractModuleDirective {
	private List<UMLModuleModifier> modifiers;

	public UMLRequiresModuleDirective(CompilationUnit cu, String sourceFolder, String filePath, RequiresDirective directive) {
		super(new LeafExpression(cu, sourceFolder, filePath, directive.getName(), CodeElementType.DIRECTIVE_NAME, null));
		this.modifiers = new ArrayList<>();
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, directive, CodeElementType.REQUIRES_DIRECTIVE);
		List<ModuleModifier> modifiers = directive.modifiers();
		for(ModuleModifier modifier : modifiers) {
			UMLModuleModifier moduleModifier = new UMLModuleModifier(cu, sourceFolder, filePath, modifier);
			this.modifiers.add(moduleModifier);
		}
	}

	public List<UMLModuleModifier> getModifiers() {
		return modifiers;
	}

	public boolean equalModifiers(UMLRequiresModuleDirective other) {
		return this.modifiers.equals(other.modifiers);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("requires ");
		int i = 0;
		for(UMLModuleModifier modifier : modifiers) {
			sb.append(modifier);
			if(i < modifiers.size() - 1)
				sb.append(", ");
			else
				sb.append(" ");
			i++;
		}
		sb.append(getName().getString());
		return sb.toString();
	}
}
