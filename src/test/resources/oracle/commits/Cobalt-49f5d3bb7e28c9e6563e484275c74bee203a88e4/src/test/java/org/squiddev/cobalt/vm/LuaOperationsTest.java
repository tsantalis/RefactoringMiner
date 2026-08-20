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

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class LuaOperationsTest {
	private final int sampleint = 77;
	private final long samplelong = 123400000000L;
	private final double sampledouble = 55.25;
	private final String samplestringstring = "abcdef";
	private final String samplestringint = String.valueOf(sampleint);
	private final String samplestringlong = String.valueOf(samplelong);
	private final String samplestringdouble = String.valueOf(sampledouble);
	private final Object sampleobject = new Object();
	private final TypeTest.MyData sampledata = new TypeTest.MyData();

	private final LuaValue somenil = Constants.NIL;
	private final LuaValue sometrue = Constants.TRUE;
	private final LuaValue somefalse = Constants.FALSE;
	private final LuaValue zero = Constants.ZERO;
	private final LuaValue intint = valueOf(sampleint);
	private final LuaValue longdouble = valueOf(samplelong);
	private final LuaValue doubledouble = valueOf(sampledouble);
	private final LuaValue stringstring = valueOf(samplestringstring);
	private final LuaValue stringint = valueOf(samplestringint);
	private final LuaValue stringlong = valueOf(samplestringlong);
	private final LuaValue stringdouble = valueOf(samplestringdouble);
	private final LuaTable table = ValueFactory.listOf(new LuaValue[]{valueOf("aaa"), valueOf("bbb")});
	private final LuaFunction somefunc = new ZeroArgFunction() {
		{
			env = table;
		}

		@Override
		public LuaValue call(LuaState state) {
			return Constants.NONE;
		}
	};
	private final LuaState state = new LuaState();
	private final LuaThread thread = new LuaThread(state, somefunc, table);
	private final Prototype proto = new Prototype();
	private final LuaClosure someclosure = new LuaInterpretedFunction(proto, table);
	private final LuaUserdata userdataobj = ValueFactory.userdataOf(sampleobject);
	private final LuaUserdata userdatacls = ValueFactory.userdataOf(sampledata);

	private void throwsLuaError(String methodName, Object obj) {
		try {
			try {
				LuaValue.class.getMethod(methodName, LuaState.class).invoke(obj, state);
			} catch (NoSuchMethodException e1) {
				try {
					LuaValue.class.getMethod(methodName).invoke(obj);
				} catch (NoSuchMethodException e2) {
					OperationHelper.class.getMethod(methodName, LuaState.class, LuaValue.class).invoke(null, state, obj);
				}
			}
			fail("failed to throw LuaError as required");
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof LuaError)) {
				fail("not a LuaError: " + e.getTargetException());
			}
		} catch (Exception e) {
			fail("bad exception: " + e);
		}
	}

	private void throwsLuaError(String methodName, Object obj, Object arg) {
		try {
			try {
				LuaValue.class.getMethod(methodName, LuaState.class, LuaValue.class).invoke(obj, state, arg);
			} catch (NoSuchMethodException e) {
				LuaValue.class.getMethod(methodName, LuaValue.class).invoke(obj, arg);
			}
			fail("failed to throw LuaError as required");
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof LuaError)) {
				fail("not a LuaError: " + e.getTargetException());
			}
		} catch (Exception e) {
			fail("bad exception: " + e);
		}
	}

	private void setfenvThrowsLuaError(String methodName, Object obj, Object arg) {
		try {
			LuaValue.class.getMethod(methodName, LuaTable.class).invoke(obj, arg);
			fail("failed to throw LuaError as required");
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof LuaError)) {
				fail("not a LuaError: " + e.getTargetException());
			}
		} catch (Exception e) {
			fail("bad exception: " + e);
		}
	}

	@Test
	public void testLength() throws LuaError, UnwindThrowable {
		throwsLuaError("length", somenil);
		throwsLuaError("length", sometrue);
		throwsLuaError("length", somefalse);
		throwsLuaError("length", zero);
		throwsLuaError("length", intint);
		throwsLuaError("length", longdouble);
		throwsLuaError("length", doubledouble);
		assertEquals(samplestringstring.length(), OperationHelper.length(state, stringstring).toInteger());
		assertEquals(samplestringint.length(), OperationHelper.length(state, stringint).toInteger());
		assertEquals(samplestringlong.length(), OperationHelper.length(state, stringlong).toInteger());
		assertEquals(samplestringdouble.length(), OperationHelper.length(state, stringdouble).toInteger());
		assertEquals(2, table.length());
		throwsLuaError("length", somefunc);
		throwsLuaError("length", thread);
		throwsLuaError("length", someclosure);
		throwsLuaError("length", userdataobj);
		throwsLuaError("length", userdatacls);
	}

	@Test
	public void testGetfenv() {
		throwsLuaError("getfenv", somenil);
		throwsLuaError("getfenv", sometrue);
		throwsLuaError("getfenv", somefalse);
		throwsLuaError("getfenv", zero);
		throwsLuaError("getfenv", intint);
		throwsLuaError("getfenv", longdouble);
		throwsLuaError("getfenv", doubledouble);
		throwsLuaError("getfenv", stringstring);
		throwsLuaError("getfenv", stringint);
		throwsLuaError("getfenv", stringlong);
		throwsLuaError("getfenv", stringdouble);
		throwsLuaError("getfenv", table);
		assertSame(table, thread.getfenv());
		assertSame(table, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		throwsLuaError("getfenv", userdataobj);
		throwsLuaError("getfenv", userdatacls);
	}

	@Test
	public void testSetfenv() {
		LuaTable table2 = ValueFactory.listOf(valueOf("ccc"), valueOf("ddd"));
		setfenvThrowsLuaError("setfenv", somenil, table2);
		setfenvThrowsLuaError("setfenv", sometrue, table2);
		setfenvThrowsLuaError("setfenv", somefalse, table2);
		setfenvThrowsLuaError("setfenv", zero, table2);
		setfenvThrowsLuaError("setfenv", intint, table2);
		setfenvThrowsLuaError("setfenv", longdouble, table2);
		setfenvThrowsLuaError("setfenv", doubledouble, table2);
		setfenvThrowsLuaError("setfenv", stringstring, table2);
		setfenvThrowsLuaError("setfenv", stringint, table2);
		setfenvThrowsLuaError("setfenv", stringlong, table2);
		setfenvThrowsLuaError("setfenv", stringdouble, table2);
		setfenvThrowsLuaError("setfenv", table, table2);
		thread.setfenv(table2);
		assertSame(table2, thread.getfenv());
		assertSame(table, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		someclosure.setfenv(table2);
		assertSame(table2, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		somefunc.setfenv(table2);
		assertSame(table2, somefunc.getfenv());
		setfenvThrowsLuaError("setfenv", userdataobj, table2);
		setfenvThrowsLuaError("setfenv", userdatacls, table2);
	}

	public Prototype createPrototype(String script, String name) {
		try {
			InputStream is = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
			return LuaC.compile(is, name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
			return null;
		}
	}

	@Test
	public void testFunctionClosureThreadEnv() throws LuaError, UnwindThrowable, InterruptedException {
		// set up suitable environments for execution
		LuaValue aaa = valueOf("aaa");
		LuaValue eee = valueOf("eee");
		LuaTable _G = JsePlatform.standardGlobals(state);
		LuaTable newenv = ValueFactory.tableOf(valueOf("a"), valueOf("aaa"),
			valueOf("b"), valueOf("bbb"));
		LuaTable mt = ValueFactory.tableOf(Constants.INDEX, _G);
		newenv.setMetatable(state, mt);
		OperationHelper.setTable(state, _G, valueOf("a"), aaa);
		OperationHelper.setTable(state, newenv, valueOf("a"), eee);

		// function tests
		{
			LuaFunction f = new ZeroArgFunction() {
				{
					env = _G;
				}

				@Override
				public LuaValue call(LuaState state) throws LuaError {
					return OperationHelper.noUnwind(state, () -> OperationHelper.getTable(state, env, valueOf("a")));
				}
			};
			assertEquals(aaa, f.call(state));
			f.setfenv(newenv);
			assertEquals(newenv, f.getfenv());
			assertEquals(eee, f.call(state));
		}

		// closure tests
		{
			Prototype p = createPrototype("return a\n", "closuretester");
			LuaClosure c = new LuaInterpretedFunction(p, _G);
			assertEquals(aaa, c.call(state));
			c.setfenv(newenv);
			assertEquals(newenv, c.getfenv());
			assertEquals(eee, c.call(state));
		}

		// thread tests, functions created in threads inherit the thread's environment initially
		// those closures created not in any other function get the thread's enviroment
		Prototype p2 = createPrototype("return loadstring('return a')", "threadtester");
		{
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p2, _G), _G);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(aaa, OperationHelper.call(state, f));
			assertEquals(_G, f.getfenv());
		}
		{
			// change the thread environment after creation!
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p2, _G), _G);
			t.setfenv(newenv);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(eee, OperationHelper.call(state, f));
			assertEquals(newenv, f.getfenv());
		}
		{
			// let the closure have a different environment from the thread
			Prototype p3 = createPrototype("return function() return a end", "envtester");
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p3, newenv), _G);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(eee, OperationHelper.call(state, f));
			assertEquals(newenv, f.getfenv());
		}
	}
}
