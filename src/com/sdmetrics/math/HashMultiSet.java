/*
 * SDMetrics Open Core for UML design measurement
 * Copyright (c) 2002-2011 Juergen Wuest
 * To contact the author, see <http://www.sdmetrics.com/Contact.html>.
 * 
 * This file is part of the SDMetrics Open Core.
 * 
 * SDMetrics Open Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
    
 * SDMetrics Open Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SDMetrics Open Core.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.sdmetrics.math;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * A hash set that can contain the same element multiple times. Such sets are
 * known as multisets or bags.
 * <p>
 * The number of times an element is contained in the multi set is called the
 * cardinality of the element.
 * <p>
 * 
 * 
 * This implementation uses a {@link HashMap} to store the elements as keys, and
 * their cardinality as values. Like HashMap, it is not thread safe and supports
 * <code>null</code> values. Unlike HashMap, the iterator does not support
 * removal of elements.
 * 
 * @param <E> the type of elements maintained by this set
 * 
 * 
 */
public class HashMultiSet<E> extends AbstractCollection<E> {
	private final static Integer ONE = Integer.valueOf(1);

	/** The elements of the set and their cardinality. */
	private HashMap<E, Integer> backingMap;
	/** The number of elements in the set, respecting their cardinality. */
	private int elementCount = 0;

	/**
	 * Creates an empty multiset with specified initial capacity. For the
	 * initial capacity, you only need to consider the number of distinct
	 * elements the set eventually holds, regardless of their cardinality.
	 * 
	 * @param capacity the initial capacity
	 */
	public HashMultiSet(int capacity) {
		backingMap = new HashMap<E, Integer>(capacity);
	}

	/**
	 * Creates a new set containing all elements in the specified collection. If
	 * the collection can contain elements multiple times (such as lists or
	 * other multi sets), the new set will respect the cardinality of the
	 * elements.
	 * 
	 * @param contents the collection whose elements are to be placed into this
	 *        set
	 */
	public HashMultiSet(Collection<? extends E> contents) {
		backingMap = new HashMap<E, Integer>(contents.size());
		addAll(contents);
	}

	/**
	 * Returns an iterator over the elements in the set. Elements are returned
	 * in no particular order, however, an element of cardinality <code>N</code>
	 * will be returned <code>N</code> times.
	 * <p>
	 * This iterator does not support {@link Iterator#remove()}.
	 */
	@Override
	public Iterator<E> iterator() {
		return new HashMultiSetIterator(false);
	}

	/**
	 * Returns an iterator over the elements in the set, ignoring the
	 * cardinality. Each element in the set is returned exactly once, regardless
	 * of its cardinality.
	 * <p>
	 * This iterator does not support {@link Iterator#remove()}.
	 * 
	 * @return an iterator over the distinct elements contained in this set
	 * 
	 */
	public Iterator<E> getFlatIterator() {
		return new HashMultiSetIterator(true);
	}

	/**
	 * Returns the number of elements in the set, respecting cardinality of the
	 * elements.
	 */
	@Override
	public int size() {
		return elementCount;
	}

	/**
	 * Gets the number of distinct elements in this set, ignoring the
	 * cardinality of the elements.
	 * 
	 * @return The number of distinct elements in this set,
	 */
	public int flatSetSize() {
		return backingMap.size();
	}

	/**
	 * Returns <code>true</code> if this set contains the specified element at
	 * least once.
	 */
	@Override
	public boolean contains(Object o) {
		return backingMap.containsKey(o);
	}

	/**
	 * Retrieves the cardinality of an element in this set.
	 * 
	 * @param o the element to look up
	 * @return The cardinality of the element in this set, <code>0</code> if the
	 *         set does not contain the element
	 */
	public int getElementCount(Object o) {
		Integer count = backingMap.get(o);
		return count == null ? 0 : count.intValue();
	}

	/**
	 * Adds the specified element to this set. If the element is already
	 * present, its cardinality increases by one.
	 * 
	 * @param e element to be added to this set
	 * @return <code>true</code> (as specified by {@link Collection#add})
	 */
	@Override
	public boolean add(E e) {
		Integer count = backingMap.get(e);
		if (count == null)
			backingMap.put(e, ONE);
		else
			backingMap.put(e, Integer.valueOf(count.intValue() + 1));
		elementCount++;
		return true;
	}

	/**
	 * Removes one occurrence of the specified element from this set. If the
	 * element is present more than once, its cardinality is decreased by one.
	 * If it is present only once, it is removed entirely from the set.
	 * 
	 * @param o Element of which one occurrence is to be removed from this set.
	 * @return <code>true</code> if the element was present in the set
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		Integer count = backingMap.get(o);
		if (count == null)
			return false;
		if (ONE.equals(count))
			backingMap.remove(o);
		else
			backingMap.put((E) o, Integer.valueOf(count.intValue() - 1));

		elementCount--;
		return true;
	}

	/**
	 * Removes elements in this set that are also contained in the specified
	 * collection. For each occurrence of an element in the specified
	 * collection, the cardinality of that element is decreased by one in this
	 * set.
	 * <p>
	 * Let the cardinality of element <code>e</code> be <code>n&gt;0</code> in
	 * this set, and assume the element is returned <code>m&ge;0</code> times by
	 * the iterator of the specified collection. After the operation,
	 * <ul>
	 * <li>
	 * if <code>m&lt;n</code>, the cardinality of element <code>e</code> in this
	 * set is <code>n-m</code></li>
	 * <li>
	 * if <code>m&ge;n</code>, all occurrences of <code>e</code> have been
	 * removed from this set</li>
	 * </ul>
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		for (Object o : c)
			modified |= remove(o);

		return modified;
	}

	/**
	 * Retains only the elements in this collection that are also contained in
	 * the specified collection.
	 * <p>
	 * Let the cardinality of element <code>e</code> be <code>n&gt;0</code> in
	 * this set, and assume the element is returned <code>m&ge;0</code> times by
	 * the iterator of the specified collection. After the operation,
	 * <ul>
	 * <li>
	 * if <code>m&lt;n</code>, the cardinality of element <code>e</code> in this
	 * set is <code>m</code></li>
	 * <li>
	 * if <code>m&ge;n</code>, the cardinality of element <code>e</code> in this
	 * set remains unchanged at <code>n</code></li>
	 * </ul>
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public boolean retainAll(Collection<?> c) {

		HashMultiSet elementsToKeep = makeMultiSet(c);
		boolean modified = false;
		Iterator<Entry<E, Integer>> it = backingMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<E, Integer> entry = it.next();
			Integer maxCount = (Integer) elementsToKeep.backingMap.get(entry
					.getKey());
			int currentCount = entry.getValue().intValue();
			if (maxCount == null) {
				elementCount -= currentCount;
				it.remove();
				modified = true;
			} else {
				int elementsToRemove = currentCount - maxCount.intValue();
				if (elementsToRemove > 0) {
					entry.setValue(Integer.valueOf(currentCount
							- elementsToRemove));
					elementCount -= elementsToRemove;
					modified = true;
				}
			}
		}
		return modified;
	}

	/**
	 * Creates a multiset from a collection if it isn't already one.
	 * 
	 * @param c The collection to turn into a multiset
	 * @return <code>c</code> for instances of HashMultiSet, else a new
	 *         HashMultiSet with the contents of <code>c</code>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private HashMultiSet makeMultiSet(Collection<?> c) {
		if (c instanceof HashMultiSet)
			return (HashMultiSet) c;
		return new HashMultiSet(c);
	}

	/**
	 * Removes all occurrences of all elements from this set. The set will be
	 * empty after this call returns.
	 */
	@Override
	public void clear() {
		backingMap.clear();
		elementCount = 0;
	}

	/**
	 * Removes all occurrences of an element from this set.
	 * 
	 * @param o The element to remove
	 * @return The cardinality of the element before the call, <code>0</code> if
	 *         the set did not contain the element
	 */
	public int removeAny(Object o) {
		Integer count = backingMap.remove(o);
		if (count != null) {
			elementCount -= count.intValue();
			return count.intValue();
		}
		return 0;
	}

	/**
	 * Compares the specified object with this set for equality. Returns
	 * <tt>true</tt> if the given object is a collection of the same size (both
	 * flat and respecting cardinality), and each element in this set has the
	 * same number of occurrences in the specified collection.
	 * <p>
	 * Note: if the specified object is not itself a HashMultiSet, we may have
	 * <code>this.equals(o)</code> but
	 * <code>this.hashCode()!=o.hashCode()</code>.
	 * 
	 * @param o object to be compared for equality with this set
	 * @return <tt>true</tt> if the specified object is equal to this set
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof Collection))
			return false;

		Collection<?> col = (Collection<?>) o;
		if (col.size() != size())
			return false;
		if (isEmpty())
			return true; // both sets are empty, happens a lot

		@SuppressWarnings("rawtypes")
		HashMultiSet ms = makeMultiSet(col);
		if (ms.flatSetSize() != flatSetSize())
			return false;
		for (Entry<E, Integer> entry : backingMap.entrySet()) {
			if (!entry.getValue().equals(ms.backingMap.get(entry.getKey())))
				return false;
		}

		return true;
	}

	/**
	 * Returns the hash code value for this set.
	 * <p>
	 * 
	 * This implementation takes the sum of
	 * <code>hashCode(e)^cardinality(e)</code> over all elements in the set.
	 */
	@Override
	public int hashCode() {
		int hash = 0;
		for (Entry<E, Integer> entry : backingMap.entrySet()) {
			int keyHash = entry.getKey() == null ? 0 : entry.getKey()
					.hashCode();
			hash += keyHash ^ entry.getValue().intValue();
		}

		return hash;
	}

	/**
	 * Iterator for the multiset. Does not support remove().
	 */
	class HashMultiSetIterator implements Iterator<E> {

		/** Iterator of the entries of the backing hash map. */
		private Iterator<Entry<E, Integer>> backingIterator;
		/** Answer for the next call to hasNext(). */
		private boolean hasNextElement;
		/** Answer for the next call to next(). */
		private E nextElement;
		/**
		 * Counts how many more calls to next() will return the current element.
		 */
		private int elementCounter;
		/** Indicates if this iterator should ignore element cardinality. */
		private boolean isFlatIteration;

		/**
		 * Constructs a new iterator.
		 * 
		 * @param flatIteration <code>true</code> if the iterator should ignore
		 *        cardinality of elements and return each element only once.
		 */
		HashMultiSetIterator(boolean flatIteration) {
			backingIterator = backingMap.entrySet().iterator();
			elementCounter = 0;
			hasNextElement = true;
			isFlatIteration = flatIteration;
			getReady();
		}

		@Override
		public boolean hasNext() {
			return hasNextElement;
		}

		@Override
		public E next() {
			if (hasNextElement) {
				E result = nextElement;
				getReady();
				return result;
			}
			throw new NoSuchElementException();
		}

		/**
		 * Raises an exception
		 * 
		 * @throws UnsupportedOperationException Removal of elements is not
		 *         supported.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/** Prepare nextObject and hasNextObject for subsequent calls. */
		private void getReady() {
			if (elementCounter > 0) {
				elementCounter--;
				return;
			}

			if (backingIterator.hasNext()) {
				Entry<E, Integer> entry = backingIterator.next();
				nextElement = entry.getKey();
				elementCounter = isFlatIteration ? 0 : entry.getValue()
						.intValue() - 1;
				return;
			}

			hasNextElement = false;
		}
	}
}
