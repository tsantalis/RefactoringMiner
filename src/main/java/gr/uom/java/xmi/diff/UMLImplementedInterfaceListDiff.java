package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLType;

public class UMLImplementedInterfaceListDiff {
	private Set<UMLType> removedInterfaces;
	private Set<UMLType> addedInterfaces;
	private Set<Pair<UMLType, UMLType>> commonInterfaces;
	private Set<Pair<UMLType, UMLType>> changedInterfaces;
	
	public UMLImplementedInterfaceListDiff(List<UMLType> oldInterfaces, List<UMLType> newInterfaces) {
		this.changedInterfaces = new LinkedHashSet<>();
		Set<UMLType> oldInterfaceSet = new LinkedHashSet<>(oldInterfaces);
		Set<UMLType> newInterfaceSet = new LinkedHashSet<>(newInterfaces);
		Set<UMLType> intersection = new LinkedHashSet<>();
		intersection.addAll(oldInterfaceSet);
		intersection.retainAll(newInterfaceSet);
		this.commonInterfaces = new LinkedHashSet<>();
		for(UMLType umlInterface : intersection) {
			UMLType oldInterface = oldInterfaces.get(oldInterfaces.indexOf(umlInterface));
			UMLType newInterface = newInterfaces.get(newInterfaces.indexOf(umlInterface));
			Pair<UMLType, UMLType> pair = Pair.of(oldInterface, newInterface);
			commonInterfaces.add(pair);
		}
		oldInterfaceSet.removeAll(intersection);
		this.removedInterfaces = oldInterfaceSet;
		newInterfaceSet.removeAll(intersection);
		this.addedInterfaces = newInterfaceSet;
	}

	public Set<UMLType> getRemovedInterfaces() {
		return removedInterfaces;
	}

	public Set<UMLType> getAddedInterfaces() {
		return addedInterfaces;
	}

	public Set<Pair<UMLType, UMLType>> getCommonInterfaces() {
		return commonInterfaces;
	}

	public Set<Pair<UMLType, UMLType>> getChangedInterfaces() {
		return changedInterfaces;
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

	public void findInterfaceChanges(String nameBefore, String nameAfter) {
		UMLType removedInterface = findMatchingInterface(removedInterfaces, nameBefore);
		UMLType addedInterface = findMatchingInterface(addedInterfaces, nameAfter);
		if(removedInterface != null && addedInterface != null) {
			Pair<UMLType, UMLType> pair = Pair.of(removedInterface, addedInterface);
			changedInterfaces.add(pair);
			removedInterfaces.remove(removedInterface);
			addedInterfaces.remove(addedInterface);
		}
	}

	public void findInterfaceChanges(UMLType typeBefore, UMLType typeAfter) {
		UMLType removedInterface = findMatchingInterface(removedInterfaces, typeBefore);
		UMLType addedInterface = findMatchingInterface(addedInterfaces, typeAfter);
		if(removedInterface != null && addedInterface != null) {
			Pair<UMLType, UMLType> pair = Pair.of(removedInterface, addedInterface);
			changedInterfaces.add(pair);
			removedInterfaces.remove(removedInterface);
			addedInterfaces.remove(addedInterface);
		}
	}
}
