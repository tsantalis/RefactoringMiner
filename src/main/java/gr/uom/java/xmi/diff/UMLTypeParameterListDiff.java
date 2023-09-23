package gr.uom.java.xmi.diff;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLTypeParameter;

public class UMLTypeParameterListDiff {
	private Set<UMLTypeParameter> removedTypeParameters;
	private Set<UMLTypeParameter> addedTypeParameters;
	private Set<UMLTypeParameterDiff> typeParameterDiffs;
	private Set<Pair<UMLTypeParameter, UMLTypeParameter>> commonTypeParameters;

	public UMLTypeParameterListDiff(List<UMLTypeParameter> typeParameters1, List<UMLTypeParameter> typeParameters2) {
		this.removedTypeParameters = new LinkedHashSet<UMLTypeParameter>();
		this.addedTypeParameters = new LinkedHashSet<UMLTypeParameter>();
		this.typeParameterDiffs = new LinkedHashSet<UMLTypeParameterDiff>();
		this.commonTypeParameters = new LinkedHashSet<Pair<UMLTypeParameter,UMLTypeParameter>>();
		Set<Pair<UMLTypeParameter, UMLTypeParameter>> matchedTypeParameters = new LinkedHashSet<Pair<UMLTypeParameter,UMLTypeParameter>>();
		for(UMLTypeParameter typeParameter1 : typeParameters1) {
			boolean found = false;
			for(UMLTypeParameter typeParameter2 : typeParameters2) {
				if(typeParameter1.equals(typeParameter2)) {
					matchedTypeParameters.add(Pair.of(typeParameter1, typeParameter2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLTypeParameter typeParameter2 : typeParameters2) {
					if(typeParameter1.getName().equals(typeParameter2.getName())) {
						matchedTypeParameters.add(Pair.of(typeParameter1, typeParameter2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				removedTypeParameters.add(typeParameter1);
			}
		}
		for(UMLTypeParameter typeParameter2 : typeParameters2) {
			boolean found = false;
			for(UMLTypeParameter typeParameter1 : typeParameters1) {
				if(typeParameter1.equals(typeParameter2)) {
					matchedTypeParameters.add(Pair.of(typeParameter1, typeParameter2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLTypeParameter typeParameter1 : typeParameters1) {
					if(typeParameter1.getName().equals(typeParameter2.getName())) {
						matchedTypeParameters.add(Pair.of(typeParameter1, typeParameter2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				addedTypeParameters.add(typeParameter2);
			}
		}
		for(Pair<UMLTypeParameter, UMLTypeParameter> pair : matchedTypeParameters) {
			UMLTypeParameterDiff typeParameterDiff = new UMLTypeParameterDiff(pair.getLeft(), pair.getRight());
			if(!typeParameterDiff.isEmpty() && !typeParameterDiffs.contains(typeParameterDiff)) {
				typeParameterDiffs.add(typeParameterDiff);
			}
			else if(!commonTypeParameters.contains(pair)){
				commonTypeParameters.add(pair);
			}
		}
		if(removedTypeParameters.size() == addedTypeParameters.size()) {
			Iterator<UMLTypeParameter> removedTypeParameterIterator = removedTypeParameters.iterator();
			Iterator<UMLTypeParameter> addedTypeParameterIterator = addedTypeParameters.iterator();
			while(removedTypeParameterIterator.hasNext() && addedTypeParameterIterator.hasNext()) {
				UMLTypeParameter removedTypeParameter = removedTypeParameterIterator.next();
				UMLTypeParameter addedTypeParameter = addedTypeParameterIterator.next();
				UMLTypeParameterDiff typeParameterDiff = new UMLTypeParameterDiff(removedTypeParameter, addedTypeParameter);
				typeParameterDiffs.add(typeParameterDiff);
				removedTypeParameterIterator.remove();
				addedTypeParameterIterator.remove();
			}
		}
	}

	public boolean isEmpty() {
		return removedTypeParameters.isEmpty() && addedTypeParameters.isEmpty() && typeParameterDiffs.isEmpty();
	}

	public Set<UMLTypeParameter> getRemovedTypeParameters() {
		return removedTypeParameters;
	}

	public Set<UMLTypeParameter> getAddedTypeParameters() {
		return addedTypeParameters;
	}

	public Set<UMLTypeParameterDiff> getTypeParameterDiffs() {
		return typeParameterDiffs;
	}

	public Set<Pair<UMLTypeParameter, UMLTypeParameter>> getCommonTypeParameters() {
		return commonTypeParameters;
	}
}
