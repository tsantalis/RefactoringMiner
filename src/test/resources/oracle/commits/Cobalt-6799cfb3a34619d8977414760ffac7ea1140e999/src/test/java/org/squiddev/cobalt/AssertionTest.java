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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.VarArgFunction;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

/**
 * Lua driven assertion tests
 */
@RunWith(Parameterized.class)
public class AssertionTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public AssertionTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/assert/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"baseIssues"},
			{"stringIssues"},
			{"debug"},
			{"debug-coroutine-hook"},
			{"gc"},
			{"immutable"},
			{"invalid-tailcall"},
			{"load-error"},
			{"no-unwind"},
			{"time"}
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

		helpers.setup();
		helpers.globals.rawset("id_", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) {
				return args;
			}
		});
	}

	@Test
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}
}
