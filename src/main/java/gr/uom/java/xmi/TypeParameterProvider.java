package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public interface TypeParameterProvider {
	public List<UMLTypeParameter> getTypeParameters();
	public void addTypeParameter(UMLTypeParameter typeParameter);

	public default List<String> getTypeParameterNames() {
		List<String> typeParameterNames = new ArrayList<String>();
		for(UMLTypeParameter typeParameter : getTypeParameters()) {
			typeParameterNames.add(typeParameter.getName());
		}
		return typeParameterNames;
	}
}
