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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class PerformanceTest {
	private static final int TOTAL = Integer.parseInt(System.getProperty("cobalt.perfTotal", "1"));
	private static final int DISCARD = Integer.parseInt(System.getProperty("cobalt.perfDiscard", "0"));

	private final String name;
	private final ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/perf/");

	public PerformanceTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		helpers.setupQuiet();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"binarytrees"},
			{"fannkuch"},
			{"nbody"},
			{"nsieve"},
			{"primes"},
		};

		return Arrays.asList(tests);
	}

	@Test()
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		System.out.println("[" + name + "]");

		for (int i = 0; i < TOTAL; i++) {
			long start = System.nanoTime();
			LuaThread.runMain(helpers.state, helpers.loadScript(name));
			long finish = System.nanoTime();

			if (i >= DISCARD) System.out.println("  Took " + (finish - start) / 1.0e9);
		}
	}
}
