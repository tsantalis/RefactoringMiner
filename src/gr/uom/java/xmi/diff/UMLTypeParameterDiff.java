package gr.uom.java.xmi.diff;

import java.util.Objects;

import gr.uom.java.xmi.UMLTypeParameter;

public class UMLTypeParameterDiff {
	private UMLTypeParameter removedTypeParameter;
	private UMLTypeParameter addedTypeParameter;
	private boolean nameChanged;
	private boolean typeBoundsChanged;

	public UMLTypeParameterDiff(UMLTypeParameter removedTypeParameter, UMLTypeParameter addedTypeParameter) {
		this.removedTypeParameter = removedTypeParameter;
		this.addedTypeParameter = addedTypeParameter;
		if(!removedTypeParameter.getName().equals(addedTypeParameter.getName())) {
			nameChanged = true;
		}
		if(!removedTypeParameter.getTypeBounds().equals(addedTypeParameter.getTypeBounds())) {
			typeBoundsChanged = true;
		}
	}

	public UMLTypeParameter getRemovedTypeParameter() {
		return removedTypeParameter;
	}

	public UMLTypeParameter getAddedTypeParameter() {
		return addedTypeParameter;
	}

	public boolean isNameChanged() {
		return nameChanged;
	}

	public boolean isTypeBoundsChanged() {
		return typeBoundsChanged;
	}

	public boolean isEmpty() {
		return !nameChanged && !typeBoundsChanged;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addedTypeParameter, removedTypeParameter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLTypeParameterDiff other = (UMLTypeParameterDiff) obj;
		return Objects.equals(addedTypeParameter, other.addedTypeParameter)
				&& Objects.equals(removedTypeParameter, other.removedTypeParameter);
	}

	@Override
	public String toString() {
		return removedTypeParameter + "\t->\t" + addedTypeParameter;
	}
}
