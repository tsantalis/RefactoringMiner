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
	
	private static String codeElementDescription(AnnotationProvider provider, boolean qualified) {
		if(provider instanceof UMLAttribute attr)
			return qualified ? attr.getVariableDeclaration().toQualifiedString() : attr.getVariableDeclaration().toString();
		if(provider instanceof VariableDeclaration vd)
			return qualified ? vd.toQualifiedString() : vd.toString();
		return provider.toString();
	}

	private boolean qualified() {
		if(getProviderBefore() instanceof UMLAttribute a1 && getProviderAfter() instanceof UMLAttribute a2)
			return a1.getVariableDeclaration().equalType(a2.getVariableDeclaration()) && !a1.getVariableDeclaration().equalQualifiedType(a2.getVariableDeclaration());
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		if(getTemplateParameterBefore().isPresent()) {
			sb.append(getTemplateParameterBefore().get());
			sb.append(" to ");
		}
		sb.append(getTemplateParameterAfter());
		AnnotationProvider provider = getProviderAfter();
		String codeElementType = codeElementType(provider);
		String className = provider.getClassName();
		if (!codeElementType.equals("class")) {
			sb.append(" in class ");
			sb.append(className);
		}
		return sb.toString();
	}
}
