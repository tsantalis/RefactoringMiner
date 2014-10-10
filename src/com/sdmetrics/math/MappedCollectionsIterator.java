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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Iterates over the elements contained in multiple collections, which are
 * organized as values in a map.
 * 
 * @param <T> Type of the elements contained in the collections.
 */
public class MappedCollectionsIterator<T> implements Iterator<T> {
	/** Iterator for the outer map. */
	private Iterator<? extends Collection<T>> outerIterator;
	/** Iterator for an inner collection. */
	private Iterator<T> innerIterator;
	/** Result for next() method. */
	private T nextObject;
	/** Result for hasNext() method. */
	private boolean hasNextObject;

	/**
	 * @param map The map containing the collections.
	 */
	public MappedCollectionsIterator(Map<?, ? extends Collection<T>> map) {
		outerIterator = map.values().iterator();
		innerIterator = null;
		hasNextObject = true;
		getReady();
	}

	/**
	 * Throws an UnsupportedOperationException.
	 * 
	 * @throws UnsupportedOperationException Remove not supported by this
	 *         iterator.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T next() {
		if (hasNextObject) {
			T result = nextObject;
			getReady();
			return result;
		}
		throw new NoSuchElementException();
	}

	@Override
	public boolean hasNext() {
		return hasNextObject;
	}

	/** Prepare nextObject and hasNextObject for subsequent calls. */
	private void getReady() {
		if (innerIterator == null) {
			nextInner();
			if (innerIterator == null)
				return; // no more inner iterators; quit
		}
		if (innerIterator.hasNext()) {
			nextObject = innerIterator.next();
		} else {
			innerIterator = null; // forget current inner iterator, try the next
									// one
			getReady();
		}
	}

	/** Switch to the next inner collection. */
	private void nextInner() {
		if (outerIterator.hasNext()) {
			Collection<T> c = outerIterator.next();
			innerIterator = c.iterator();
		} else {
			hasNextObject = false;
		}
	}
}
