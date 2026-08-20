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
package org.squiddev.cobalt.vm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.lib.StringLib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class MetatableTest {

	private final String samplestring = "abcdef";
	private final Object sampleobject = new Object();
	private final TypeTest.MyData sampledata = new TypeTest.MyData();

	private final LuaValue string = valueOf(samplestring);
	private final LuaTable table = ValueFactory.tableOf();
	private final LuaFunction function = new ZeroArgFunction() {
		@Override
		public LuaValue call(LuaState state) {
			return Constants.NONE;
		}
	};
	private final LuaState state = new LuaState();
	private final LuaThread thread = new LuaThread(state, function, table);
	private final LuaClosure closure = new LuaInterpretedFunction(new Prototype());
	private final LuaUserdata userdata = ValueFactory.userdataOf(sampleobject);
	private final LuaUserdata userdatamt = ValueFactory.userdataOf(sampledata, table);

	public MetatableTest() throws LuaError {
	}

	@Before
	public void setup() throws Exception {
		// needed for metatable ops to work on strings
		new StringLib();
	}

	@After
	public void tearDown() throws Exception {
		state.booleanMetatable = null;
		state.functionMetatable = null;
		state.nilMetatable = null;
		state.numberMetatable = null;
		state.threadMetatable = null;
//		LuaString.s_metatable = null;
	}

	@Test
	public void testGetMetatable() {
		assertEquals(null, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, table.getMetatable(state));
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		assertEquals(null, userdata.getMetatable(state));
		assertEquals(table, userdatamt.getMetatable(state));
	}

	@Test
	public void testSetMetatable() {
		LuaTable mt = ValueFactory.tableOf();
		assertEquals(null, table.getMetatable(state));
		assertEquals(null, userdata.getMetatable(state));
		assertEquals(table, userdatamt.getMetatable(state));
		table.setMetatable(state, mt);
		userdata.setMetatable(state, mt);
		userdatamt.setMetatable(state, mt);
		assertEquals(mt, table.getMetatable(state));
		assertEquals(mt, userdata.getMetatable(state));
		assertEquals(mt, userdatamt.getMetatable(state));

		// these all get metatable behind-the-scenes
		assertEquals(null, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.nilMetatable = mt;
		assertEquals(mt, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.booleanMetatable = mt;
		assertEquals(mt, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.numberMetatable = mt;
		assertEquals(mt, Constants.ONE.getMetatable(state));
		assertEquals(mt, valueOf(1.25).getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
//		LuaString.s_metatable = mt;
//		assertEquals( mt, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.functionMetatable = mt;
		assertEquals(mt, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		state.threadMetatable = mt;
		assertEquals(mt, thread.getMetatable(state));
	}

	@Test
	public void testMetatableIndex() throws LuaError, UnwindThrowable {
		table.setMetatable(state, null);
		userdata.setMetatable(state, null);
		userdatamt.setMetatable(state, null);
		assertEquals(Constants.NIL, OperationHelper.getTable(state, table, valueOf(1)));
		assertThrows(LuaError.class, () -> OperationHelper.getTable(state, userdata, valueOf(1)));
		assertThrows(LuaError.class, () -> OperationHelper.getTable(state, userdatamt, valueOf(1)));

		// empty metatable
		LuaTable mt = ValueFactory.tableOf();
		table.setMetatable(state, mt);
		userdata.setMetatable(state, mt);
		state.booleanMetatable = mt;
		state.functionMetatable = mt;
		state.nilMetatable = mt;
		state.numberMetatable = mt;
		state.stringMetatable = mt;
		state.threadMetatable = mt;
		userdata.metatable = mt;
		userdatamt.metatable = mt;

		assertEquals(mt, table.getMetatable(state));
		assertEquals(mt, userdata.getMetatable(state));
		assertEquals(mt, Constants.NIL.getMetatable(state));
		assertEquals(mt, Constants.TRUE.getMetatable(state));
		assertEquals(mt, Constants.ONE.getMetatable(state));
		assertEquals(mt, string.getMetatable(state));
		assertEquals(mt, function.getMetatable(state));
		assertEquals(mt, thread.getMetatable(state));

		// plain metatable
		LuaValue abc = valueOf("abc");
		OperationHelper.setTable(state, mt, Constants.INDEX, ValueFactory.listOf(new LuaValue[]{abc}));
		assertEquals(abc, OperationHelper.getTable(state, table, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, userdata, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, Constants.NIL, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, Constants.TRUE, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, Constants.ONE, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, Constants.EMPTYSTRING, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, function, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, thread, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, userdata, valueOf(1)));
		assertEquals(abc, OperationHelper.getTable(state, userdatamt, valueOf(1)));

		// plain metatable
		OperationHelper.setTable(state, mt, Constants.INDEX, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state1, LuaValue arg1, LuaValue arg2) {
				return valueOf(arg1.typeName() + "[" + arg2.toString() + "]=xyz");
			}

		});
		assertEquals("table[1]=xyz", OperationHelper.getTable(state, table, valueOf(1)).toString());
		assertEquals("userdata[1]=xyz", OperationHelper.getTable(state, userdata, valueOf(1)).toString());
		assertEquals("nil[1]=xyz", OperationHelper.getTable(state, Constants.NIL, valueOf(1)).toString());
		assertEquals("boolean[1]=xyz", OperationHelper.getTable(state, Constants.TRUE, valueOf(1)).toString());
		assertEquals("number[1]=xyz", OperationHelper.getTable(state, Constants.ONE, valueOf(1)).toString());
		assertEquals("string[1]=xyz", OperationHelper.getTable(state, Constants.EMPTYSTRING, valueOf(1)).toString());
		assertEquals("function[1]=xyz", OperationHelper.getTable(state, function, valueOf(1)).toString());
		assertEquals("thread[1]=xyz", OperationHelper.getTable(state, thread, valueOf(1)).toString());
	}


	@Test
	public void testMetatableNewIndex() throws LuaError, UnwindThrowable {
		// empty metatable
		LuaTable mt = ValueFactory.tableOf();
		table.setMetatable(state, mt);
		userdata.setMetatable(state, mt);
		state.booleanMetatable = mt;
		state.functionMetatable = mt;
		state.nilMetatable = mt;
		state.numberMetatable = mt;
//		LuaString.s_metatable = mt;
		state.threadMetatable = mt;

		// plain metatable
		final LuaTable fallback = ValueFactory.tableOf();
		LuaValue abc = valueOf("abc");
		OperationHelper.setTable(state, mt, Constants.NEWINDEX, fallback);
		OperationHelper.setTable(state, table, valueOf(2), abc);
		OperationHelper.setTable(state, userdata, valueOf(3), abc);
		OperationHelper.setTable(state, Constants.NIL, valueOf(4), abc);
		OperationHelper.setTable(state, Constants.TRUE, valueOf(5), abc);
		OperationHelper.setTable(state, Constants.ONE, valueOf(6), abc);
		// 		string.set(7,abc);
		OperationHelper.setTable(state, function, valueOf(8), abc);
		OperationHelper.setTable(state, thread, valueOf(9), abc);
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(2)));
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(3)));
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(4)));
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(5)));
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(6)));
// 		assertEquals( abc, StringLib.instance.get(7) );
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(8)));
		assertEquals(abc, OperationHelper.getTable(state, fallback, valueOf(9)));

		// metatable with function call
		OperationHelper.setTable(state, mt, Constants.NEWINDEX, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaState state1, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				fallback.rawset(arg2, valueOf("via-func-" + arg3));
				return Constants.NONE;
			}

		});
		OperationHelper.setTable(state, table, valueOf(12), abc);
		OperationHelper.setTable(state, userdata, valueOf(13), abc);
		OperationHelper.setTable(state, Constants.NIL, valueOf(14), abc);
		OperationHelper.setTable(state, Constants.TRUE, valueOf(15), abc);
		OperationHelper.setTable(state, Constants.ONE, valueOf(16), abc);
		// 		string.set(17,abc);
		OperationHelper.setTable(state, function, valueOf(18), abc);
		OperationHelper.setTable(state, thread, valueOf(19), abc);
		LuaValue via = valueOf("via-func-abc");
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(12)));
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(13)));
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(14)));
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(15)));
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(16)));
//		assertEquals( via, StringLib.instance.get(17) );
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(18)));
		assertEquals(via, OperationHelper.getTable(state, fallback, valueOf(19)));
	}


	private void checkTable(LuaTable t,
							LuaValue aa, LuaValue bb, LuaValue cc, LuaValue dd, LuaValue ee, LuaValue ff, LuaValue gg,
							LuaValue ra, LuaValue rb, LuaValue rc, LuaValue rd, LuaValue re, LuaValue rf, LuaValue rg) throws LuaError, UnwindThrowable {
		assertEquals(aa, OperationHelper.getTable(state, t, valueOf("aa")));
		assertEquals(bb, OperationHelper.getTable(state, t, valueOf("bb")));
		assertEquals(cc, OperationHelper.getTable(state, t, valueOf("cc")));
		assertEquals(dd, OperationHelper.getTable(state, t, valueOf("dd")));
		assertEquals(ee, OperationHelper.getTable(state, t, valueOf("ee")));
		assertEquals(ff, OperationHelper.getTable(state, t, valueOf("ff")));
		assertEquals(gg, OperationHelper.getTable(state, t, valueOf("gg")));
		assertEquals(ra, t.rawget("aa"));
		assertEquals(rb, t.rawget("bb"));
		assertEquals(rc, t.rawget("cc"));
		assertEquals(rd, t.rawget("dd"));
		assertEquals(re, t.rawget("ee"));
		assertEquals(rf, t.rawget("ff"));
		assertEquals(rg, t.rawget("gg"));
	}

	private LuaTable makeTable(String key1, String val1, String key2, String val2) {
		return ValueFactory.tableOf(
			valueOf(key1), valueOf(val1),
			valueOf(key2), valueOf(val2)
		);
	}

	@Test
	public void testRawsetMetatableSet() throws LuaError, UnwindThrowable {
		// set up tables
		LuaTable m = makeTable("aa", "aaa", "bb", "bbb");
		OperationHelper.setTable(state, m, Constants.INDEX, m);
		OperationHelper.setTable(state, m, Constants.NEWINDEX, m);
		LuaTable s = makeTable("cc", "ccc", "dd", "ddd");
		LuaTable t = makeTable("cc", "ccc", "dd", "ddd");
		t.setMetatable(state, m);
		LuaValue aaa = valueOf("aaa");
		LuaValue bbb = valueOf("bbb");
		LuaValue ccc = valueOf("ccc");
		LuaValue ddd = valueOf("ddd");
		LuaValue ppp = valueOf("ppp");
		LuaValue qqq = valueOf("qqq");
		LuaValue rrr = valueOf("rrr");
		LuaValue sss = valueOf("sss");
		LuaValue ttt = valueOf("ttt");
		LuaValue www = valueOf("www");
		LuaValue xxx = valueOf("xxx");
		LuaValue yyy = valueOf("yyy");
		LuaValue zzz = valueOf("zzz");
		LuaValue nil = Constants.NIL;

		// check initial values
		//             values via "bet()"           values via "rawget()"
		checkTable(s, nil, nil, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);

		// rawset()
		s.rawset("aa", www);
		checkTable(s, www, nil, ccc, ddd, nil, nil, nil, www, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		s.rawset("cc", xxx);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset("bb", yyy);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, yyy, ccc, ddd, nil, nil, nil, nil, yyy, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset("dd", zzz);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);

		// set() invoking metatables
		OperationHelper.setTable(state, s, valueOf("ee"), ppp);
		checkTable(s, www, nil, xxx, ddd, ppp, nil, nil, www, nil, xxx, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		OperationHelper.setTable(state, s, valueOf("cc"), qqq);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		OperationHelper.setTable(state, t, valueOf("ff"), rrr);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, rrr, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		OperationHelper.setTable(state, t, valueOf("dd"), sss);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, nil, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		OperationHelper.setTable(state, m, valueOf("gg"), ttt);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);

		// make s fall back to t
		s.setMetatable(state, ValueFactory.tableOf(new LuaValue[]{Constants.INDEX, t, Constants.NEWINDEX, t}));
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		OperationHelper.setTable(state, s, valueOf("aa"), www);
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		OperationHelper.setTable(state, s, valueOf("bb"), zzz);
		checkTable(s, www, zzz, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		OperationHelper.setTable(state, s, valueOf("ee"), xxx);
		checkTable(s, www, zzz, qqq, ddd, xxx, rrr, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		OperationHelper.setTable(state, s, valueOf("ff"), yyy);
		checkTable(s, www, zzz, qqq, ddd, xxx, yyy, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, yyy, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, yyy, ttt, aaa, bbb, nil, nil, nil, yyy, ttt);


	}

}
