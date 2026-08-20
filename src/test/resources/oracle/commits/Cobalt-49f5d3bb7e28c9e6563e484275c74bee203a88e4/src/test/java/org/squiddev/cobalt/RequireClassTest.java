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
package org.squiddev.cobalt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.require.RequireSampleClassCastExcep;
import org.squiddev.cobalt.require.RequireSampleLoadLuaError;
import org.squiddev.cobalt.require.RequireSampleLoadRuntimeExcep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RequireClassTest {

	private LuaFunction require;
	private LuaState state;

	@BeforeEach
	public void setup() throws LuaError, UnwindThrowable {
		state = new LuaState();
		LuaTable globals = JsePlatform.standardGlobals(state);
		require = (LuaFunction) OperationHelper.getTable(state, globals, ValueFactory.valueOf("require"));
	}

	@Test
	public void testRequireClassSuccess() throws LuaError, UnwindThrowable {
		LuaValue result = require.call(state, ValueFactory.valueOf("org.squiddev.cobalt.require.RequireSampleSuccess"));
		assertEquals("require-sample-success", result.toString());
		result = require.call(state, ValueFactory.valueOf("org.squiddev.cobalt.require.RequireSampleSuccess"));
		assertEquals("require-sample-success", result.toString());
	}

	@Test
	public void testRequireClassLoadLuaError() throws UnwindThrowable {
		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleLoadLuaError.class.getName()));
			fail("incorrectly loaded class that threw lua error");
		} catch (LuaError le) {
			assertEquals(
				"sample-load-lua-error",
				le.getMessage());
		}
		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleLoadLuaError.class.getName()));
			fail("incorrectly loaded class that threw lua error");
		} catch (LuaError le) {
			assertEquals(
				"loop or previous error loading module '" + RequireSampleLoadLuaError.class.getName() + "'",
				le.getMessage());
		}
	}

	@Test
	public void testRequireClassLoadRuntimeException() throws UnwindThrowable {
		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleLoadRuntimeExcep.class.getName()));
			fail("incorrectly loaded class that threw runtime exception");
		} catch (RuntimeException le) {
			assertEquals(
				"sample-load-runtime-exception",
				le.getMessage());
		} catch (LuaError e) {
			fail(e.getMessage());
		}

		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleLoadRuntimeExcep.class.getName()));
			fail("incorrectly loaded class that threw runtime exception");
		} catch (LuaError le) {
			assertEquals(
				"loop or previous error loading module '" + RequireSampleLoadRuntimeExcep.class.getName() + "'",
				le.getMessage());
		}
	}

	@Test
	public void testRequireClassClassCastException() throws UnwindThrowable {
		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleClassCastExcep.class.getName()));
			fail("incorrectly loaded class that threw class cast exception");
		} catch (LuaError le) {
			String msg = le.getMessage();
			if (!msg.contains("not found")) {
				fail("expected 'not found' message but got " + msg);
			}
		}
		try {
			LuaValue result = require.call(state, ValueFactory.valueOf(RequireSampleClassCastExcep.class.getName()));
			fail("incorrectly loaded class that threw class cast exception");
		} catch (LuaError le) {
			String msg = le.getMessage();
			if (!msg.contains("not found")) {
				fail("expected 'not found' message but got " + msg);
			}
		}
	}
}
