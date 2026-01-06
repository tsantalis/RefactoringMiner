package gr.uom.java.xmi.diff;

import java.util.Objects;

import gr.uom.java.xmi.UMLTypeAlias;

public class UMLTypeAliasDiff {
	private UMLTypeAlias removedTypeAlias;
	private UMLTypeAlias addedTypeAlias;
	private boolean nameChanged;
	private boolean typeChanged;

	public UMLTypeAliasDiff(UMLTypeAlias removedTypeAlias, UMLTypeAlias addedTypeAlias) {
		this.removedTypeAlias = removedTypeAlias;
		this.addedTypeAlias = addedTypeAlias;
		if(!removedTypeAlias.getName().equals(addedTypeAlias.getName())) {
			nameChanged = true;
		}
		if(!removedTypeAlias.getRightType().equals(addedTypeAlias.getRightType())) {
			typeChanged = true;
		}
	}

	public UMLTypeAlias getRemovedTypeAlias() {
		return removedTypeAlias;
	}

	public UMLTypeAlias getAddedTypeAlias() {
		return addedTypeAlias;
	}

	public boolean isNameChanged() {
		return nameChanged;
	}

	public boolean isTypeChanged() {
		return typeChanged;
	}

	public boolean isEmpty() {
		return !nameChanged && !typeChanged;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addedTypeAlias, removedTypeAlias);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLTypeAliasDiff other = (UMLTypeAliasDiff) obj;
		return Objects.equals(addedTypeAlias, other.addedTypeAlias)
				&& Objects.equals(removedTypeAlias, other.removedTypeAlias);
	}

	@Override
	public String toString() {
		return removedTypeAlias + "\t->\t" + addedTypeAlias;
	}
}
