package gr.uom.java.xmi.diff;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLTypeAlias;

public class UMLTypeAliasListDiff {
	private Set<UMLTypeAlias> removedTypeAliases;
	private Set<UMLTypeAlias> addedTypeAliases;
	private Set<UMLTypeAliasDiff> typeAliasDiffs;
	private Set<Pair<UMLTypeAlias, UMLTypeAlias>> commonTypeAliases;

	public UMLTypeAliasListDiff(List<UMLTypeAlias> typeAliases1, List<UMLTypeAlias> typeAliases2) {
		this.removedTypeAliases = new LinkedHashSet<UMLTypeAlias>();
		this.addedTypeAliases = new LinkedHashSet<UMLTypeAlias>();
		this.typeAliasDiffs = new LinkedHashSet<UMLTypeAliasDiff>();
		this.commonTypeAliases = new LinkedHashSet<Pair<UMLTypeAlias,UMLTypeAlias>>();
		Set<Pair<UMLTypeAlias, UMLTypeAlias>> matchedTypeAliases = new LinkedHashSet<Pair<UMLTypeAlias,UMLTypeAlias>>();
		for(UMLTypeAlias typeAlias1 : typeAliases1) {
			boolean found = false;
			for(UMLTypeAlias typeParameter2 : typeAliases2) {
				if(typeAlias1.equals(typeParameter2)) {
					matchedTypeAliases.add(Pair.of(typeAlias1, typeParameter2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLTypeAlias typeAlias2 : typeAliases2) {
					if(typeAlias1.getName().equals(typeAlias2.getName())) {
						matchedTypeAliases.add(Pair.of(typeAlias1, typeAlias2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				removedTypeAliases.add(typeAlias1);
			}
		}
		for(UMLTypeAlias typeAlias2 : typeAliases2) {
			boolean found = false;
			for(UMLTypeAlias typeParameter1 : typeAliases1) {
				if(typeParameter1.equals(typeAlias2)) {
					matchedTypeAliases.add(Pair.of(typeParameter1, typeAlias2));
					found = true;
					break;
				}
			}
			if(!found) {
				for(UMLTypeAlias typeParameter1 : typeAliases1) {
					if(typeParameter1.getName().equals(typeAlias2.getName())) {
						matchedTypeAliases.add(Pair.of(typeParameter1, typeAlias2));
						found = true;
						break;
					}
				}
			}
			if(!found) {
				addedTypeAliases.add(typeAlias2);
			}
		}
		for(Pair<UMLTypeAlias, UMLTypeAlias> pair : matchedTypeAliases) {
			UMLTypeAliasDiff typeAliasDiff = new UMLTypeAliasDiff(pair.getLeft(), pair.getRight());
			if(!typeAliasDiff.isEmpty() && !typeAliasDiffs.contains(typeAliasDiff)) {
				typeAliasDiffs.add(typeAliasDiff);
			}
			else if(!commonTypeAliases.contains(pair)){
				commonTypeAliases.add(pair);
			}
		}
		if(removedTypeAliases.size() == addedTypeAliases.size()) {
			Iterator<UMLTypeAlias> removedTypeAliasIterator = removedTypeAliases.iterator();
			Iterator<UMLTypeAlias> addedTypeAliasIterator = addedTypeAliases.iterator();
			while(removedTypeAliasIterator.hasNext() && addedTypeAliasIterator.hasNext()) {
				UMLTypeAlias removedTypeAlias = removedTypeAliasIterator.next();
				UMLTypeAlias addedTypeAlias = addedTypeAliasIterator.next();
				UMLTypeAliasDiff typeAliasDiff = new UMLTypeAliasDiff(removedTypeAlias, addedTypeAlias);
				typeAliasDiffs.add(typeAliasDiff);
				removedTypeAliasIterator.remove();
				addedTypeAliasIterator.remove();
			}
		}
	}

	public boolean isEmpty() {
		return removedTypeAliases.isEmpty() && addedTypeAliases.isEmpty() && typeAliasDiffs.isEmpty();
	}

	public Set<UMLTypeAlias> getRemovedTypeAliases() {
		return removedTypeAliases;
	}

	public Set<UMLTypeAlias> getAddedTypeAliases() {
		return addedTypeAliases;
	}

	public Set<UMLTypeAliasDiff> getTypeAliasDiffs() {
		return typeAliasDiffs;
	}

	public Set<Pair<UMLTypeAlias, UMLTypeAlias>> getCommonTypeAliases() {
		return commonTypeAliases;
	}
}
