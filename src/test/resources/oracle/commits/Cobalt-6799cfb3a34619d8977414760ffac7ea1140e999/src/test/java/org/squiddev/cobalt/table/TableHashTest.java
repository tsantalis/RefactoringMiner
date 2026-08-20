/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.table;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.TwoArgFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for tables used as lists.
 */
public class TableHashTest {
	private LuaState state;

	@Before
	public void setup() throws Exception {
		state = new LuaState();
	}

	@Test
	public void testSetRemove() throws LuaError, UnwindThrowable {
		LuaTable t = new LuaTable();

		assertEquals(0, t.getHashLength());
		assertEquals(0, t.length());
		assertEquals(0, t.keyCount());

		String[] keys = {"abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "wxy", "z01",
			"cd", "ef", "g", "hi", "jk", "lm", "no", "pq", "rs",};
		int[] capacities = {0, 1, 2, 4, 4, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16, 32, 32, 32};
		for (int i = 0; i < keys.length; ++i) {
			assertEquals(capacities[i], t.getHashLength());
			String si = "Test Value! " + i;
			OperationHelper.setTable(state, t, ValueFactory.valueOf(keys[i]), ValueFactory.valueOf(si));
			assertEquals(0, t.length());
			assertEquals(i + 1, t.keyCount());
		}
		assertEquals(capacities[keys.length], t.getHashLength());
		for (int i = 0; i < keys.length; ++i) {
			LuaValue vi = LuaString.valueOf("Test Value! " + i);
			assertEquals(vi, OperationHelper.getTable(state, t, ValueFactory.valueOf(keys[i])));
			assertEquals(vi, OperationHelper.getTable(state, t, LuaString.valueOf(keys[i])));
			assertEquals(vi, t.rawget(keys[i]));
			assertEquals(vi, t.rawget(keys[i]));
		}

		// replace with new values
		for (int i = 0; i < keys.length; ++i) {
			OperationHelper.setTable(state, t, ValueFactory.valueOf(keys[i]), LuaString.valueOf("Replacement Value! " + i));
			assertEquals(0, t.length());
			assertEquals(keys.length, t.keyCount());
			assertEquals(capacities[keys.length], t.getHashLength());
		}
		for (int i = 0; i < keys.length; ++i) {
			LuaValue vi = LuaString.valueOf("Replacement Value! " + i);
			assertEquals(vi, OperationHelper.getTable(state, t, ValueFactory.valueOf(keys[i])));
		}

		// remove
		for (int i = 0; i < keys.length; ++i) {
			OperationHelper.setTable(state, t, ValueFactory.valueOf(keys[i]), Constants.NIL);
			assertEquals(0, t.length());
			assertEquals(keys.length - i - 1, t.keyCount());
			if (i < keys.length - 1) {
				assertEquals(capacities[keys.length], t.getHashLength());
			} else {
				assertTrue(0 <= t.getHashLength());
			}
		}
		for (String key : keys) {
			assertEquals(Constants.NIL, OperationHelper.getTable(state, t, ValueFactory.valueOf(key)));
		}
	}

	@Test
	public void testIndexMetatag() throws LuaError, UnwindThrowable {
		LuaTable t = new LuaTable();
		LuaTable mt = new LuaTable();
		LuaTable fb = new LuaTable();

		// set basic values
		OperationHelper.setTable(state, t, ValueFactory.valueOf("ppp"), ValueFactory.valueOf("abc"));
		OperationHelper.setTable(state, t, ValueFactory.valueOf(123), ValueFactory.valueOf("def"));
		OperationHelper.setTable(state, mt, Constants.INDEX, fb);
		OperationHelper.setTable(state, fb, ValueFactory.valueOf("qqq"), ValueFactory.valueOf("ghi"));
		OperationHelper.setTable(state, fb, ValueFactory.valueOf(456), ValueFactory.valueOf("jkl"));

		// check before setting metatable
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());
		assertEquals("nil", OperationHelper.getTable(state, fb, ValueFactory.valueOf("ppp")).toString());
		assertEquals("nil", OperationHelper.getTable(state, fb, ValueFactory.valueOf(123)).toString());
		assertEquals("ghi", OperationHelper.getTable(state, fb, ValueFactory.valueOf("qqq")).toString());
		assertEquals("jkl", OperationHelper.getTable(state, fb, ValueFactory.valueOf(456)).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf("ppp")).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf(456)).toString());

		// check before setting metatable
		t.setMetatable(state, mt);
		assertEquals(mt, t.getMetatable(state));
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("ghi", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("jkl", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());
		assertEquals("nil", OperationHelper.getTable(state, fb, ValueFactory.valueOf("ppp")).toString());
		assertEquals("nil", OperationHelper.getTable(state, fb, ValueFactory.valueOf(123)).toString());
		assertEquals("ghi", OperationHelper.getTable(state, fb, ValueFactory.valueOf("qqq")).toString());
		assertEquals("jkl", OperationHelper.getTable(state, fb, ValueFactory.valueOf(456)).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf("ppp")).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, mt, ValueFactory.valueOf(456)).toString());

		// set metatable to metatable without values
		t.setMetatable(state, fb);
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());

		// set metatable to null
		t.setMetatable(state, null);
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());
	}

	@Test
	public void testIndexFunction() throws LuaError, UnwindThrowable {
		final LuaTable t = new LuaTable();
		final LuaTable mt = new LuaTable();

		final TwoArgFunction fb = new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue tbl, LuaValue key) {
				assertEquals(tbl, t);
				return ValueFactory.valueOf("from mt: " + key);
			}
		};

		// set basic values
		OperationHelper.setTable(state, t, ValueFactory.valueOf("ppp"), ValueFactory.valueOf("abc"));
		OperationHelper.setTable(state, t, ValueFactory.valueOf(123), ValueFactory.valueOf("def"));
		OperationHelper.setTable(state, mt, Constants.INDEX, fb);

		// check before setting metatable
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());


		// check before setting metatable
		t.setMetatable(state, mt);
		assertEquals(mt, t.getMetatable(state));
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("from mt: qqq", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("from mt: 456", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());

		// use raw set
		t.rawset("qqq", ValueFactory.valueOf("alt-qqq"));
		t.rawset(456, ValueFactory.valueOf("alt-456"));
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("alt-qqq", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("alt-456", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());

		// remove using raw set
		t.rawset("qqq", Constants.NIL);
		t.rawset(456, Constants.NIL);
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("from mt: qqq", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("from mt: 456", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());

		// set metatable to null
		t.setMetatable(state, null);
		assertEquals("abc", OperationHelper.getTable(state, t, ValueFactory.valueOf("ppp")).toString());
		assertEquals("def", OperationHelper.getTable(state, t, ValueFactory.valueOf(123)).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf("qqq")).toString());
		assertEquals("nil", OperationHelper.getTable(state, t, ValueFactory.valueOf(456)).toString());
	}

	@Test
	public void testNext() throws LuaError, UnwindThrowable {
		final LuaTable t = new LuaTable();
		assertEquals(Constants.NIL, t.next(Constants.NIL));

		// insert array elements
		OperationHelper.setTable(state, t, ValueFactory.valueOf(1), ValueFactory.valueOf("one"));
		assertEquals(ValueFactory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(ValueFactory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(Constants.NIL, t.next(Constants.ONE));
		OperationHelper.setTable(state, t, ValueFactory.valueOf(2), ValueFactory.valueOf("two"));
		assertEquals(ValueFactory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(ValueFactory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(ValueFactory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(ValueFactory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(Constants.NIL, t.next(ValueFactory.valueOf(2)));

		// insert hash elements
		OperationHelper.setTable(state, t, ValueFactory.valueOf("aa"), ValueFactory.valueOf("aaa"));
		assertEquals(ValueFactory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(ValueFactory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(ValueFactory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(ValueFactory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(ValueFactory.valueOf("aa"), t.next(ValueFactory.valueOf(2)).arg(1));
		assertEquals(ValueFactory.valueOf("aaa"), t.next(ValueFactory.valueOf(2)).arg(2));
		assertEquals(Constants.NIL, t.next(ValueFactory.valueOf("aa")));
		OperationHelper.setTable(state, t, ValueFactory.valueOf("bb"), ValueFactory.valueOf("bbb"));
		assertEquals(ValueFactory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(ValueFactory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(ValueFactory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(ValueFactory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(ValueFactory.valueOf("aa"), t.next(ValueFactory.valueOf(2)).arg(1));
		assertEquals(ValueFactory.valueOf("aaa"), t.next(ValueFactory.valueOf(2)).arg(2));
		assertEquals(ValueFactory.valueOf("bb"), t.next(ValueFactory.valueOf("aa")).arg(1));
		assertEquals(ValueFactory.valueOf("bbb"), t.next(ValueFactory.valueOf("aa")).arg(2));
		assertEquals(Constants.NIL, t.next(ValueFactory.valueOf("bb")));
	}
}
