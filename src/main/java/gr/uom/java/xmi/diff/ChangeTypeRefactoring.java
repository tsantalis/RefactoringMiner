package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public abstract class ChangeTypeRefactoring extends AbstractRefactoring {
	public Optional<String> getTemplateParameterBefore() {
		return Optional.of(codeElementDescription(getProviderBefore(), qualified()));
	}
	public String getTemplateParameterAfter() {
		return codeElementDescription(getProviderAfter(), qualified());
	}
	public boolean addCodeElementDescription() {return false;}
	
	private static String codeElementDescription(AnnotationProvider provider, boolean qualified) {
		if(provider instanceof UMLAttribute attr)
			return qualified ? attr.getVariableDeclaration().toQualifiedString() : attr.getVariableDeclaration().toString();
		else if(provider instanceof VariableDeclaration vd)
			return qualified ? vd.toQualifiedString() : vd.toString();
		return provider.toString();
	}

	private boolean qualified() {
		if(getProviderBefore() instanceof UMLAttribute a1 && getProviderAfter() instanceof UMLAttribute a2)
			return a1.getVariableDeclaration().equalType(a2.getVariableDeclaration()) && !a1.getVariableDeclaration().equalQualifiedType(a2.getVariableDeclaration());
		else if(getProviderBefore() instanceof VariableDeclaration v1 && getProviderAfter() instanceof VariableDeclaration v2)
			return v1.equalType(v2) && !v1.equalQualifiedType(v2);
		
		return false;
	}
}
