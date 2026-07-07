package gr.uom.java.xmi;

import java.util.List;

public interface TypeParameterProvider {
	public List<UMLTypeParameter> getTypeParameters();
	public void addTypeParameter(UMLTypeParameter typeParameter);
}
