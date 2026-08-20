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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import java.io.IOException;

/**
 * Tests that long running programs are terminated correctly.
 */
public class ProtectionTest {
	private ScriptHelper helpers;

	@BeforeEach
	public void setup() {
		helpers = new ScriptHelper("/protection/");
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

	@Timeout(3)
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {"string", "loop"})
	public void run(String name) throws IOException, CompileException, LuaError, InterruptedException {
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}
}
