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

import java.util.HashMap;

/**
 * Realizes a sparse matrix of integers.
 * <p>
 * This implementation stores the coordinates and values of non-null entries in
 * a hash map. Hence, memory consumption depends on the number of non-null
 * entries in the matrix rather than the dimension of the matrix.
 */
public class IntegerMatrix {
	/** Constant for 0. */
	private final static Integer ZERO = Integer.valueOf(0);
	/** Constant for 1. */
	private final static Integer ONE = Integer.valueOf(1);

	/**
	 * HashMap to store the non-null values. Key is the coordinate pair, value
	 * is the integer value at those coordinates.
	 */
	private HashMap<Long, Integer> map;

	/** Creates a new null matrix. */
	public IntegerMatrix() {
		map = new HashMap<Long, Integer>();
	}

	/**
	 * Increments the integer value at the given coordinates by one.
	 * 
	 * @param row Matrix row number. Must not be negative.
	 * @param col Matrix column number. Must not be negative.
	 * @return The new value at the given coordinates.
	 */
	public Integer increment(int row, int col) {
		Long coordinates = getCoordinatesKey(row, col);
		Integer oldValue = map.get(coordinates);
		if (oldValue == null) {
			map.put(coordinates, ONE);
			return ONE;
		}

		Integer newValue = Integer.valueOf(oldValue.intValue() + 1);
		map.put(coordinates, newValue);
		return newValue;
	}

	/**
	 * Retrieves the integer at the given coordinates.
	 * 
	 * @param row Matrix row number. Must not be negative.
	 * @param col Matrix column number. Must not be negative.
	 * @return Value at those coordinates.
	 */
	public Integer get(int row, int col) {
		Integer result = map.get(getCoordinatesKey(row, col));
		if (result == null)
			return ZERO;
		return result;
	}

	/**
	 * Tests whether this matrix is a null matrix.
	 * 
	 * @return <code>false</code> if the matrix has any non-zero entries, else
	 *         <code>true</code>.
	 */
	public boolean isEmpty() {
		return map.size() == 0;
	}

	private Long getCoordinatesKey(int row, int col) {
		// put the column in the upper 4 bytes of a long, the rows in the lower
		// 4 bytes
		return Long.valueOf((((long) col) << 32) | row);
	}
}
