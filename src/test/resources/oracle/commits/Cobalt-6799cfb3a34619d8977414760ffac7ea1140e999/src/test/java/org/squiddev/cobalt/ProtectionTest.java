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
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests protection
 */
@RunWith(Parameterized.class)
public class ProtectionTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public ProtectionTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/protection/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"string"},
			{"loop"},
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() {
		helpers.setup(s -> s.debug(new DebugHandler() {
			private long time = System.currentTimeMillis();

			@Override
			public void poll() throws LuaError {
				if (System.currentTimeMillis() > time + 500) {
					time = System.currentTimeMillis();
					throw new LuaError("Timed out");
				}
			}

			@Override
			public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
				poll();
				super.onInstruction(ds, di, pc);
			}
		}));
	}

	@Test(timeout = 3000)
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}
}
