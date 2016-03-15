package org.refactoringminer.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class EntitySet<E> implements Set<E> {

	private Set<E> set;
	
	public EntitySet() {
		this(new HashSet<E>());
	}
	
	public EntitySet(Set<E> set) {
		this.set = set;
	}
	
	public E getFirst() {
		return set.iterator().next();
	}

	public EntitySet<E> suchThat(Filter<E> filter) {
		EntitySet<E> result = new EntitySet<E>(); 
		for (E e : set) {
			if (filter.accept(e)) {
				result.add(e);
			}
		}
		return result;
	}

	public EntitySet<E> minus(EntitySet<E> other) {
		EntitySet<E> result = new EntitySet<E>(); 
		for (E e : set) {
			if (!other.contains(e)) {
				result.add(e);
			}
		}
		return result;
	}

	public EntitySet<E> minus(E entity) {
		if (set.contains(entity)) {
			EntitySet<E> result = new EntitySet<E>();
			for (E e : set) {
				if (!e.equals(entity)) {
					result.add(e);
				}
			}
			return result;
		}
		return this;
	}
	
	public int size() {
		return set.size();
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public boolean contains(Object o) {
		return set.contains(o);
	}

	public Iterator<E> iterator() {
		return set.iterator();
	}

	public Object[] toArray() {
		return set.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	public boolean add(E e) {
		return set.add(e);
	}

	public boolean remove(Object o) {
		return set.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		return set.addAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	public void clear() {
		set.clear();
	}

	public boolean equals(Object o) {
		return set.equals(o);
	}

	public int hashCode() {
		return set.hashCode();
	}

	public String toString() {
		return set.toString();
	}
}
