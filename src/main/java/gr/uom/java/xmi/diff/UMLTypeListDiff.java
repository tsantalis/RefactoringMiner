package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLType;

public class UMLTypeListDiff {
	private Set<UMLType> removedTypes;
	private Set<UMLType> addedTypes;
	private Set<Pair<UMLType, UMLType>> commonTypes;
	private Set<Pair<UMLType, UMLType>> changedTypes;
	
	public UMLTypeListDiff(List<UMLType> oldInterfaces, List<UMLType> newInterfaces) {
		this.changedTypes = new LinkedHashSet<>();
		Set<UMLType> oldInterfaceSet = new LinkedHashSet<>(oldInterfaces);
		Set<UMLType> newInterfaceSet = new LinkedHashSet<>(newInterfaces);
		Set<UMLType> intersection = new LinkedHashSet<>();
		intersection.addAll(oldInterfaceSet);
		intersection.retainAll(newInterfaceSet);
		this.commonTypes = new LinkedHashSet<>();
		for(UMLType umlInterface : intersection) {
			UMLType oldInterface = oldInterfaces.get(oldInterfaces.indexOf(umlInterface));
			UMLType newInterface = newInterfaces.get(newInterfaces.indexOf(umlInterface));
			Pair<UMLType, UMLType> pair = Pair.of(oldInterface, newInterface);
			commonTypes.add(pair);
		}
		oldInterfaceSet.removeAll(intersection);
		this.removedTypes = oldInterfaceSet;
		newInterfaceSet.removeAll(intersection);
		this.addedTypes = newInterfaceSet;
	}

	public Set<UMLType> getRemovedTypes() {
		return removedTypes;
	}

	public Set<UMLType> getAddedTypes() {
		return addedTypes;
	}

	public Set<Pair<UMLType, UMLType>> getCommonTypes() {
		return commonTypes;
	}

	public Set<Pair<UMLType, UMLType>> getChangedTypes() {
		return changedTypes;
	}

	private UMLType findMatchingInterface(Set<UMLType> interfaces, String className) {
		for(UMLType type : interfaces) {
			if(className.endsWith("." + type.getClassType()) || className.equals(type.getClassType())) {
				return type;
			}
		}
		return null;
	}

	private UMLType findMatchingInterface(Set<UMLType> interfaces, UMLType type) {
		for(UMLType i : interfaces) {
			if(type.equalClassType(i)) {
				return i;
			}
		}
		return null;
	}

	public void findTypeChanges(String nameBefore, String nameAfter) {
		UMLType removedInterface = findMatchingInterface(removedTypes, nameBefore);
		UMLType addedInterface = findMatchingInterface(addedTypes, nameAfter);
		if(removedInterface != null && addedInterface != null) {
			Pair<UMLType, UMLType> pair = Pair.of(removedInterface, addedInterface);
			changedTypes.add(pair);
			removedTypes.remove(removedInterface);
			addedTypes.remove(addedInterface);
		}
	}

	public void findTypeChanges(UMLType typeBefore, UMLType typeAfter) {
		UMLType removedInterface = findMatchingInterface(removedTypes, typeBefore);
		UMLType addedInterface = findMatchingInterface(addedTypes, typeAfter);
		if(removedInterface != null && addedInterface != null) {
			Pair<UMLType, UMLType> pair = Pair.of(removedInterface, addedInterface);
			changedTypes.add(pair);
			removedTypes.remove(removedInterface);
			addedTypes.remove(addedInterface);
		}
	}
}
